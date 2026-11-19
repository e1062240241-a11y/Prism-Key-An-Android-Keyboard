package com.example.engine

import com.example.data.repository.SuggestionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SuggestionEngine(
    private val suggestionRepository: SuggestionRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val wordTrie = WordTrie().apply {
        insertBaseWords(CORE_WORDS)
    }

    val allDictionaryWords: List<String>
        get() = wordTrie.allWords()

    suspend fun refreshLearnedWords() {
        val learnedSnapshot = suggestionRepository.getSnapshot()
        val transitionSnapshot = suggestionRepository.getTransitionsSnapshot()
        val typoSnapshot = suggestionRepository.getTypoPatternsSnapshot()

        wordTrie.setLearnedWords(
            learnedSnapshot.map { Triple(it.word, it.frequency, it.lastUsed to it.isUserAdded) },
        )
        wordTrie.setLearnedTransitions(
            transitionSnapshot.map { Triple(it.prevWord, it.nextWord, it.frequency) },
        )
        wordTrie.setTypoPatterns(
            typoSnapshot.map { Triple(it.typedInput, it.targetWord, it.frequency) },
        )
    }

    fun learnWord(
        word: String,
        previousWord: String? = null,
        rawTyped: String? = null,
        userAdded: Boolean = false,
    ) {
        wordTrie.learnWord(
            word = word,
            previousWord = previousWord,
            rawTyped = rawTyped,
            userAdded = userAdded,
        )
    }

    fun learnWordAsync(
        word: String,
        previousWord: String? = null,
        rawTyped: String? = null,
        userAdded: Boolean = false,
    ) {
        val clean = word.trim().lowercase()
        if (clean.length < 2 || !clean.all { it.isLetter() || it == '\'' }) return

        wordTrie.learnWord(
            word = clean,
            previousWord = previousWord,
            rawTyped = rawTyped,
            userAdded = userAdded,
        )

        scope.launch {
            suggestionRepository.learnWord(clean, userAdded = userAdded)
            if (!previousWord.isNullOrBlank()) {
                suggestionRepository.recordTransition(previousWord, clean)
            }
            if (!rawTyped.isNullOrBlank()) {
                val cleanTyped = rawTyped.trim().lowercase()
                if (cleanTyped != clean && cleanTyped.length >= 2) {
                    suggestionRepository.recordTypoPattern(cleanTyped, clean)
                }
            }
        }
    }

    fun recordUndoAutocorrectAsync(
        correctedWord: String,
        originalTyped: String,
        previousWord: String? = null,
    ) {
        val cleanOrig = originalTyped.trim().lowercase()
        if (cleanOrig.length < 2 || !cleanOrig.all { it.isLetter() || it == '\'' }) return

        // Promote the user's original word in the Trie so it isn't overridden again
        wordTrie.learnWord(
            word = cleanOrig,
            previousWord = previousWord,
            userAdded = true,
        )
        scope.launch {
            suggestionRepository.learnWord(cleanOrig, userAdded = true)
            if (!previousWord.isNullOrBlank()) {
                suggestionRepository.recordTransition(previousWord, cleanOrig)
            }
        }
    }

    fun removeLearnedWord(word: String) {
        wordTrie.removeLearnedWord(word)
    }

    fun clearLearnedPatterns() {
        wordTrie.clearAllLearnedPatterns()
    }

    fun suggestNextWords(previousWord: String?): List<String> {
        return wordTrie.suggestNextWords(
            previousWord = previousWord,
            fallbackBigrams = BIGRAM_NEXT,
            limit = 3,
        )
    }

    fun suggestForComposing(
        composing: String,
        previousWord: String? = null,
        autocorrectEnabled: Boolean = true,
    ): SuggestionResult {
        if (composing.isBlank()) {
            val next = suggestNextWords(previousWord)
            return SuggestionResult(suggestions = next, autocorrectCandidate = null)
        }

        val lower = composing.lowercase()
        val isCapitalized = composing.firstOrNull()?.isUpperCase() == true
        val isAllCaps = composing.length > 1 && composing.all { !it.isLetter() || it.isUpperCase() }

        val prefixMatches = wordTrie.suggestPrefix(
            prefix = lower,
            previousWord = previousWord,
            limit = 6,
        )

        val isExactKnown = wordTrie.containsActiveWord(lower)

        val fuzzyMatches = if ((!isExactKnown || prefixMatches.size < 3) && lower.length >= 2) {
            val maxCost = if (lower.length <= 3) 1.65f else 2.2f
            wordTrie.suggestFuzzy(
                input = lower,
                previousWord = previousWord,
                maxCost = maxCost,
                limit = 4,
                areAdjacentKeys = ::areAdjacentKeys,
            ).map { it.word }
        } else {
            emptyList()
        }

        val combined = buildList {
            if (isExactKnown) {
                add(lower)
            }
            prefixMatches.forEach { if (it !in this) add(it) }
            fuzzyMatches.forEach { if (it !in this) add(it) }
            if (isEmpty()) add(composing)
        }.take(3).map { applyCasing(it, isCapitalized, isAllCaps) }

        val autocorrect = if (autocorrectEnabled && !isExactKnown && lower.length >= 2) {
            val bestFuzzy = fuzzyMatches.firstOrNull()
                ?: prefixMatches.firstOrNull { it.length == lower.length }
            bestFuzzy?.let { applyCasing(it, isCapitalized, isAllCaps) }
        } else {
            null
        }

        return SuggestionResult(
            suggestions = combined,
            autocorrectCandidate = autocorrect,
        )
    }

    fun wordScore(word: String, previousWord: String? = null): Int =
        wordTrie.wordScore(word, previousWord).toInt()

    fun trieWordCount(): Int = wordTrie.wordCount()

    fun learnedTransitionCount(): Int = wordTrie.transitionPatternCount()

    fun learnedTypoPatternCount(): Int = wordTrie.typoPatternCount()

    private fun applyCasing(word: String, capitalized: Boolean, allCaps: Boolean): String {
        return when {
            allCaps -> word.uppercase()
            capitalized -> word.replaceFirstChar { it.uppercase() }
            else -> word
        }
    }

    private fun areAdjacentKeys(c1: Char, c2: Char): Boolean {
        val p1 = KEY_COORDS[c1] ?: return false
        val p2 = KEY_COORDS[c2] ?: return false
        val dx = p1.first - p2.first
        val dy = p1.second - p2.second
        return (dx * dx + dy * dy) <= 0.025f
    }

    data class SuggestionResult(
        val suggestions: List<String>,
        val autocorrectCandidate: String?,
    )

    companion object {
        val KEY_COORDS: Map<Char, Pair<Float, Float>> = mapOf(
            'q' to (0.05f to 0.17f), 'w' to (0.15f to 0.17f), 'e' to (0.25f to 0.17f),
            'r' to (0.35f to 0.17f), 't' to (0.45f to 0.17f), 'y' to (0.55f to 0.17f),
            'u' to (0.65f to 0.17f), 'i' to (0.75f to 0.17f), 'o' to (0.85f to 0.17f),
            'p' to (0.95f to 0.17f),
            'a' to (0.10f to 0.50f), 's' to (0.20f to 0.50f), 'd' to (0.30f to 0.50f),
            'f' to (0.40f to 0.50f), 'g' to (0.50f to 0.50f), 'h' to (0.60f to 0.50f),
            'j' to (0.70f to 0.50f), 'k' to (0.80f to 0.50f), 'l' to (0.90f to 0.50f),
            'z' to (0.20f to 0.83f), 'x' to (0.30f to 0.83f), 'c' to (0.40f to 0.83f),
            'v' to (0.50f to 0.83f), 'b' to (0.60f to 0.83f), 'n' to (0.70f to 0.83f),
            'm' to (0.80f to 0.83f),
        )

        private val BIGRAM_NEXT: Map<String, List<String>> = mapOf(
            "i" to listOf("am", "have", "think", "will", "love", "just"),
            "i'm" to listOf("on", "going", "good", "here", "sorry"),
            "how" to listOf("are", "is", "much", "about", "was"),
            "thank" to listOf("you", "god", "goodness"),
            "thanks" to listOf("for", "so", "again", "a"),
            "good" to listOf("morning", "night", "luck", "idea", "job"),
            "see" to listOf("you", "ya", "the", "what"),
            "on" to listOf("my", "the", "time", "it"),
            "my" to listOf("way", "friend", "phone", "day"),
            "let's" to listOf("go", "meet", "do", "talk"),
            "are" to listOf("you", "we", "they", "there"),
            "what" to listOf("is", "are", "time", "about"),
            "where" to listOf("are", "is", "did", "we"),
            "can" to listOf("you", "i", "we", "help"),
            "please" to listOf("send", "let", "check", "help"),
            "have" to listOf("a", "you", "to", "fun"),
            "a" to listOf("great", "good", "lot", "little"),
            "the" to listOf("best", "new", "same", "next"),
            "you" to listOf("are", "can", "have", "know"),
            "to" to listOf("see", "get", "be", "the"),
        )

        private val CORE_WORDS: List<String> = listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
            "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
            "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
            "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
            "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
            "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
            "hello", "hey", "hi", "thanks", "thank", "please", "sorry", "yes", "yeah", "okay",
            "sure", "great", "awesome", "amazing", "love", "happy", "morning", "night", "evening", "afternoon",
            "today", "tomorrow", "yesterday", "tonight", "soon", "later", "always", "never", "maybe", "really",
            "very", "much", "more", "here", "where", "why", "need", "help", "send", "call",
            "text", "message", "email", "phone", "home", "office", "meeting", "coffee", "lunch", "dinner",
            "food", "water", "music", "movie", "game", "play", "read", "write", "type", "keyboard",
            "offline", "private", "privacy", "local", "translate", "translation", "language", "voice", "clipboard", "sticker",
            "stickers", "theme", "themes", "custom", "gesture", "swipe", "glide", "fast", "accurate", "smart",
            "friend", "friends", "family", "world", "city", "place", "house", "room", "car", "train",
            "flight", "travel", "trip", "hotel", "airport", "street", "address", "number", "name", "code",
            "app", "android", "device", "screen", "battery", "photo", "image", "video", "file", "save",
            "saved", "pin", "pinned", "copy", "copied", "paste", "pasted", "delete", "clear", "search",
            "find", "found", "open", "close", "start", "stop", "ready", "done", "finish", "finished",
            "check", "checked", "update", "updated", "share", "shared", "create", "created", "edit", "edited",
            "feel", "feeling", "feelings", "hope", "wish", "dream", "idea", "ideas", "plan", "plans",
            "project", "design", "color", "colors", "style", "modern", "clean", "smooth", "bright", "dark",
            "light", "warm", "cool", "nice", "best", "better", "pretty", "beautiful", "cute", "funny",
            "excited", "busy", "free", "tired", "hungry", "thirsty", "sick", "fine", "safe", "welcome",
            "goodbye", "bye", "cheers", "congrats", "congratulations", "birthday", "holiday", "weekend", "monday", "friday",
            "saturday", "sunday", "minute", "minutes", "hour", "hours", "week", "weeks", "month", "year",
            "right", "left", "up", "down", "inside", "outside", "before", "during", "while", "until",
            "between", "around", "through", "without", "within", "every", "each", "many", "few", "several",
            "everything", "something", "nothing", "anything", "everyone", "someone", "anyone", "nobody", "always", "often",
            "sometimes", "usually", "already", "still", "again", "almost", "enough", "quite", "rather", "actually",
            "probably", "definitely", "certainly", "especially", "literally", "seriously", "honestly", "basically", "simply", "quickly",
            "slowly", "easily", "carefully", "together", "alone", "away", "forward", "ahead", "behind", "under",
            "above", "across", "along", "against", "among", "toward", "towards", "upon", "into", "onto",
            "about", "going", "coming", "making", "taking", "getting", "giving", "looking", "working", "thinking",
            "saying", "telling", "asking", "trying", "using", "finding", "waiting", "talking", "walking", "running",
            "eating", "drinking", "sleeping", "watching", "listening", "learning", "building", "coding", "typing", "swiping",
            "translate", "translated", "translating", "spanish", "french", "german", "japanese", "korean", "hindi", "arabic",
            "italian", "portuguese", "chinese", "russian", "turkish", "dutch", "polish", "vietnamese", "thai", "indonesian",
            "i'm", "you're", "we're", "they're", "it's", "that's", "what's", "there's", "here's", "let's",
            "don't", "can't", "won't", "didn't", "doesn't", "isn't", "aren't", "wasn't", "weren't", "haven't",
            "couldn't", "wouldn't", "shouldn't", "i'll", "you'll", "we'll", "i've", "you've", "we've", "they've",
            "first", "second", "third", "last", "next", "previous", "early", "late", "quick", "easy",
            "hard", "simple", "complex", "important", "special", "different", "same", "similar", "real", "true",
            "false", "wrong", "possible", "available", "useful", "helpful", "kind", "sweet", "glad", "proud",
            "please", "excuse", "pardon", "thankful", "grateful", "awesome", "fantastic", "wonderful", "perfect", "brilliant",
            "super", "lovely", "enjoy", "enjoyed", "celebrate", "party", "gift", "present", "surprise", "smile",
            "laugh", "hug", "kiss", "heart", "star", "spark", "magic", "prism", "keys", "studio",
            "remember", "forget", "understand", "believe", "agree", "decide", "choose", "pick", "select", "change",
            "keep", "hold", "bring", "carry", "move", "turn", "push", "pull", "drop", "catch",
            "throw", "break", "fix", "repair", "clean", "wash", "cook", "bake", "order", "deliver",
            "buy", "sell", "pay", "cost", "price", "money", "dollar", "dollars", "card", "bank",
            "book", "paper", "note", "notes", "list", "task", "tasks", "reminder", "calendar", "schedule",
            "weather", "sunny", "rain", "rainy", "cloudy", "snow", "wind", "cold", "hot", "warm",
            "spring", "summer", "autumn", "winter", "morning", "noon", "midnight", "today", "tonight", "forever",
        )
    }
}
