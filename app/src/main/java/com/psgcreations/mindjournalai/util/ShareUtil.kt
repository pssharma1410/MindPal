package com.psgcreations.mindjournalai.util

import android.content.Context
import android.content.Intent
import com.psgcreations.mindjournalai.room.JournalEntry
import java.text.SimpleDateFormat
import java.util.*

private fun formatTimestamp(timestamp: Long): String {
    return if (timestamp == 0L) ""
    else SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
        .format(Date(timestamp))
}

private fun formatEntry(entry: JournalEntry): String {
    val title = if (entry.title.isBlank()) "Untitled Note" else entry.title
    val content = entry.content.ifBlank { "(No content)" }
    val date = formatTimestamp(entry.createdAt)

    return buildString {
        appendLine("📘 *$title*")
        if (date.isNotEmpty()) appendLine("_${date}_")
        appendLine()
        append(content)
    }
}

fun shareNotes(context: Context, notes: List<JournalEntry>) {
    if (notes.isEmpty()) return

    val combinedText = notes.joinToString(
        separator = "\n\n━━━━━━━━━━━━━━\n\n"
    ) { formatEntry(it) }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, combinedText)
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share Notes")
    context.startActivity(shareIntent)
}

fun shareSingleNote(context: Context, entry: JournalEntry) {
    val formatted = formatEntry(entry)

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, formatted)
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share Note")
    context.startActivity(shareIntent)
}
