package com.example.engine

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class TranslationEngineKind(val label: String) {
    ML_KIT_NEURAL("Offline Language Pack"),
    BUNDLED_OFFLINE_LEXICON("Built-in Phrasebook"),
    IDENTITY("Same Language"),
}

data class DetectedLanguage(
    val languageCode: String,
    val confidence: Float,
    val usedMlKit: Boolean,
)

data class TranslationResult(
    val sourceText: String,
    val translatedText: String,
    val sourceCode: String,
    val targetCode: String,
    val engineKind: TranslationEngineKind,
    val latencyMs: Long,
    val detectedSourceCode: String? = null,
    val detectedConfidence: Float? = null,
)

class OfflineTranslationEngine(
    private val context: Context,
) {
    private val modelManager by lazy { RemoteModelManager.getInstance() }
    private val languageIdentifier: LanguageIdentifier by lazy {
        LanguageIdentification.getClient(
            LanguageIdentificationOptions.Builder()
                .setConfidenceThreshold(0.35f)
                .build(),
        )
    }
    private val translatorCache = ConcurrentHashMap<String, Translator>()

    private val _isMlKitReady = MutableStateFlow(false)
    val isMlKitReady: StateFlow<Boolean> = _isMlKitReady.asStateFlow()

    private val _downloadedModels = MutableStateFlow<Set<String>>(setOf("en"))
    val downloadedModels: StateFlow<Set<String>> = _downloadedModels.asStateFlow()

    private val _downloadingModels = MutableStateFlow<Set<String>>(emptySet())
    val downloadingModels: StateFlow<Set<String>> = _downloadingModels.asStateFlow()

    private val _lastStatusMessage = MutableStateFlow<String?>(null)
    val lastStatusMessage: StateFlow<String?> = _lastStatusMessage.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val ok = runCatching {
            MlKitContext.initializeIfNeeded(context.applicationContext)
        }.isSuccess
        _isMlKitReady.value = ok
        if (ok) {
            refreshDownloadedModels()
        }
    }

    suspend fun refreshDownloadedModels() = withContext(Dispatchers.IO) {
        runCatching {
            MlKitContext.initializeIfNeeded(context.applicationContext)
            val models = modelManager.getDownloadedModels(TranslateRemoteModel::class.java).awaitResult()
            val codes = models.map { it.language }.toMutableSet().apply { add("en") }
            _downloadedModels.value = codes
            _isMlKitReady.value = true
        }
    }

    suspend fun detectLanguage(text: String): DetectedLanguage = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext DetectedLanguage("en", 0f, usedMlKit = false)
        }

        // 1. Try ML Kit On-Device Language Identification model
        val mlKitHit = runCatching {
            MlKitContext.initializeIfNeeded(context.applicationContext)
            val candidates = languageIdentifier.identifyPossibleLanguages(trimmed).awaitResult()
            candidates.firstOrNull { candidate ->
                candidate.languageTag != LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG &&
                    TranslateLanguage.fromLanguageTag(candidate.languageTag) != null
            }?.let { identified ->
                val mlKitTranslateCode = TranslateLanguage.fromLanguageTag(identified.languageTag)!!
                DetectedLanguage(
                    languageCode = mlKitTranslateCode,
                    confidence = identified.confidence.coerceIn(0f, 1f),
                    usedMlKit = true,
                )
            }
        }.getOrNull()

        if (mlKitHit != null) {
            return@withContext mlKitHit
        }

        // 2. Fallback to instant offline Unicode script & phrase detector
        return@withContext detectLanguageOfflineFallback(trimmed)
    }

    suspend fun translate(
        text: String,
        sourceCode: String,
        targetCode: String,
    ): TranslationResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext TranslationResult(
                sourceText = "",
                translatedText = "",
                sourceCode = sourceCode,
                targetCode = targetCode,
                engineKind = TranslationEngineKind.IDENTITY,
                latencyMs = 0,
            )
        }

        var detectedCode: String? = null
        var detectedConf: Float? = null
        val effectiveSource = if (sourceCode.equals(LanguageCatalog.AUTO_DETECT_CODE, ignoreCase = true)) {
            val detected = detectLanguage(trimmed)
            detectedCode = detected.languageCode
            detectedConf = detected.confidence
            detected.languageCode
        } else {
            sourceCode
        }

        if (effectiveSource == targetCode) {
            return@withContext TranslationResult(
                sourceText = trimmed,
                translatedText = trimmed,
                sourceCode = effectiveSource,
                targetCode = targetCode,
                engineKind = TranslationEngineKind.IDENTITY,
                latencyMs = System.currentTimeMillis() - start,
                detectedSourceCode = detectedCode,
                detectedConfidence = detectedConf,
            )
        }

        val downloaded = _downloadedModels.value
        val canUseMlKit = (effectiveSource in downloaded && targetCode in downloaded)

        if (canUseMlKit) {
            val mlKitResult = runCatching {
                val translator = getOrCreateTranslator(effectiveSource, targetCode)
                translator.translate(trimmed).awaitResult()
            }.getOrNull()

            if (!mlKitResult.isNullOrBlank()) {
                return@withContext TranslationResult(
                    sourceText = trimmed,
                    translatedText = mlKitResult,
                    sourceCode = effectiveSource,
                    targetCode = targetCode,
                    engineKind = TranslationEngineKind.ML_KIT_NEURAL,
                    latencyMs = System.currentTimeMillis() - start,
                    detectedSourceCode = detectedCode,
                    detectedConfidence = detectedConf,
                )
            }
        }

        // Instant zero-network bundled offline lexicon & phrase translation
        val lexiconResult = BundledOfflineLexicon.translate(trimmed, effectiveSource, targetCode)
        TranslationResult(
            sourceText = trimmed,
            translatedText = lexiconResult,
            sourceCode = effectiveSource,
            targetCode = targetCode,
            engineKind = TranslationEngineKind.BUNDLED_OFFLINE_LEXICON,
            latencyMs = System.currentTimeMillis() - start,
            detectedSourceCode = detectedCode,
            detectedConfidence = detectedConf,
        )
    }

    suspend fun downloadLanguagePack(
        langCode: String,
        strictOfflineMode: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (strictOfflineMode) {
            val msg = "Offline-only mode is on. Turn it off briefly in the Translate tab to save this language pack for offline use."
            _lastStatusMessage.value = msg
            return@withContext Result.failure(IllegalStateException(msg))
        }
        _downloadingModels.value = _downloadingModels.value + langCode
        return@withContext runCatching {
            MlKitContext.initializeIfNeeded(context.applicationContext)
            val model = TranslateRemoteModel.Builder(langCode).build()
            val conditions = DownloadConditions.Builder().build()
            modelManager.download(model, conditions).awaitResult()
            refreshDownloadedModels()
            _lastStatusMessage.value = "${LanguageCatalog.findByCode(langCode).englishName} is saved and ready offline!"
        }.onFailure { err ->
            _lastStatusMessage.value = err.localizedMessage ?: "Couldn't download right now — built-in offline translation is still ready."
        }.also {
            _downloadingModels.value = _downloadingModels.value - langCode
        }
    }

    suspend fun deleteLanguagePack(langCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            MlKitContext.initializeIfNeeded(context.applicationContext)
            val model = TranslateRemoteModel.Builder(langCode).build()
            modelManager.deleteDownloadedModel(model).awaitResult()
            refreshDownloadedModels()
            _lastStatusMessage.value = "Removed offline pack for ${LanguageCatalog.findByCode(langCode).englishName}"
        }
    }

    fun clearStatusMessage() {
        _lastStatusMessage.value = null
    }

    fun release() {
        translatorCache.values.forEach { runCatching { it.close() } }
        translatorCache.clear()
        runCatching { languageIdentifier.close() }
    }

    private fun getOrCreateTranslator(sourceCode: String, targetCode: String): Translator {
        val key = "$sourceCode->$targetCode"
        return translatorCache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build()
            Translation.getClient(options)
        }
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exc ->
            if (cont.isActive) cont.resumeWithException(exc)
        }
        addOnCanceledListener {
            cont.cancel()
        }
    }

    companion object {
        fun detectLanguageOfflineFallback(text: String): DetectedLanguage {
            val lower = text.lowercase()

            // Script-based instant detection
            for (ch in text) {
                val code = ch.code
                when {
                    code in 0x3040..0x30FF -> return DetectedLanguage("ja", 0.98f, false)
                    code in 0xAC00..0xD7AF || code in 0x1100..0x11FF -> return DetectedLanguage("ko", 0.98f, false)
                    code in 0x4E00..0x9FFF -> return DetectedLanguage("zh", 0.92f, false)
                    code in 0x0900..0x097F -> return DetectedLanguage("hi", 0.95f, false)
                    code in 0x0600..0x06FF -> return DetectedLanguage("ar", 0.90f, false)
                    code in 0x0400..0x04FF -> return DetectedLanguage("ru", 0.88f, false)
                    code in 0x0370..0x03FF -> return DetectedLanguage("el", 0.96f, false)
                    code in 0x0590..0x05FF -> return DetectedLanguage("he", 0.96f, false)
                    code in 0x0E00..0x0E7F -> return DetectedLanguage("th", 0.97f, false)
                    code in 0x0980..0x09FF -> return DetectedLanguage("bn", 0.96f, false)
                    code in 0x0B80..0x0BFF -> return DetectedLanguage("ta", 0.96f, false)
                }
            }

            // Latin-script common word & diacritic detection
            val spanishHints = listOf("hola", "gracias", "buenos", "buenas", "cómo", "estás", "por favor", "mañana", "adiós", "amigo")
            val frenchHints = listOf("bonjour", "merci", "salut", "comment", "allez", "demain", "aujourd'hui", "s'il vous", "au revoir")
            val germanHints = listOf("hallo", "danke", "guten", "morgen", "nacht", "bitte", "tschüss", "wie geht", "freund")
            val italianHints = listOf("ciao", "grazie", "buongiorno", "come stai", "prego", "arrivederci", "domani")
            val portugueseHints = listOf("olá", "obrigado", "obrigada", "bom dia", "boa noite", "tchau", "amanhã", "você")

            val scores = listOf(
                "es" to spanishHints.count { lower.contains(it) },
                "fr" to frenchHints.count { lower.contains(it) },
                "de" to germanHints.count { lower.contains(it) },
                "it" to italianHints.count { lower.contains(it) },
                "pt" to portugueseHints.count { lower.contains(it) },
            )
            val best = scores.maxByOrNull { it.second }
            if (best != null && best.second > 0) {
                return DetectedLanguage(best.first, 0.85f, false)
            }

            return DetectedLanguage("en", 0.78f, false)
        }
    }
}
