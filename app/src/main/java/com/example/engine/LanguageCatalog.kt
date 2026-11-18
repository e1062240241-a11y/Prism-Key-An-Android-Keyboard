package com.example.engine

import com.google.mlkit.nl.translate.TranslateLanguage

data class SupportedLanguage(
    val code: String,
    val englishName: String,
    val nativeName: String,
    val flag: String,
    val isRtl: Boolean = false,
    val hasBuiltInLexicon: Boolean = false,
)

object LanguageCatalog {
    val allLanguages: List<SupportedLanguage> = listOf(
        SupportedLanguage(TranslateLanguage.ENGLISH, "English", "English", "🇺🇸", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.SPANISH, "Spanish", "Español", "🇪🇸", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.FRENCH, "French", "Français", "🇫🇷", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.GERMAN, "German", "Deutsch", "🇩🇪", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.ITALIAN, "Italian", "Italiano", "🇮🇹", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.PORTUGUESE, "Portuguese", "Português", "🇧🇷", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.HINDI, "Hindi", "हिन्दी", "🇮🇳", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.JAPANESE, "Japanese", "日本語", "🇯🇵", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.KOREAN, "Korean", "한국어", "🇰🇷", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.CHINESE, "Chinese", "中文", "🇨🇳", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.ARABIC, "Arabic", "العربية", "🇸🇦", isRtl = true, hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.RUSSIAN, "Russian", "Русский", "🇷🇺", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.TURKISH, "Turkish", "Türkçe", "🇹🇷", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.DUTCH, "Dutch", "Nederlands", "🇳🇱", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.POLISH, "Polish", "Polski", "🇵🇱", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.VIETNAMESE, "Vietnamese", "Tiếng Việt", "🇻🇳", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.THAI, "Thai", "ไทย", "🇹🇭", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.INDONESIAN, "Indonesian", "Bahasa Indonesia", "🇮🇩", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.UKRAINIAN, "Ukrainian", "Українська", "🇺🇦", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.SWEDISH, "Swedish", "Svenska", "🇸🇪", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.GREEK, "Greek", "Ελληνικά", "🇬🇷", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.HEBREW, "Hebrew", "עברית", "🇮🇱", isRtl = true, hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.BENGALI, "Bengali", "বাংলা", "🇧🇩", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.TAMIL, "Tamil", "தமிழ்", "🇮🇳", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.URDU, "Urdu", "اردو", "🇵🇰", isRtl = true, hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.SWAHILI, "Swahili", "Kiswahili", "🇰🇪", hasBuiltInLexicon = true),
        SupportedLanguage(TranslateLanguage.AFRIKAANS, "Afrikaans", "Afrikaans", "🇿🇦"),
        SupportedLanguage(TranslateLanguage.ALBANIAN, "Albanian", "Shqip", "🇦🇱"),
        SupportedLanguage(TranslateLanguage.BELARUSIAN, "Belarusian", "Беларуская", "🇧🇾"),
        SupportedLanguage(TranslateLanguage.BULGARIAN, "Bulgarian", "Български", "🇧🇬"),
        SupportedLanguage(TranslateLanguage.CATALAN, "Catalan", "Català", "🇪🇸"),
        SupportedLanguage(TranslateLanguage.CROATIAN, "Croatian", "Hrvatski", "🇭🇷"),
        SupportedLanguage(TranslateLanguage.CZECH, "Czech", "Čeština", "🇨🇿"),
        SupportedLanguage(TranslateLanguage.DANISH, "Danish", "Dansk", "🇩🇰"),
        SupportedLanguage(TranslateLanguage.ESPERANTO, "Esperanto", "Esperanto", "🌐"),
        SupportedLanguage(TranslateLanguage.ESTONIAN, "Estonian", "Eesti", "🇪🇪"),
        SupportedLanguage(TranslateLanguage.FINNISH, "Finnish", "Suomi", "🇫🇮"),
        SupportedLanguage(TranslateLanguage.GALICIAN, "Galician", "Galego", "🇪🇸"),
        SupportedLanguage(TranslateLanguage.GEORGIAN, "Georgian", "ქართული", "🇬🇪"),
        SupportedLanguage(TranslateLanguage.GUJARATI, "Gujarati", "ગુજરાતી", "🇮🇳"),
        SupportedLanguage(TranslateLanguage.HAITIAN_CREOLE, "Haitian Creole", "Kreyòl Ayisyen", "🇭🇹"),
        SupportedLanguage(TranslateLanguage.HUNGARIAN, "Hungarian", "Magyar", "🇭🇺"),
        SupportedLanguage(TranslateLanguage.ICELANDIC, "Icelandic", "Íslenska", "🇮🇸"),
        SupportedLanguage(TranslateLanguage.IRISH, "Irish", "Gaeilge", "🇮🇪"),
        SupportedLanguage(TranslateLanguage.KANNADA, "Kannada", "ಕನ್ನಡ", "🇮🇳"),
        SupportedLanguage(TranslateLanguage.LATVIAN, "Latvian", "Latviešu", "🇱🇻"),
        SupportedLanguage(TranslateLanguage.LITHUANIAN, "Lithuanian", "Lietuvių", "🇱🇹"),
        SupportedLanguage(TranslateLanguage.MACEDONIAN, "Macedonian", "Македонски", "🇲🇰"),
        SupportedLanguage(TranslateLanguage.MALAY, "Malay", "Bahasa Melayu", "🇲🇾"),
        SupportedLanguage(TranslateLanguage.MALTESE, "Maltese", "Malti", "🇲🇹"),
        SupportedLanguage(TranslateLanguage.MARATHI, "Marathi", "मराठी", "🇮🇳"),
        SupportedLanguage(TranslateLanguage.NORWEGIAN, "Norwegian", "Norsk", "🇳🇴"),
        SupportedLanguage(TranslateLanguage.PERSIAN, "Persian", "فارسی", "🇮🇷", isRtl = true),
        SupportedLanguage(TranslateLanguage.ROMANIAN, "Romanian", "Română", "🇷🇴"),
        SupportedLanguage(TranslateLanguage.SLOVAK, "Slovak", "Slovenčina", "🇸🇰"),
        SupportedLanguage(TranslateLanguage.SLOVENIAN, "Slovenian", "Slovenščina", "🇸🇮"),
        SupportedLanguage(TranslateLanguage.TAGALOG, "Tagalog", "Tagalog", "🇵🇭"),
        SupportedLanguage(TranslateLanguage.TELUGU, "Telugu", "తెలుగు", "🇮🇳"),
        SupportedLanguage(TranslateLanguage.WELSH, "Welsh", "Cymraeg", "🏴󠁧󠁢󠁷󠁬󠁳󠁿"),
    )

    const val AUTO_DETECT_CODE = "auto"

    val autoDetectLanguage = SupportedLanguage(
        code = AUTO_DETECT_CODE,
        englishName = "Auto-Detect",
        nativeName = "Finds the language for you",
        flag = "✨",
        hasBuiltInLexicon = true,
    )

    fun findSourceByCode(code: String): SupportedLanguage {
        if (code.equals(AUTO_DETECT_CODE, ignoreCase = true)) return autoDetectLanguage
        return findByCode(code)
    }

    fun findByCode(code: String): SupportedLanguage {
        return allLanguages.find { it.code.equals(code, ignoreCase = true) }
            ?: allLanguages.first()
    }
}
