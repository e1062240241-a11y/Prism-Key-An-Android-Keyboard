package com.example.engine

import kotlin.math.hypot

class GestureDecoder(
    private val suggestionEngine: SuggestionEngine,
) {
    /**
     * Normalized path points in [0f..1f] across the alpha key grid (rows Q-P, A-L, Z-M).
     */
    fun decodeGesture(
        normalizedPoints: List<Pair<Float, Float>>,
        maxCandidates: Int = 3,
    ): List<String> {
        if (normalizedPoints.size < 4) return emptyList()

        val startPt = normalizedPoints.first()
        val endPt = normalizedPoints.last()
        val totalPathLength = computePathLength(normalizedPoints)
        if (totalPathLength < 0.06f) return emptyList()

        val sampledStroke = resamplePath(normalizedPoints, SAMPLE_COUNT)
        val startChars = nearestChars(startPt, maxRadius = 0.20f)
        val endChars = nearestChars(endPt, maxRadius = 0.20f)

        if (startChars.isEmpty() || endChars.isEmpty()) return emptyList()

        val candidates = suggestionEngine.allDictionaryWords
            .asSequence()
            .filter { it.length >= 2 }
            .filter { word ->
                val clean = word.filter { it.isLetter() }
                clean.length >= 2 && clean.first() in startChars && clean.last() in endChars
            }
            .mapNotNull { word ->
                val clean = word.filter { it.isLetter() }
                val idealPoints = clean.mapNotNull { SuggestionEngine.KEY_COORDS[it] }
                if (idealPoints.size < 2) return@mapNotNull null

                val idealLength = computePathLength(idealPoints)
                val lengthRatioDiff = kotlin.math.abs(totalPathLength - idealLength)
                if (lengthRatioDiff > 1.35f) return@mapNotNull null

                val resampledIdeal = resamplePath(idealPoints, SAMPLE_COUNT)
                val shapeError = meanPointDistance(sampledStroke, resampledIdeal)
                val startError = dist(startPt, idealPoints.first())
                val endError = dist(endPt, idealPoints.last())
                val freqBonus = (suggestionEngine.wordScore(word).coerceAtMost(800)) / 5000f

                val totalCost = shapeError * 1.4f + startError * 0.9f + endError * 0.9f + lengthRatioDiff * 0.12f - freqBonus
                word to totalCost
            }
            .sortedBy { it.second }
            .take(maxCandidates)
            .map { it.first }
            .toList()

        return candidates
    }

    private fun nearestChars(pt: Pair<Float, Float>, maxRadius: Float): Set<Char> {
        return SuggestionEngine.KEY_COORDS.entries
            .filter { (_, kPt) -> dist(pt, kPt) <= maxRadius }
            .sortedBy { (_, kPt) -> dist(pt, kPt) }
            .take(4)
            .map { it.key }
            .toSet()
    }

    private fun resamplePath(
        points: List<Pair<Float, Float>>,
        targetCount: Int,
    ): List<Pair<Float, Float>> {
        if (points.size <= 1) return List(targetCount) { points.firstOrNull() ?: (0.5f to 0.5f) }
        val totalLen = computePathLength(points).coerceAtLeast(0.001f)
        val step = totalLen / (targetCount - 1)
        val result = ArrayList<Pair<Float, Float>>(targetCount)
        result.add(points.first())

        var accumulated = 0f
        var prev = points.first()
        var idx = 1
        while (idx < points.size && result.size < targetCount) {
            val curr = points[idx]
            val seg = dist(prev, curr)
            if (accumulated + seg >= step) {
                val t = ((step - accumulated) / seg.coerceAtLeast(1e-5f)).coerceIn(0f, 1f)
                val nx = prev.first + t * (curr.first - prev.first)
                val ny = prev.second + t * (curr.second - prev.second)
                val newPt = nx to ny
                result.add(newPt)
                prev = newPt
                accumulated = 0f
            } else {
                accumulated += seg
                prev = curr
                idx++
            }
        }
        while (result.size < targetCount) {
            result.add(points.last())
        }
        return result
    }

    private fun meanPointDistance(
        a: List<Pair<Float, Float>>,
        b: List<Pair<Float, Float>>,
    ): Float {
        var sum = 0f
        val count = minOf(a.size, b.size)
        for (i in 0 until count) {
            sum += dist(a[i], b[i])
        }
        return sum / count.coerceAtLeast(1)
    }

    private fun computePathLength(points: List<Pair<Float, Float>>): Float {
        var total = 0f
        for (i in 1 until points.size) {
            total += dist(points[i - 1], points[i])
        }
        return total
    }

    private fun dist(a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
        return hypot(a.first - b.first, a.second - b.second)
    }

    companion object {
        private const val SAMPLE_COUNT = 24
    }
}
