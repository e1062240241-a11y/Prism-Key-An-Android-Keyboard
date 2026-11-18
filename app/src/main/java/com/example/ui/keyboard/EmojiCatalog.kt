package com.example.ui.keyboard

data class EmojiCategory(
    val id: String,
    val label: String,
    val iconEmoji: String,
    val emojis: List<String>,
)

object EmojiCatalog {
    val categories: List<EmojiCategory> = listOf(
        EmojiCategory(
            id = "smileys",
            label = "Smileys",
            iconEmoji = "😊",
            emojis = listOf(
                "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
                "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
                "😘", "😗", "😋", "😛", "😜", "🤪", "😝", "🤗",
                "🤭", "🤫", "🤔", "🫡", "🤐", "🤨", "😐", "😏",
                "😌", "😔", "😪", "😴", "🤯", "🥳", "😎", "🤓",
                "🥹", "😭", "😤", "😡", "😱", "😨", "🤗", "🫶",
            ),
        ),
        EmojiCategory(
            id = "gestures",
            label = "Gestures",
            iconEmoji = "👋",
            emojis = listOf(
                "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏",
                "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉",
                "👆", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛",
                "🤜", "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏",
            ),
        ),
        EmojiCategory(
            id = "hearts",
            label = "Hearts",
            iconEmoji = "💜",
            emojis = listOf(
                "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
                "🤎", "❤️‍🔥", "💖", "💗", "💓", "💞", "💕", "💘",
                "💝", "✨", "🌟", "💫", "⭐", "🔥", "💯", "🎉",
            ),
        ),
        EmojiCategory(
            id = "objects",
            label = "Everyday",
            iconEmoji = "☕",
            emojis = listOf(
                "☕", "🍵", "🍕", "🍔", "🌮", "🍣", "🍰", "🍩",
                "🚀", "✈️", "🚗", "🏠", "📱", "💻", "🎧", "📷",
                "🎨", "🎵", "📚", "📝", "🔒", "🛡️", "🌍", "🌈",
            ),
        ),
    )
}
