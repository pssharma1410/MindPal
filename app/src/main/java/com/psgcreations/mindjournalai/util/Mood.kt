package com.psgcreations.mindjournalai.util

enum class Mood(val emoji: String, val description: String) {
    NEUTRAL("😐", "Neutral"),
    HAPPY("😊", "Happy"),
    CALM("😌", "Calm"),
    ANGRY("😠", "Angry"),
    SAD("😔", "Sad"),
    EXCITED("🤩", "Excited");

    companion object {
        fun getByEmoji(emoji: String): Mood? = values().find { it.emoji == emoji }
    }
}