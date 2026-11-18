package com.example.engine

/**
 * Bundled zero-network multi-language phrase and vocabulary engine.
 * Enables instant offline translation even before ML Kit neural packs are downloaded.
 */
object BundledOfflineLexicon {

    // Canonical English phrase/word -> Map<langCode, translatedText>
    private val enToLang: Map<String, Map<String, String>> = mapOf(
        // Multi-word common phrases (longest-match first)
        "good morning" to mapOf(
            "es" to "buenos días", "fr" to "bonjour", "de" to "guten Morgen", "it" to "buongiorno",
            "pt" to "bom dia", "hi" to "सुप्रभात", "ja" to "おはようございます", "ko" to "좋은 아침이에요",
            "zh" to "早上好", "ar" to "صباح الخير", "ru" to "доброе утро", "tr" to "günaydın",
            "nl" to "goedemorgen", "pl" to "dzień dobry", "vi" to "chào buổi sáng", "th" to "สวัสดีตอนเช้า",
            "id" to "selamat pagi", "uk" to "доброго ранку", "sv" to "god morgon", "el" to "καλημέρα",
            "he" to "בוקר טוב", "bn" to "শুভ সকাল", "ta" to "காலை வணக்கம்", "ur" to "صبح بخیر", "sw" to "habari za asubuhi",
        ),
        "good night" to mapOf(
            "es" to "buenas noches", "fr" to "bonne nuit", "de" to "gute Nacht", "it" to "buonanotte",
            "pt" to "boa noite", "hi" to "शुभ रात्रि", "ja" to "おやすみなさい", "ko" to "안녕히 주무세요",
            "zh" to "晚安", "ar" to "تصبح على خير", "ru" to "спокойной ночи", "tr" to "iyi geceler",
            "nl" to "goedenacht", "pl" to "dobranoc", "vi" to "chúc ngủ ngon", "th" to "ราตรีสวัสดิ์",
            "id" to "selamat malam", "uk" to "на добраніч", "sv" to "god natt", "el" to "καληνύχτα",
            "he" to "לילה טוב", "bn" to "শুভ রাত্রি", "ta" to "இனிய இரவு", "ur" to "شب بخیر", "sw" to "usiku mwema",
        ),
        "how are you" to mapOf(
            "es" to "¿cómo estás?", "fr" to "comment allez-vous ?", "de" to "wie geht es dir?", "it" to "come stai?",
            "pt" to "como você está?", "hi" to "आप कैसे हैं?", "ja" to "お元気ですか？", "ko" to "어떻게 지내세요?",
            "zh" to "你好吗？", "ar" to "كيف حالك؟", "ru" to "как дела?", "tr" to "nasılsın?",
            "nl" to "hoe gaat het?", "pl" to "jak się masz?", "vi" to "bạn khỏe không?", "th" to "สบายดีไหม",
            "id" to "apa kabar?", "uk" to "як справи?", "sv" to "hur mår du?", "el" to "τι κάνεις;",
            "he" to "מה שלומך?", "bn" to "আপনি কেমন আছেন?", "ta" to "நீங்கள் எப்படி இருக்கிறீர்கள்?", "ur" to "آپ کیسے ہیں؟", "sw" to "habari yako?",
        ),
        "thank you" to mapOf(
            "es" to "gracias", "fr" to "merci", "de" to "danke", "it" to "grazie",
            "pt" to "obrigado", "hi" to "धन्यवाद", "ja" to "ありがとうございます", "ko" to "감사합니다",
            "zh" to "谢谢", "ar" to "شكراً لك", "ru" to "спасибо", "tr" to "teşekkür ederim",
            "nl" to "dank je", "pl" to "dziękuję", "vi" to "cảm ơn", "th" to "ขอบคุณ",
            "id" to "terima kasih", "uk" to "дякую", "sv" to "tack", "el" to "ευχαριστώ",
            "he" to "תודה", "bn" to "ধন্যবাদ", "ta" to "நன்றி", "ur" to "شکریہ", "sw" to "asante",
        ),
        "you're welcome" to mapOf(
            "es" to "de nada", "fr" to "de rien", "de" to "bitte schön", "it" to "prego",
            "pt" to "de nada", "hi" to "आपका स्वागत है", "ja" to "どういたしまして", "ko" to "천만에요",
            "zh" to "不客气", "ar" to "عفواً", "ru" to "пожалуйста", "tr" to "rica ederim",
            "nl" to "graag gedaan", "pl" to "nie ma za co", "vi" to "không có gì", "th" to "ยินดี",
            "id" to "sama-sama", "uk" to "будь ласка", "sv" to "varsågod", "el" to "παρακαλώ",
            "he" to "בבקשה", "bn" to "স্বাগতম", "ta" to "நல்வரவு", "ur" to "کوئی بات نہیں", "sw" to "karibu",
        ),
        "i love you" to mapOf(
            "es" to "te quiero", "fr" to "je t'aime", "de" to "ich liebe dich", "it" to "ti amo",
            "pt" to "eu te amo", "hi" to "मैं तुमसे प्यार करता हूँ", "ja" to "愛しています", "ko" to "사랑해요",
            "zh" to "我爱你", "ar" to "أحبك", "ru" to "я тебя люблю", "tr" to "seni seviyorum",
            "nl" to "ik hou van jou", "pl" to "kocham cię", "vi" to "tôi yêu bạn", "th" to "ฉันรักคุณ",
            "id" to "aku cinta kamu", "uk" to "я тебе люблю", "sv" to "jag älskar dig", "el" to "σ' αγαπώ",
            "he" to "אני אוהב אותך", "bn" to "আমি তোমায় ভালোবাসি", "ta" to "நான் உன்னை காதலிக்கிறேன்", "ur" to "میں تم سے پیار کرتا ہوں", "sw" to "nakupenda",
        ),
        "on my way" to mapOf(
            "es" to "en camino", "fr" to "j'arrive", "de" to "bin unterwegs", "it" to "sto arrivando",
            "pt" to "a caminho", "hi" to "रास्ते में हूँ", "ja" to "今向かっています", "ko" to "가는 중이에요",
            "zh" to "我在路上了", "ar" to "أنا في طريقي", "ru" to "я уже в пути", "tr" to "yoldayım",
            "nl" to "onderweg", "pl" to "jestem w drodze", "vi" to "tôi đang trên đường đến", "th" to "กำลังเดินทาง",
            "id" to "sedang di jalan", "uk" to "я вже в дорозі", "sv" to "på väg", "el" to "είμαι καθ' οδόν",
            "he" to "אני בדרך", "bn" to "আমি আসছি", "ta" to "வந்து கொண்டிருக்கிறேன்", "ur" to "میں راستے میں ہوں", "sw" to "niko njiani",
        ),
        "see you soon" to mapOf(
            "es" to "hasta pronto", "fr" to "à bientôt", "de" to "bis bald", "it" to "a presto",
            "pt" to "até breve", "hi" to "जल्द मिलते हैं", "ja" to "またね", "ko" to "곧 봐요",
            "zh" to "待会见", "ar" to "أراك قريباً", "ru" to "до скорого", "tr" to "yakında görüşürüz",
            "nl" to "tot snel", "pl" to "do zobaczenia wkrótce", "vi" to "hẹn gặp lại", "th" to "แล้วพบกัน",
            "id" to "sampai jumpa", "uk" to "до скорої зустрічі", "sv" to "vi ses snart", "el" to "τα λέμε σύντομα",
            "he" to "נתראה בקרוב", "bn" to "শীঘ্রই দেখা হবে", "ta" to "விரைவில் சந்திப்போம்", "ur" to "جلد ملتے ہیں", "sw" to "tutaonana hivi karibuni",
        ),
        "excuse me" to mapOf(
            "es" to "disculpe", "fr" to "excusez-moi", "de" to "entschuldigung", "it" to "mi scusi",
            "pt" to "com licença", "hi" to "माफ़ कीजिए", "ja" to "すみません", "ko" to "실례합니다",
            "zh" to "打扰一下", "ar" to "عفواً", "ru" to "извините", "tr" to "afedersiniz",
            "nl" to "pardon", "pl" to "przepraszam", "vi" to "xin lỗi", "th" to "ขอโทษ",
            "id" to "permisi", "uk" to "вибачте", "sv" to "ursäkta", "el" to "συγγνώμη",
            "he" to "סליחה", "bn" to "মাফ করবেন", "ta" to "மன்னிக்கவும்", "ur" to "معاف کیجیے", "sw" to "samahani",
        ),
        "where is" to mapOf(
            "es" to "dónde está", "fr" to "où est", "de" to "wo ist", "it" to "dov'è",
            "pt" to "onde fica", "hi" to "कहाँ है", "ja" to "どこですか", "ko" to "어디예요",
            "zh" to "在哪里", "ar" to "أين", "ru" to "где находится", "tr" to "nerede",
            "nl" to "waar is", "pl" to "gdzie jest", "vi" to "ở đâu", "th" to "อยู่ที่ไหน",
            "id" to "di mana", "uk" to "де знаходиться", "sv" to "var är", "el" to "πού είναι",
            "he" to "איפה", "bn" to "কোথায়", "ta" to "எங்கே உள்ளது", "ur" to "کہاں ہے", "sw" to "iko wapi",
        ),
        "how much" to mapOf(
            "es" to "cuánto cuesta", "fr" to "combien", "de" to "wie viel", "it" to "quanto costa",
            "pt" to "quanto custa", "hi" to "कितना", "ja" to "いくらですか", "ko" to "얼마예요",
            "zh" to "多少钱", "ar" to "بكم هذا", "ru" to "сколько стоит", "tr" to "ne kadar",
            "nl" to "hoeveel", "pl" to "ile kosztuje", "vi" to "bao nhiêu", "th" to "เท่าไหร่",
            "id" to "berapa", "uk" to "скільки коштує", "sv" to "hur mycket", "el" to "πόσο κάνει",
            "he" to "כמה זה עולה", "bn" to "কত দাম", "ta" to "எவ்வளவு", "ur" to "کتنے کا ہے", "sw" to "ni kiasi gani",
        ),
        "i am fine" to mapOf(
            "es" to "estoy bien", "fr" to "je vais bien", "de" to "mir geht es gut", "it" to "sto bene",
            "pt" to "estou bem", "hi" to "मैं ठीक हूँ", "ja" to "元気です", "ko" to "잘 지내요",
            "zh" to "我很好", "ar" to "أنا بخير", "ru" to "у меня всё хорошо", "tr" to "iyiyim",
            "nl" to "het gaat goed", "pl" to "wszystko w porządku", "vi" to "tôi khỏe", "th" to "ฉันสบายดี",
            "id" to "saya baik-baik saja", "uk" to "у мене все добре", "sv" to "jag mår bra", "el" to "είμαι καλά",
            "he" to "אני בסדר", "bn" to "আমি ভালো আছি", "ta" to "நான் நலமாக இருக்கிறேன்", "ur" to "میں ٹھیک ہوں", "sw" to "niko salama",
        ),

        // Core single words
        "hello" to mapOf(
            "es" to "hola", "fr" to "bonjour", "de" to "hallo", "it" to "ciao", "pt" to "olá",
            "hi" to "नमस्ते", "ja" to "こんにちは", "ko" to "안녕하세요", "zh" to "你好", "ar" to "مرحباً",
            "ru" to "привет", "tr" to "merhaba", "nl" to "hallo", "pl" to "cześć", "vi" to "xin chào",
            "th" to "สวัสดี", "id" to "halo", "uk" to "привіт", "sv" to "hej", "el" to "γεια",
            "he" to "שלום", "bn" to "হ্যালো", "ta" to "வணக்கம்", "ur" to "سلام", "sw" to "jambo",
        ),
        "hi" to mapOf(
            "es" to "hola", "fr" to "salut", "de" to "hallo", "it" to "ciao", "pt" to "oi",
            "hi" to "नमस्ते", "ja" to "やあ", "ko" to "안녕", "zh" to "嗨", "ar" to "أهلاً",
            "ru" to "привет", "tr" to "selam", "nl" to "hoi", "pl" to "hej", "vi" to "chào",
            "th" to "หวัดดี", "id" to "hai", "uk" to "привіт", "sv" to "hej", "el" to "γεια",
            "he" to "היי", "bn" to "হাই", "ta" to "ஹாய்", "ur" to "سلام", "sw" to "mambo",
        ),
        "goodbye" to mapOf(
            "es" to "adiós", "fr" to "au revoir", "de" to "auf Wiedersehen", "it" to "arrivederci", "pt" to "tchau",
            "hi" to "अलविदा", "ja" to "さようなら", "ko" to "안녕히 가세요", "zh" to "再见", "ar" to "وداعاً",
            "ru" to "до свидания", "tr" to "hoşça kal", "nl" to "tot ziens", "pl" to "do widzenia", "vi" to "tạm biệt",
            "th" to "ลาก่อน", "id" to "selamat tinggal", "uk" to "до побачення", "sv" to "hej då", "el" to "αντίο",
            "he" to "להתראות", "bn" to "বিদায়", "ta" to "பிரியாவிடை", "ur" to "خدا حافظ", "sw" to "kwaheri",
        ),
        "please" to mapOf(
            "es" to "por favor", "fr" to "s'il vous plaît", "de" to "bitte", "it" to "per favore", "pt" to "por favor",
            "hi" to "कृपया", "ja" to "お願いします", "ko" to "제발", "zh" to "请", "ar" to "من فضلك",
            "ru" to "пожалуйста", "tr" to "lütfen", "nl" to "alsjeblieft", "pl" to "proszę", "vi" to "làm ơn",
            "th" to "กรุณา", "id" to "tolong", "uk" to "будь ласка", "sv" to "snälla", "el" to "παρακαλώ",
            "he" to "בבקשה", "bn" to "দয়া করে", "ta" to "தயவுசெய்து", "ur" to "براہ کرم", "sw" to "tafadhali",
        ),
        "thanks" to mapOf(
            "es" to "gracias", "fr" to "merci", "de" to "danke", "it" to "grazie", "pt" to "obrigado",
            "hi" to "धन्यवाद", "ja" to "ありがとう", "ko" to "고마워", "zh" to "谢谢", "ar" to "شكراً",
            "ru" to "спасибо", "tr" to "teşekkürler", "nl" to "bedankt", "pl" to "dzięki", "vi" to "cảm ơn",
            "th" to "ขอบคุณ", "id" to "makasih", "uk" to "дякую", "sv" to "tack", "el" to "ευχαριστώ",
            "he" to "תודה", "bn" to "ধন্যবাদ", "ta" to "நன்றி", "ur" to "شکریہ", "sw" to "asante",
        ),
        "sorry" to mapOf(
            "es" to "lo siento", "fr" to "désolé", "de" to "entschuldigung", "it" to "mi dispiace", "pt" to "desculpa",
            "hi" to "माफ़ करें", "ja" to "ごめんなさい", "ko" to "미안해요", "zh" to "对不起", "ar" to "آسف",
            "ru" to "извини", "tr" to "üzgünüm", "nl" to "sorry", "pl" to "przepraszam", "vi" to "xin lỗi",
            "th" to "ขอโทษ", "id" to "maaf", "uk" to "вибач", "sv" to "förlåt", "el" to "συγγνώμη",
            "he" to "סליחה", "bn" to "দুঃখিত", "ta" to "மன்னிக்கவும்", "ur" to "معاف کرنا", "sw" to "pole",
        ),
        "yes" to mapOf(
            "es" to "sí", "fr" to "oui", "de" to "ja", "it" to "sì", "pt" to "sim",
            "hi" to "हाँ", "ja" to "はい", "ko" to "네", "zh" to "是", "ar" to "نعم",
            "ru" to "да", "tr" to "evet", "nl" to "ja", "pl" to "tak", "vi" to "vâng",
            "th" to "ใช่", "id" to "ya", "uk" to "так", "sv" to "ja", "el" to "ναι",
            "he" to "כן", "bn" to "হ্যাঁ", "ta" to "ஆம்", "ur" to "جی ہاں", "sw" to "ndiyo",
        ),
        "no" to mapOf(
            "es" to "no", "fr" to "non", "de" to "nein", "it" to "no", "pt" to "não",
            "hi" to "नहीं", "ja" to "いいえ", "ko" to "아니요", "zh" to "不", "ar" to "لا",
            "ru" to "нет", "tr" to "hayır", "nl" to "nee", "pl" to "nie", "vi" to "không",
            "th" to "ไม่", "id" to "tidak", "uk" to "ні", "sv" to "nej", "el" to "όχι",
            "he" to "לא", "bn" to "না", "ta" to "இல்லை", "ur" to "نہیں", "sw" to "hapana",
        ),
        "help" to mapOf(
            "es" to "ayuda", "fr" to "aide", "de" to "hilfe", "it" to "aiuto", "pt" to "ajuda",
            "hi" to "मदद", "ja" to "助けて", "ko" to "도움", "zh" to "帮助", "ar" to "مساعدة",
            "ru" to "помощь", "tr" to "yardım", "nl" to "hulp", "pl" to "pomoc", "vi" to "giúp đỡ",
            "th" to "ช่วยด้วย", "id" to "bantuan", "uk" to "допомога", "sv" to "hjälp", "el" to "βοήθεια",
            "he" to "עזרה", "bn" to "সাহায্য", "ta" to "உதவி", "ur" to "مدد", "sw" to "msaada",
        ),
        "coffee" to mapOf(
            "es" to "café", "fr" to "café", "de" to "Kaffee", "it" to "caffè", "pt" to "café",
            "hi" to "कॉफ़ी", "ja" to "コーヒー", "ko" to "커피", "zh" to "咖啡", "ar" to "قهوة",
            "ru" to "кофе", "tr" to "kahve", "nl" to "koffie", "pl" to "kawa", "vi" to "cà phê",
            "th" to "กาแฟ", "id" to "kopi", "uk" to "кава", "sv" to "kaffe", "el" to "καφές",
            "he" to "קפה", "bn" to "কফি", "ta" to "காபி", "ur" to "کافی", "sw" to "kahawa",
        ),
        "water" to mapOf(
            "es" to "agua", "fr" to "eau", "de" to "Wasser", "it" to "acqua", "pt" to "água",
            "hi" to "पानी", "ja" to "水", "ko" to "물", "zh" to "水", "ar" to "ماء",
            "ru" to "вода", "tr" to "su", "nl" to "water", "pl" to "woda", "vi" to "nước",
            "th" to "น้ำ", "id" to "air", "uk" to "вода", "sv" to "vatten", "el" to "νερό",
            "he" to "מים", "bn" to "পানি", "ta" to "தண்ணீர்", "ur" to "پانی", "sw" to "maji",
        ),
        "food" to mapOf(
            "es" to "comida", "fr" to "nourriture", "de" to "Essen", "it" to "cibo", "pt" to "comida",
            "hi" to "खाना", "ja" to "食べ物", "ko" to "음식", "zh" to "食物", "ar" to "طعام",
            "ru" to "еда", "tr" to "yemek", "nl" to "eten", "pl" to "jedzenie", "vi" to "đồ ăn",
            "th" to "อาหาร", "id" to "makanan", "uk" to "їжа", "sv" to "mat", "el" to "φαγητό",
            "he" to "אוכל", "bn" to "খাবার", "ta" to "உணவு", "ur" to "کھانا", "sw" to "chakula",
        ),
        "friend" to mapOf(
            "es" to "amigo", "fr" to "ami", "de" to "Freund", "it" to "amico", "pt" to "amigo",
            "hi" to "दोस्त", "ja" to "友達", "ko" to "친구", "zh" to "朋友", "ar" to "صديق",
            "ru" to "друг", "tr" to "arkadaş", "nl" to "vriend", "pl" to "przyjaciel", "vi" to "bạn bè",
            "th" to "เพื่อน", "id" to "teman", "uk" to "друг", "sv" to "vän", "el" to "φίλος",
            "he" to "חבר", "bn" to "বন্ধু", "ta" to "நண்பர்", "ur" to "دوست", "sw" to "rafiki",
        ),
        "love" to mapOf(
            "es" to "amor", "fr" to "amour", "de" to "Liebe", "it" to "amore", "pt" to "amor",
            "hi" to "प्यार", "ja" to "愛", "ko" to "사랑", "zh" to "爱", "ar" to "حب",
            "ru" to "любовь", "tr" to "aşk", "nl" to "liefde", "pl" to "miłość", "vi" to "tình yêu",
            "th" to "ความรัก", "id" to "cinta", "uk" to "кохання", "sv" to "kärlek", "el" to "αγάπη",
            "he" to "אהבה", "bn" to "ভালোবাসা", "ta" to "அன்பு", "ur" to "محبت", "sw" to "upendo",
        ),
        "today" to mapOf(
            "es" to "hoy", "fr" to "aujourd'hui", "de" to "heute", "it" to "oggi", "pt" to "hoje",
            "hi" to "आज", "ja" to "今日", "ko" to "오늘", "zh" to "今天", "ar" to "اليوم",
            "ru" to "сегодня", "tr" to "bugün", "nl" to "vandaag", "pl" to "dzisiaj", "vi" to "hôm nay",
            "th" to "วันนี้", "id" to "hari ini", "uk" to "сьогодні", "sv" to "idag", "el" to "σήμερα",
            "he" to "היום", "bn" to "আজ", "ta" to "இன்று", "ur" to "آج", "sw" to "leo",
        ),
        "tomorrow" to mapOf(
            "es" to "mañana", "fr" to "demain", "de" to "morgen", "it" to "domani", "pt" to "amanhã",
            "hi" to "कल", "ja" to "明日", "ko" to "내일", "zh" to "明天", "ar" to "غداً",
            "ru" to "завтра", "tr" to "yarın", "nl" to "morgen", "pl" to "jutro", "vi" to "ngày mai",
            "th" to "พรุ่งนี้", "id" to "besok", "uk" to "завтра", "sv" to "imorgon", "el" to "αύριο",
            "he" to "מחר", "bn" to "আগামীকাল", "ta" to "நாளை", "ur" to "کل", "sw" to "kesho",
        ),
        "home" to mapOf(
            "es" to "casa", "fr" to "maison", "de" to "Zuhause", "it" to "casa", "pt" to "casa",
            "hi" to "घर", "ja" to "家", "ko" to "집", "zh" to "家", "ar" to "بيت",
            "ru" to "дом", "tr" to "ev", "nl" to "thuis", "pl" to "dom", "vi" to "nhà",
            "th" to "บ้าน", "id" to "rumah", "uk" to "дім", "sv" to "hem", "el" to "σπίτι",
            "he" to "בית", "bn" to "বাড়ি", "ta" to "வீடு", "ur" to "گھر", "sw" to "nyumbani",
        ),
        "work" to mapOf(
            "es" to "trabajo", "fr" to "travail", "de" to "Arbeit", "it" to "lavoro", "pt" to "trabalho",
            "hi" to "काम", "ja" to "仕事", "ko" to "일", "zh" to "工作", "ar" to "عمل",
            "ru" to "работа", "tr" to "iş", "nl" to "werk", "pl" to "praca", "vi" to "công việc",
            "th" to "งาน", "id" to "kerja", "uk" to "робота", "sv" to "arbete", "el" to "δουλειά",
            "he" to "עבודה", "bn" to "কাজ", "ta" to "வேலை", "ur" to "کام", "sw" to "kazi",
        ),
        "meeting" to mapOf(
            "es" to "reunión", "fr" to "réunion", "de" to "Treffen", "it" to "riunione", "pt" to "reunião",
            "hi" to "मीटिंग", "ja" to "会議", "ko" to "회의", "zh" to "会议", "ar" to "اجتماع",
            "ru" to "встреча", "tr" to "toplantı", "nl" to "vergadering", "pl" to "spotkanie", "vi" to "cuộc họp",
            "th" to "การประชุม", "id" to "rapat", "uk" to "зустріч", "sv" to "möte", "el" to "συνάντηση",
            "he" to "פגישה", "bn" to "সভা", "ta" to "கூட்டம்", "ur" to "میٹنگ", "sw" to "mkutano",
        ),
        "happy" to mapOf(
            "es" to "feliz", "fr" to "heureux", "de" to "glücklich", "it" to "felice", "pt" to "feliz",
            "hi" to "खुश", "ja" to "幸せ", "ko" to "행복해요", "zh" to "开心", "ar" to "سعيد",
            "ru" to "счастливый", "tr" to "mutlu", "nl" to "blij", "pl" to "szczęśliwy", "vi" to "hạnh phúc",
            "th" to "มีความสุข", "id" to "bahagia", "uk" to "щасливий", "sv" to "glad", "el" to "χαρούμενος",
            "he" to "שמח", "bn" to "খুশি", "ta" to "மகிழ்ச்சி", "ur" to "خوش", "sw" to "furaha",
        ),
        "good" to mapOf(
            "es" to "bueno", "fr" to "bon", "de" to "gut", "it" to "buono", "pt" to "bom",
            "hi" to "अच्छा", "ja" to "良い", "ko" to "좋아요", "zh" to "好", "ar" to "جيد",
            "ru" to "хорошо", "tr" to "iyi", "nl" to "goed", "pl" to "dobry", "vi" to "tốt",
            "th" to "ดี", "id" to "bagus", "uk" to "добре", "sv" to "bra", "el" to "καλός",
            "he" to "טוב", "bn" to "ভালো", "ta" to "நல்ல", "ur" to "اچھا", "sw" to "nzuri",
        ),
        "great" to mapOf(
            "es" to "genial", "fr" to "super", "de" to "großartig", "it" to "fantastico", "pt" to "ótimo",
            "hi" to "बढ़िया", "ja" to "素晴らしい", "ko" to "멋져요", "zh" to "太棒了", "ar" to "رائع",
            "ru" to "отлично", "tr" to "harika", "nl" to "geweldig", "pl" to "świetnie", "vi" to "tuyệt vời",
            "th" to "เยี่ยม", "id" to "hebat", "uk" to "чудово", "sv" to "toppen", "el" to "υπέροχα",
            "he" to "מעולה", "bn" to "দারুণ", "ta" to "சிறப்பு", "ur" to "زبردست", "sw" to "bora",
        ),
        "awesome" to mapOf(
            "es" to "increíble", "fr" to "génial", "de" to "super", "it" to "fantastico", "pt" to "incrível",
            "hi" to "कमाल", "ja" to "すごい", "ko" to "대박", "zh" to "太赞了", "ar" to "مذهل",
            "ru" to "потрясающе", "tr" to "müthiş", "nl" to "geweldig", "pl" to "super", "vi" to "quá đỉnh",
            "th" to "สุดยอด", "id" to "keren", "uk" to "неймовірно", "sv" to "grymt", "el" to "φοβερό",
            "he" to "מדהים", "bn" to "অসাধারণ", "ta" to "அருமை", "ur" to "لاجواب", "sw" to "ajabu",
        ),
        "welcome" to mapOf(
            "es" to "bienvenido", "fr" to "bienvenue", "de" to "willkommen", "it" to "benvenuto", "pt" to "bem-vindo",
            "hi" to "स्वागत है", "ja" to "ようこそ", "ko" to "환영합니다", "zh" to "欢迎", "ar" to "أهلاً وسهلاً",
            "ru" to "добро пожаловать", "tr" to "hoş geldin", "nl" to "welkom", "pl" to "witamy", "vi" to "chào mừng",
            "th" to "ยินดีต้อนรับ", "id" to "selamat datang", "uk" to "ласкаво просимо", "sv" to "välkommen", "el" to "καλώς ήρθατε",
            "he" to "ברוך הבא", "bn" to "স্বাগতম", "ta" to "நல்வரவு", "ur" to "خوش آمدید", "sw" to "karibu",
        ),
        "keyboard" to mapOf(
            "es" to "teclado", "fr" to "clavier", "de" to "Tastatur", "it" to "tastiera", "pt" to "teclado",
            "hi" to "कीबोर्ड", "ja" to "キーボード", "ko" to "키보드", "zh" to "键盘", "ar" to "لوحة المفاتيح",
            "ru" to "клавиатура", "tr" to "klavye", "nl" to "toetsenbord", "pl" to "klawiatura", "vi" to "bàn phím",
            "th" to "แป้นพิมพ์", "id" to "papan ketik", "uk" to "клавіатура", "sv" to "tangentbord", "el" to "πληκτρολόγιο",
            "he" to "מקלדת", "bn" to "কীবোর্ড", "ta" to "விசைப்பலகை", "ur" to "کی بورڈ", "sw" to "kibodi",
        ),
        "offline" to mapOf(
            "es" to "sin conexión", "fr" to "hors ligne", "de" to "offline", "it" to "offline", "pt" to "offline",
            "hi" to "ऑफ़लाइन", "ja" to "オフライン", "ko" to "오프라인", "zh" to "离线", "ar" to "بدون إنترنت",
            "ru" to "офлайн", "tr" to "çevrimdışı", "nl" to "offline", "pl" to "offline", "vi" to "ngoại tuyến",
            "th" to "ออฟไลน์", "id" to "luring", "uk" to "офлайн", "sv" to "offline", "el" to "εκτός σύνδεσης",
            "he" to "לא מקוון", "bn" to "অফলাইন", "ta" to "ஆஃப்லைன்", "ur" to "آف لائن", "sw" to "nje ya mtandao",
        ),
        "private" to mapOf(
            "es" to "privado", "fr" to "privé", "de" to "privat", "it" to "privato", "pt" to "privado",
            "hi" to "निजी", "ja" to "プライベート", "ko" to "비공개", "zh" to "私密", "ar" to "خاص",
            "ru" to "приватный", "tr" to "gizli", "nl" to "privé", "pl" to "prywatny", "vi" to "riêng tư",
            "th" to "ส่วนตัว", "id" to "pribadi", "uk" to "приватний", "sv" to "privat", "el" to "ιδιωτικό",
            "he" to "פרטי", "bn" to "ব্যক্তিগত", "ta" to "தனிப்பட்ட", "ur" to "نجی", "sw" to "binafsi",
        ),
        "ok" to mapOf(
            "es" to "vale", "fr" to "d'accord", "de" to "okay", "it" to "va bene", "pt" to "tudo bem",
            "hi" to "ठीक है", "ja" to "了解", "ko" to "알겠어요", "zh" to "好的", "ar" to "حسناً",
            "ru" to "хорошо", "tr" to "tamam", "nl" to "oké", "pl" to "ok", "vi" to "được",
            "th" to "โอเค", "id" to "oke", "uk" to "гаразд", "sv" to "okej", "el" to "εντάξει",
            "he" to "בסדר", "bn" to "ঠিক আছে", "ta" to "சரி", "ur" to "ٹھیک ہے", "sw" to "sawa",
        ),
        "i" to mapOf(
            "es" to "yo", "fr" to "je", "de" to "ich", "it" to "io", "pt" to "eu",
            "hi" to "मैं", "ja" to "私", "ko" to "나", "zh" to "我", "ar" to "أنا",
            "ru" to "я", "tr" to "ben", "nl" to "ik", "pl" to "ja", "vi" to "tôi",
            "th" to "ฉัน", "id" to "saya", "uk" to "я", "sv" to "jag", "el" to "εγώ",
            "he" to "אני", "bn" to "আমি", "ta" to "நான்", "ur" to "میں", "sw" to "mimi",
        ),
        "you" to mapOf(
            "es" to "tú", "fr" to "vous", "de" to "du", "it" to "tu", "pt" to "você",
            "hi" to "आप", "ja" to "あなた", "ko" to "당신", "zh" to "你", "ar" to "أنت",
            "ru" to "ты", "tr" to "sen", "nl" to "jij", "pl" to "ty", "vi" to "bạn",
            "th" to "คุณ", "id" to "kamu", "uk" to "ти", "sv" to "du", "el" to "εσύ",
            "he" to "אתה", "bn" to "আপনি", "ta" to "நீங்கள்", "ur" to "آپ", "sw" to "wewe",
        ),
        "and" to mapOf(
            "es" to "y", "fr" to "et", "de" to "und", "it" to "e", "pt" to "e",
            "hi" to "और", "ja" to "と", "ko" to "그리고", "zh" to "和", "ar" to "و",
            "ru" to "и", "tr" to "ve", "nl" to "en", "pl" to "i", "vi" to "và",
            "th" to "และ", "id" to "dan", "uk" to "і", "sv" to "och", "el" to "και",
            "he" to "ו", "bn" to "এবং", "ta" to "மற்றும்", "ur" to "اور", "sw" to "na",
        ),
        "time" to mapOf(
            "es" to "tiempo", "fr" to "temps", "de" to "Zeit", "it" to "tempo", "pt" to "tempo",
            "hi" to "समय", "ja" to "時間", "ko" to "시간", "zh" to "时间", "ar" to "وقت",
            "ru" to "время", "tr" to "zaman", "nl" to "tijd", "pl" to "czas", "vi" to "thời gian",
            "th" to "เวลา", "id" to "waktu", "uk" to "час", "sv" to "tid", "el" to "χρόνος",
            "he" to "זמן", "bn" to "সময়", "ta" to "நேரம்", "ur" to "وقت", "sw" to "wakati",
        ),
        "soon" to mapOf(
            "es" to "pronto", "fr" to "bientôt", "de" to "bald", "it" to "presto", "pt" to "em breve",
            "hi" to "जल्दी", "ja" to "すぐに", "ko" to "곧", "zh" to "很快", "ar" to "قريباً",
            "ru" to "скоро", "tr" to "yakında", "nl" to "binnenkort", "pl" to "wkrótce", "vi" to "sớm",
            "th" to "เร็วๆ นี้", "id" to "segera", "uk" to "незабаром", "sv" to "snart", "el" to "σύντομα",
            "he" to "בקרוב", "bn" to "শীঘ্রই", "ta" to "விரைவில்", "ur" to "جلد", "sw" to "hivi karibuni",
        ),
        "now" to mapOf(
            "es" to "ahora", "fr" to "maintenant", "de" to "jetzt", "it" to "ora", "pt" to "agora",
            "hi" to "अभी", "ja" to "今", "ko" to "지금", "zh" to "现在", "ar" to "الآن",
            "ru" to "сейчас", "tr" to "şimdi", "nl" to "nu", "pl" to "teraz", "vi" to "bây giờ",
            "th" to "ตอนนี้", "id" to "sekarang", "uk" to "зараз", "sv" to "nu", "el" to "τώρα",
            "he" to "עכשיו", "bn" to "এখন", "ta" to "இப்போது", "ur" to "ابھی", "sw" to "sasa",
        ),
    )

    // Reverse index built lazily: langCode -> Map<lowercaseForeignPhraseOrWord, canonicalEnglish>
    private val reverseIndex: Map<String, Map<String, String>> by lazy {
        val map = mutableMapOf<String, MutableMap<String, String>>()
        enToLang.forEach { (enKey, translations) ->
            translations.forEach { (langCode, foreign) ->
                val langMap = map.getOrPut(langCode) { mutableMapOf() }
                langMap.putIfAbsent(foreign.lowercase(), enKey)
            }
        }
        map
    }

    private val sortedPhrases: List<String> by lazy {
        enToLang.keys.sortedByDescending { it.length }
    }

    fun translate(text: String, sourceCode: String, targetCode: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || sourceCode == targetCode) return trimmed

        // First convert source -> canonical English if source != "en"
        val englishPivot = if (sourceCode == "en") {
            trimmed
        } else {
            toEnglishPivot(trimmed, sourceCode)
        }

        if (targetCode == "en") return englishPivot

        // Now translate English pivot -> target language
        return fromEnglishPivot(englishPivot, targetCode)
    }

    private fun toEnglishPivot(text: String, sourceCode: String): String {
        val rev = reverseIndex[sourceCode] ?: return text
        val lower = text.lowercase()
        rev[lower]?.let { return matchCase(text, it) }

        var working = lower
        rev.keys.sortedByDescending { it.length }.forEach { foreignPhrase ->
            if (working.contains(foreignPhrase)) {
                val enReplacement = rev[foreignPhrase] ?: return@forEach
                working = working.replace(foreignPhrase, enReplacement)
            }
        }
        return matchCase(text, working)
    }

    private fun fromEnglishPivot(englishText: String, targetCode: String): String {
        val lower = englishText.lowercase()

        // Exact phrase match
        enToLang[lower]?.get(targetCode)?.let { return matchCase(englishText, it) }

        // Multi-phrase and word-by-word replacement preserving punctuation
        var working = englishText
        for (phrase in sortedPhrases) {
            if (!phrase.contains(' ')) continue
            val replacement = enToLang[phrase]?.get(targetCode) ?: continue
            val regex = Regex("\\b${Regex.escape(phrase)}\\b", RegexOption.IGNORE_CASE)
            working = regex.replace(working) { match ->
                matchCase(match.value, replacement)
            }
        }

        val wordRegex = Regex("[A-Za-z']+")
        working = wordRegex.replace(working) { match ->
            val wordLower = match.value.lowercase()
            val translated = enToLang[wordLower]?.get(targetCode)
            if (translated != null) matchCase(match.value, translated) else match.value
        }

        return working
    }

    private fun matchCase(source: String, target: String): String {
        if (source.isEmpty() || target.isEmpty()) return target
        return when {
            source.length > 1 && source.all { !it.isLetter() || it.isUpperCase() } -> target.uppercase()
            source.first().isUpperCase() -> target.replaceFirstChar { it.uppercase() }
            else -> target
        }
    }
}
