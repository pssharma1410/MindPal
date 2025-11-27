package com.psgcreations.mindjournalai.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.psgcreations.mindjournalai.room.JournalEntry
import com.psgcreations.mindjournalai.ui.journal.JournalViewModel
import com.psgcreations.mindjournalai.util.Mood
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryScreen(
    navController: NavController,
    viewModel: JournalViewModel,
    entryId: Int? = null,
    initialContent: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var currentMood by remember { mutableStateOf(Mood.NEUTRAL.emoji) }

    var existingEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var isLoading by remember { mutableStateOf(entryId != null) }

    val isNewEntry = entryId == null
    val contentFocusRequester = remember { FocusRequester() }

    // Load entry logic (UNCHANGED)
    LaunchedEffect(entryId) {
        if (!isNewEntry && entryId != null) {
            val entry = viewModel.getEntryById(entryId)
            existingEntry = entry
            entry?.let {
                title = it.title
                content = it.content
                currentMood = it.mood
            }
            isLoading = false
        }

        initialContent?.let {
            content = URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            coroutineScope.launch {
                contentFocusRequester.requestFocus()
                // Scroll to content input field if initial content is present
                // Removed listState.scrollToItem(2) as it might break with new items
            }
        }

        if (entryId == null && initialContent == null) {
            isLoading = false
        }
    }

    // Save entry logic (UNCHANGED)
    val saveEntry: () -> Unit = {
        coroutineScope.launch {
            if (title.isBlank() && content.isBlank() && isNewEntry) {
                navController.popBackStack()
                return@launch
            }

            if (isNewEntry) {
                viewModel.addEntry(title.trim(), content.trim(), currentMood)
            } else {
                existingEntry?.let { entry ->
                    if (entry.title != title.trim() ||
                        entry.content != content.trim() ||
                        entry.mood != currentMood
                    ) {
                        viewModel.updateEntry(
                            entry.copy(
                                title = title.trim(),
                                content = content.trim(),
                                mood = currentMood,
                            )
                        )
                    }
                }
            }
            navController.popBackStack()
        }
    }

    // Share entry logic (UNCHANGED)
    val shareEntry: () -> Unit = {
        val entryText = "${title.ifBlank { "Untitled Note" }}\n\n$content"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, entryText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Note via"))
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isNewEntry) "New Entry" else "Edit Entry") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = !isNewEntry && existingEntry != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(onClick = shareEntry) {
                            Icon(Icons.Default.Share, contentDescription = "Share Entry")
                        }
                    }

                    IconButton(onClick = saveEntry) {
                        Icon(Icons.Default.Done, contentDescription = "Save Entry")
                    }
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), // Apply padding here
            state = listState
        ) {

            // 🔥 NEW: Metadata Section (Created At / Updated At)
            if (!isNewEntry && existingEntry != null) {
                item {
                    EntryMetadata(existingEntry!!)
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    MoodPicker(
                        selectedMood = currentMood,
                        onMoodSelected = { currentMood = it }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                TitleInputField(
                    value = title,
                    onValueChange = { title = it }
                )
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // ⭐ WORD COUNT TOP RIGHT (UNCHANGED)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${content.length} Characters",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                ContentInputField(
                    value = content,
                    onValueChange = { content = it },
                    focusRequester = contentFocusRequester
                )
            }
            item {
                // Ensure space below content when keyboard is active
                Spacer(
                    modifier = Modifier
                        .fillParentMaxHeight(0.5f)
                        .imePadding()
                )
            }

        }

    }
}

// 🔥 NEW: Composable to display Created and Edited times
@Composable
fun EntryMetadata(entry: JournalEntry) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val createdDate = remember { formatter.format(Date(entry.createdAt)) }
    val isEdited = entry.createdAt != entry.updatedAt

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Created At
        Text(
            text = "Created: $createdDate",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        // Updated At (only show if different)
        if (isEdited) {
            val updatedDate = remember { formatter.format(Date(entry.updatedAt)) }
            Text(
                text = "Edited: $updatedDate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary // Highlight edited time
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun TitleInputField(
    value: String,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                "Untitled Note",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun ContentInputField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("What's on your mind today? Start writing...") },
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 400.dp)
            .focusRequester(focusRequester)
            .padding(horizontal = 16.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun MoodPicker(
    selectedMood: String,
    onMoodSelected: (String) -> Unit
) {
    val allMoods = remember { Mood.entries.toList() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "How are you feeling?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(allMoods) { mood ->
                val isSelected = mood.emoji == selectedMood
                MoodItem(
                    mood = mood,
                    isSelected = isSelected,
                    onClick = { onMoodSelected(mood.emoji) }
                )
            }
        }
    }
}

@Composable
fun MoodItem(
    mood: Mood,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh

    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(bgColor, CircleShape)
                .wrapContentSize(Alignment.Center)
        ) {
            Text(
                text = mood.emoji,
                fontSize = 28.sp,
                color = Color.Black
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = mood.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}