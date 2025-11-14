package com.psgcreations.mindjournalai.ui.home

// ADDED: Pager imports
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
// ---

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import com.psgcreations.mindjournalai.ui.journal.JournalViewModel
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction

// REMOVED: All gesture, animation, and transform imports
// import androidx.compose.animation.core.Animatable
// import androidx.compose.animation.core.spring
// import androidx.compose.foundation.gestures.detectHorizontalDragGestures
// import androidx.compose.ui.graphics.graphicsLayer
// import androidx.compose.ui.graphics.TransformOrigin
// import kotlin.math.absoluteValue
// import kotlinx.coroutines.Job

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class) // ADDED ExperimentalFoundationApi
@Composable
fun JournalEntryScreen(
    navController: NavController,
    viewModel: JournalViewModel,
    entryId: Int? = null
) {
    val creamBg = Color(0xFFFFFBF3)
    val coroutineScope = rememberCoroutineScope()
    val entries by viewModel.entries.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // --- REFACTORED: Pager and State Logic ---

    // True if we are creating a new entry, false if editing existing ones
    val isNewEntry = entryId == null

    // Find the initial page index. If new, default to 0. If editing, find the index of the entryId.
    val initialPageIndex = remember(entries, entryId) {
        if (isNewEntry) 0 else entries.indexOfFirst { it.id == entryId }.coerceAtLeast(0)
    }

    // Standard PagerState. It will be controlled by the HorizontalPager.
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { if (isNewEntry) 1 else entries.size } // Page count is 1 for new, or all entries
    )

    // Local state for the text fields. This will be updated when the page changes.
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var content by remember { mutableStateOf(TextFieldValue("")) }

    // This is the ID of the entry currently being viewed, used for saving/deleting.
    var currentVisibleEntryId by remember { mutableStateOf<Int?>(entryId) }

    // This effect updates the text fields when the user swipes to a new page
    // or when the initial data is loaded.
    LaunchedEffect(pagerState.currentPage, entries, isNewEntry) {
        if (isNewEntry) {
            // "New Entry" mode: clear fields
            title = TextFieldValue("")
            content = TextFieldValue("")
            currentVisibleEntryId = null
        } else if (entries.isNotEmpty()) {
            // "Edit" mode: load data for the currently visible page
            val index = pagerState.currentPage
            if (index in entries.indices) {
                val entry = entries[index]
                title = TextFieldValue(entry.title)
                content = TextFieldValue(entry.content)
                currentVisibleEntryId = entry.id
            }
        }
    }

    // REMOVED: All custom animation functions (goToIndex, goNext, goPrev)
    // REMOVED: All animation state (isAnimating, directionForward, animationProgress, dragAccum, dragJob)

    // --- End REFACTORED ---

    Scaffold(
        containerColor = creamBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(if (isNewEntry) "New Entry" else "Entry") },

                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // CHANGED: Delete button only shows in "Edit" mode
                    if (!isNewEntry && entries.isNotEmpty()) {
                        IconButton(onClick = {
                            // Delete the entry currently visible in the pager
                            val toDelete = entries[pagerState.currentPage]
                            coroutineScope.launch {
                                viewModel.deleteEntry(toDelete)
                            }
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .background(creamBg)
        ) {

            if (isNewEntry) {
                PageSurface(
                    modifier = Modifier.fillMaxSize(),
                    title = title.text,
                    content = content.text,
                    showRuled = true,
                    editable = true, // Always editable
                    titleValue = title,
                    contentValue = content,
                    onTitleChange = { title = it },
                    onContentChange = { content = it }
                )
            } else if (entries.isNotEmpty()) {
                // --- Case 2: EDITING EXISTING ---
                // Show a Pager with all entries.
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->

                    val entry = entries[pageIndex]
                    // The page is only editable if it's the one currently settled by the pager.
                    val isEditable = (pageIndex == pagerState.currentPage)

                    PageSurface(
                        modifier = Modifier.fillMaxSize(),
                        // For non-editable pages, show the raw entry data
                        // For the editable page, show the text field state
                        title = if (isEditable) title.text else entry.title,
                        content = if (isEditable) content.text else entry.content,
                        showRuled = true,
                        editable = isEditable,
                        titleValue = title,
                        contentValue = content,
                        onTitleChange = { title = it },
                        onContentChange = { content = it }
                    )
                }
            }
            // REMOVED: All the PageSurface composables used for animation
            // REMOVED: Prev/Next FloatingActionButtons

            // Save button logic is now simpler

            val density = LocalDensity.current

            val imePx = WindowInsets.ime.getBottom(density)

            val imeDp = with(density) { imePx.toDp() }

            val basePadding = if(imeDp == 0.dp) 18.dp else 0.dp
            FloatingActionButton(
                onClick = {
                    val t = title.text.trim()
                    val c = content.text.trim()
                    coroutineScope.launch {
                        if (isNewEntry) {
                            // Save as new
                            viewModel.addEntry(t, c)
                        } else {
                            // Update existing. We use the ID we've been tracking.
                            val entryToUpdate = entries.find { it.id == currentVisibleEntryId }
                            entryToUpdate?.let {
                                viewModel.updateEntry(
                                    it.copy(
                                        title = t,
                                        content = c,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    navController.popBackStack()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = basePadding+imeDp,
                        end = 18.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save")
            }
        }
        // --- End REFACTORED ---
    }
}

/**
 * PageSurface composable remains unchanged.
 * It's perfect for this use case because its `editable` flag
 * correctly switches between TextField and Text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageSurface(
    modifier: Modifier = Modifier,
    title: String,
    content: String,
    showRuled: Boolean = true,
    editable: Boolean = false,
    titleValue: TextFieldValue = TextFieldValue(""),
    contentValue: TextFieldValue = TextFieldValue(""),
    onTitleChange: (TextFieldValue) -> Unit = {},
    onContentChange: (TextFieldValue) -> Unit = {}
) {
    val creamBg = Color(0xFFFFFBF3)
    Box(
        modifier = modifier
            .padding(18.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(creamBg)
            .shadow(2.dp, MaterialTheme.shapes.medium)
    ) {
        // ruled background
        val lineHeightSp = 28.sp
        val lineColor = Color(0xFFEDE7DC)
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    if (showRuled) {
                        val lineHeightPx = lineHeightSp.toPx()
                        var y = lineHeightPx + 24.dp.toPx()
                        val stroke = 1.dp.toPx()
                        while (y < size.height) {
                            drawLine(
                                color = lineColor,
                                start = Offset(x = 0f, y = y),
                                end = Offset(x = size.width, y = y),
                                strokeWidth = stroke
                            )
                            y += lineHeightPx
                        }
                    }
                }
        )
        Column(modifier = Modifier
            .padding(18.dp)
            .fillMaxSize()
        ) {
            if (editable) {
                OutlinedTextField(
                    value = titleValue,
                    onValueChange = {onTitleChange(it.copy()) },
                    modifier = Modifier
                        .fillMaxWidth(),
                    placeholder = { Text(text = "Title...") },
                    singleLine = false,
                    textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = contentValue,
                    onValueChange = {onContentChange(it.copy()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text(text = "Start writing here...") },
                    textStyle = TextStyle(fontSize = 16.sp),
                    maxLines = Int.MAX_VALUE,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                )
            } else {
                // Non-editable preview (used for animated incoming/outgoing pages)
                Text(
                    text = if (title.isBlank()) "Untitled" else title,
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (content.isBlank()) "Start writing here..." else content,
                    style = TextStyle(fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}