package com.example.engine

import java.util.PriorityQueue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.min

/**
 * Thread-safe local Trie index for 100% offline real-time word completion,
 * context-aware next-word prediction, and Damerau-Levenshtein autocorrect.
 */
class WordTrie {

    data class FuzzyMatch(
        val word: String,
        val editCost: Float,
        val score: Double,
    )

    private class TrieNode {
        val children = HashMap<Char, TrieNode>(4)
        var word: String? = null
        var baseRank: Int = 0
        var userFrequency: Int = 0
        var lastUsedMillis: Long = 0L
        var isUserAdded: Boolean = false
        var maxSubtreeScore: Double = 0.0
        var nextWordCounts: HashMap<String, Int>? = null

        fun isActiveTerminal(): Boolean =
            word != null && (baseRank > 0 || userFrequency > 0 || isUserAdded)
    }

    private val lock = ReentrantReadWriteLock()
    private val root = TrieNode()
    private val typoCorrections = HashMap<String, HashMap<String, Int>>()
    private var activeWordCount: Int = 0

    fun insertBaseWords(words: List<String>) = lock.write {
        val total = words.size
        words.forEachIndexed { idx, raw ->
            val clean = raw.trim().lowercase()
            if (clean.isNotEmpty()) {
                val rank = total - idx
                insertOrUpdateInternal(
                    clean = clean,
                    baseRank = rank,
                    userFrequencyDelta = 0,
                    lastUsedMillis = 0L,
                    userAdded = false,
                )
            }
        }
    }

    fun setLearnedWords(
        words: List<Triple<String, Int, Pair<Long, Boolean>>>,
    ) = lock.write {
        resetUserStatsInternal(root)
        words.forEach { (raw, freq, meta) ->
            val clean = raw.trim().lowercase()
            if (isLearnableWord(clean)) {
                insertOrUpdateInternal(
                    clean = clean,
                    baseRank = null,
                    userFrequencyDelta = freq,
                    lastUsedMillis = meta.first,
                    userAdded = meta.second,
                )
            }
        }
    }

    fun setLearnedTransitions(transitions: List<Triple<String, String, Int>>) = lock.write {
        clearTransitionsInternal(root)
        transitions.forEach { (rawPrev, rawNext, freq) ->
            recordTransitionInternal(rawPrev, rawNext, freq.coerceAtLeast(1))
        }
    }

    fun setTypoPatterns(patterns: List<Triple<String, String, Int>>) = lock.write {
        typoCorrections.clear()
        patterns.forEach { (rawTyped, rawTarget, freq) ->
            recordTypoPatternInternal(rawTyped, rawTarget, freq.coerceAtLeast(1))
        }
    }

    fun learnWord(
        word: String,
        previousWord: String? = null,
        rawTyped: String? = null,
        userAdded: Boolean = false,
        nowMillis: Long = System.currentTimeMillis(),
    ) = lock.write {
        val clean = word.trim().lowercase()
        if (!isLearnableWord(clean)) return@write

        val increment = if (userAdded) 15 else 1
        insertOrUpdateInternal(
            clean = clean,
            baseRank = null,
            userFrequencyDelta = increment,
            lastUsedMillis = nowMillis,
            userAdded = userAdded,
        )

        if (!previousWord.isNullOrBlank()) {
            recordTransitionInternal(previousWord, clean, 1)
        }

        if (!rawTyped.isNullOrBlank()) {
            val cleanTyped = rawTyped.trim().lowercase()
            if (cleanTyped != clean && isLearnableWord(cleanTyped)) {
                recordTypoPatternInternal(cleanTyped, clean, 1)
            }
        }
    }

    fun removeLearnedWord(word: String) = lock.write {
        val clean = word.trim().lowercase()
        val path = ArrayList<TrieNode>(clean.length + 1)
        var curr: TrieNode? = root
        path.add(root)
        for (ch in clean) {
            curr = curr?.children?.get(ch) ?: return@write
            path.add(curr)
        }
        val target = curr ?: return@write
        val wasActive = target.isActiveTerminal()
        target.userFrequency = 0
        target.lastUsedMillis = 0L
        target.isUserAdded = false
        if (wasActive && !target.isActiveTerminal()) {
            target.word = null
            activeWordCount = (activeWordCount - 1).coerceAtLeast(0)
        }
        recomputePathMaxScores(path)
    }

    fun clearAllLearnedPatterns() = lock.write {
        resetUserStatsInternal(root)
        clearTransitionsInternal(root)
        typoCorrections.clear()
    }

    fun containsActiveWord(word: String): Boolean = lock.read {
        findNode(word.trim().lowercase())?.isActiveTerminal() == true
    }

    fun wordScore(
        word: String,
        previousWord: String? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): Double = lock.read {
        val clean = word.trim().lowercase()
        val node = findNode(clean) ?: return@read 0.0
        if (!node.isActiveTerminal()) return@read 0.0
        val transitionCount = if (!previousWord.isNullOrBlank()) {
            findTransitionCount(previousWord, clean)
        } else {
            0
        }
        scoreNode(node, transitionCount, nowMillis)
    }

    fun wordCount(): Int = lock.read { activeWordCount }

    fun transitionPatternCount(): Int = lock.read {
        var count = 0
        fun dfs(node: TrieNode) {
            count += node.nextWordCounts?.size ?: 0
            node.children.values.forEach(::dfs)
        }
        dfs(root)
        count
    }

    fun typoPatternCount(): Int = lock.read {
        typoCorrections.values.sumOf { it.size }
    }

    fun allWords(): List<String> = lock.read {
        val out = ArrayList<String>(activeWordCount.coerceAtLeast(16))
        fun dfs(node: TrieNode) {
            if (node.isActiveTerminal()) {
                node.word?.let(out::add)
            }
            node.children.values.forEach(::dfs)
        }
        dfs(root)
        out
    }

    /**
     * Traverses to the prefix node in O(prefix.length) and uses a best-first max-heap
     * over `maxSubtreeScore` to return top completions without scanning unrelated words.
     */
    fun suggestPrefix(
        prefix: String,
        previousWord: String? = null,
        limit: Int = 6,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<String> = lock.read {
        val cleanPrefix = prefix.trim().lowercase()
        if (cleanPrefix.isEmpty()) return@read emptyList()

        val prefixNode = findNode(cleanPrefix)
        val prevClean = normalizeContextWord(previousWord)
        val prevNode = if (prevClean.isNotEmpty()) findNode(prevClean) else null
        val prevTransitions = prevNode?.nextWordCounts

        val candidateNodes = ArrayList<TrieNode>(limit * 4)

        if (prefixNode != null) {
            if (prefixNode.isActiveTerminal()) {
                candidateNodes.add(prefixNode)
            }
            val pq = PriorityQueue<TrieNode>(compareByDescending { it.maxSubtreeScore })
            for (child in prefixNode.children.values) {
                pq.add(child)
            }
            val collectBudget = (limit * 6).coerceAtLeast(24)

            while (pq.isNotEmpty() && candidateNodes.size < collectBudget) {
                val curr = pq.poll() ?: break
                if (curr.isActiveTerminal()) {
                    candidateNodes.add(curr)
                }
                for (child in curr.children.values) {
                    pq.add(child)
                }
            }
        }

        // Ensure context-matched next words starting with prefix are included even if lower static rank
        if (prevTransitions != null) {
            for (nextWord in prevTransitions.keys) {
                if (nextWord.startsWith(cleanPrefix)) {
                    val n = findNode(nextWord)
                    if (n != null && n.isActiveTerminal() && n !in candidateNodes) {
                        candidateNodes.add(n)
                    }
                }
            }
        }

        return@read candidateNodes
            .asSequence()
            .mapNotNull { node ->
                val w = node.word ?: return@mapNotNull null
                val transCount = prevTransitions?.get(w) ?: 0
                val exactBoost = if (w == cleanPrefix) 420.0 else 0.0
                val finalScore = scoreNode(node, transCount, nowMillis) + exactBoost
                w to finalScore
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .distinct()
            .take(limit)
            .toList()
    }

    /**
     * Returns next-word candidates learned in the Trie from user typing transitions,
     * falling back to static common bigrams when no user pattern exists yet.
     */
    fun suggestNextWords(
        previousWord: String?,
        fallbackBigrams: Map<String, List<String>>,
        limit: Int = 3,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<String> = lock.read {
        val prev = normalizeContextWord(previousWord)
        if (prev.isEmpty()) return@read listOf("I", "The", "Thanks").take(limit)

        val learned = findNode(prev)?.nextWordCounts
            ?.entries
            ?.asSequence()
            ?.map { (nextWord, count) ->
                val targetNode = findNode(nextWord)
                val base = if (targetNode != null) scoreNode(targetNode, count, nowMillis) else count * 60.0
                nextWord to (count * 120.0 + base)
            }
            ?.sortedByDescending { it.second }
            ?.map { it.first }
            ?.toList()
            .orEmpty()

        val staticNext = fallbackBigrams[prev].orEmpty()
        val combined = buildList {
            learned.forEach { if (it !in this) add(it) }
            staticNext.forEach { if (it !in this) add(it) }
            if (isEmpty()) {
                add("the")
                add("to")
                add("is")
            }
        }
        combined.take(limit)
    }

    /**
     * Trie-guided Damerau-Levenshtein DP traversal with branch pruning and keyboard adjacency weights.
     * Merges learned typo-to-word correction patterns from user typing history.
     */
    fun suggestFuzzy(
        input: String,
        previousWord: String? = null,
        maxCost: Float = 2.15f,
        limit: Int = 4,
        areAdjacentKeys: (Char, Char) -> Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<FuzzyMatch> = lock.read {
        val lower = input.trim().lowercase()
        val m = lower.length
        if (m < 2) return@read emptyList()

        val prevClean = normalizeContextWord(previousWord)
        val prevTransitions = if (prevClean.isNotEmpty()) findNode(prevClean)?.nextWordCounts else null
        val collected = HashMap<String, Float>()

        val rootRow = FloatArray(m + 1) { it.toFloat() }

        fun traverse(
            node: TrieNode,
            prevRow: FloatArray,
            prevPrevRow: FloatArray?,
            prevTrieChar: Char?,
            prevRowMin: Float,
        ) {
            for ((ch, child) in node.children) {
                val currRow = FloatArray(m + 1)
                currRow[0] = prevRow[0] + 1f
                var rowMin = currRow[0]

                for (j in 1..m) {
                    val inputChar = lower[j - 1]
                    val subCost = when {
                        ch == inputChar -> 0f
                        areAdjacentKeys(ch, inputChar) -> 0.50f
                        else -> 1.0f
                    }
                    var cost = minOf(
                        prevRow[j] + 1f,
                        currRow[j - 1] + 1f,
                        prevRow[j - 1] + subCost,
                    )
                    if (
                        j >= 2 &&
                        prevTrieChar != null &&
                        prevPrevRow != null &&
                        ch == lower[j - 2] &&
                        prevTrieChar == inputChar
                    ) {
                        cost = min(cost, prevPrevRow[j - 2] + 0.55f)
                    }
                    currRow[j] = cost
                    if (cost < rowMin) rowMin = cost
                }

                if (child.isActiveTerminal()) {
                    val w = child.word
                    val editCost = currRow[m]
                    if (w != null && w != lower && editCost <= maxCost) {
                        val existing = collected[w]
                        if (existing == null || editCost < existing) {
                            collected[w] = editCost
                        }
                    }
                }

                if (min(rowMin, prevRowMin) <= maxCost) {
                    traverse(
                        node = child,
                        prevRow = currRow,
                        prevPrevRow = prevRow,
                        prevTrieChar = ch,
                        prevRowMin = rowMin,
                    )
                }
            }
        }

        traverse(
            node = root,
            prevRow = rootRow,
            prevPrevRow = null,
            prevTrieChar = null,
            prevRowMin = 0f,
        )

        typoCorrections[lower]?.forEach { (targetWord, patternCount) ->
            if (containsActiveWordInternal(targetWord)) {
                val discount = (0.35f + patternCount * 0.12f).coerceAtMost(0.95f)
                val baseCost = collected[targetWord] ?: 1.15f
                collected[targetWord] = (baseCost - discount).coerceAtLeast(0.05f)
            }
        }

        return@read collected.entries
            .asSequence()
            .map { (word, cost) ->
                val node = findNode(word)
                val transCount = prevTransitions?.get(word) ?: 0
                val wordScore = if (node != null) scoreNode(node, transCount, nowMillis) else 0.0
                FuzzyMatch(
                    word = word,
                    editCost = cost,
                    score = wordScore,
                )
            }
            .sortedBy { match ->
                match.editCost.toDouble() - (match.score / 8500.0)
            }
            .take(limit)
            .toList()
    }

    private fun insertOrUpdateInternal(
        clean: String,
        baseRank: Int?,
        userFrequencyDelta: Int,
        lastUsedMillis: Long,
        userAdded: Boolean,
    ) {
        val path = ArrayList<TrieNode>(clean.length + 1)
        var curr = root
        path.add(curr)
        for (ch in clean) {
            curr = curr.children.getOrPut(ch) { TrieNode() }
            path.add(curr)
        }

        val wasActive = curr.isActiveTerminal()
        curr.word = clean
        if (baseRank != null && baseRank > curr.baseRank) {
            curr.baseRank = baseRank
        }
        if (userFrequencyDelta > 0) {
            curr.userFrequency += userFrequencyDelta
        }
        if (lastUsedMillis > curr.lastUsedMillis) {
            curr.lastUsedMillis = lastUsedMillis
        }
        if (userAdded) {
            curr.isUserAdded = true
        }
        if (!wasActive && curr.isActiveTerminal()) {
            activeWordCount++
        }

        recomputePathMaxScores(path)
    }

    private fun recordTransitionInternal(rawPrev: String, rawNext: String, weight: Int) {
        val prev = normalizeContextWord(rawPrev)
        val next = normalizeContextWord(rawNext)
        if (prev.isEmpty() || next.length < 2) return
        val prevNode = findNode(prev) ?: return
        val map = prevNode.nextWordCounts ?: HashMap<String, Int>(4).also { prevNode.nextWordCounts = it }
        map[next] = (map[next] ?: 0) + weight
    }

    private fun recordTypoPatternInternal(rawTyped: String, rawTarget: String, weight: Int) {
        val typed = rawTyped.trim().lowercase()
        val target = rawTarget.trim().lowercase()
        if (typed.length < 2 || target.length < 2 || typed == target) return
        val map = typoCorrections.getOrPut(typed) { HashMap(2) }
        map[target] = (map[target] ?: 0) + weight
    }

    private fun resetUserStatsInternal(node: TrieNode): Double {
        val wasActive = node.isActiveTerminal()
        node.userFrequency = 0
        node.lastUsedMillis = 0L
        node.isUserAdded = false
        if (wasActive && !node.isActiveTerminal()) {
            node.word = null
            activeWordCount = (activeWordCount - 1).coerceAtLeast(0)
        }
        var best = if (node.isActiveTerminal()) staticNodeScore(node) else 0.0
        for (child in node.children.values) {
            val childMax = resetUserStatsInternal(child)
            if (childMax > best) best = childMax
        }
        node.maxSubtreeScore = best
        return best
    }

    private fun clearTransitionsInternal(node: TrieNode) {
        node.nextWordCounts?.clear()
        node.children.values.forEach(::clearTransitionsInternal)
    }

    private fun recomputePathMaxScores(path: List<TrieNode>) {
        for (i in path.indices.reversed()) {
            val node = path[i]
            var best = if (node.isActiveTerminal()) staticNodeScore(node) else 0.0
            for (child in node.children.values) {
                if (child.maxSubtreeScore > best) {
                    best = child.maxSubtreeScore
                }
            }
            node.maxSubtreeScore = best
        }
    }

    private fun findNode(word: String): TrieNode? {
        var curr = root
        for (ch in word) {
            curr = curr.children[ch] ?: return null
        }
        return curr
    }

    private fun containsActiveWordInternal(word: String): Boolean =
        findNode(word)?.isActiveTerminal() == true

    private fun findTransitionCount(previousWord: String, nextWord: String): Int {
        val prev = normalizeContextWord(previousWord)
        if (prev.isEmpty()) return 0
        return findNode(prev)?.nextWordCounts?.get(nextWord) ?: 0
    }

    private fun staticNodeScore(node: TrieNode): Double {
        val userPart = node.userFrequency * 35.0
        val manualPart = if (node.isUserAdded) 180.0 else 0.0
        val recencyBase = if (node.lastUsedMillis > 0L) 60.0 else 0.0
        return node.baseRank + userPart + manualPart + recencyBase
    }

    private fun scoreNode(
        node: TrieNode,
        transitionCount: Int,
        nowMillis: Long,
    ): Double {
        val userPart = node.userFrequency * 35.0
        val manualPart = if (node.isUserAdded) 180.0 else 0.0
        val recencyPart = if (node.lastUsedMillis > 0L) {
            val ageHours = ((nowMillis - node.lastUsedMillis).coerceAtLeast(0L)) / 3_600_000.0
            (80.0 - ageHours * 1.5).coerceAtLeast(0.0)
        } else {
            0.0
        }
        val transitionPart = transitionCount * 55.0
        return node.baseRank + userPart + manualPart + recencyPart + transitionPart
    }

    private fun normalizeContextWord(raw: String?): String =
        raw?.trim()?.lowercase()?.trimEnd('.', ',', '!', '?', ';', ':').orEmpty()

    private fun isLearnableWord(clean: String): Boolean =
        clean.length >= 2 && clean.all { it.isLetter() || it == '\'' }
}
