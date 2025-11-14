package com.psgcreations.mindjournalai.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.psgcreations.mindjournalai.ui.journal.JournalViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun JournalEntryScreen(
    navController: NavController,
    viewModel: JournalViewModel,
    entryId: Int? = null,
    initialContent: String? = null
) {
    // --- THEME COLORS ---
    val isDark = isSystemInDarkTheme()

    // Background: Light Cream vs. Deep Warm Charcoal
    val screenBg = if (isDark) Color(0xFF121212) else Color(0xFFFFFBF3)

    // Paper: White (pops on cream) vs. Lighter Dark Grey (pops on black)
    val paperBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    // Text: Black vs. Off-White
    val textColor = if (isDark) Color(0xFFE1E1E1) else Color(0xFF1D1B20)

    // Icons: Dark Grey vs. Light Grey
    val iconColor = if (isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)

    val coroutineScope = rememberCoroutineScope()
    val entries by viewModel.entries.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val isNewEntry = entryId == null

    val initialPageIndex = remember(entries, entryId) {
        if (isNewEntry) 0 else entries.indexOfFirst { it.id == entryId }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { if (isNewEntry) 1 else entries.size }
    )

    var title by remember { mutableStateOf(TextFieldValue("")) }
    var content by remember { mutableStateOf(TextFieldValue(initialContent ?: "")) }
    var currentVisibleEntryId by remember { mutableStateOf<Int?>(entryId) }

    LaunchedEffect(pagerState.currentPage, entries, isNewEntry) {
        if (isNewEntry) {
            title = TextFieldValue("")
            content = TextFieldValue(initialContent ?: "")
            currentVisibleEntryId = null
        } else if (entries.isNotEmpty()) {
            val index = pagerState.currentPage
            if (index in entries.indices) {
                val entry = entries[index]
                title = TextFieldValue(entry.title)
                content = TextFieldValue(entry.content)
                currentVisibleEntryId = entry.id
            }
        }
    }

    Scaffold(
        containerColor = screenBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(if (isNewEntry) "New Entry" else "Entry") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBg,
                    titleContentColor = textColor,
                    navigationIconContentColor = iconColor,
                    actionIconContentColor = iconColor
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNewEntry && entries.isNotEmpty()) {
                        IconButton(onClick = {
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
                .background(screenBg)
        ) {
            if (isNewEntry) {
                PageSurface(
                    modifier = Modifier.fillMaxSize(),
                    title = title.text,
                    content = content.text,
                    showRuled = true,
                    editable = true,
                    titleValue = title,
                    contentValue = content,
                    onTitleChange = { title = it },
                    onContentChange = { content = it },
                    backgroundColor = paperBg,
                    textColor = textColor,
                    isDark = isDark
                )
            } else if (entries.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val entry = entries[pageIndex]
                    val isEditable = (pageIndex == pagerState.currentPage)

                    PageSurface(
                        modifier = Modifier.fillMaxSize(),
                        title = if (isEditable) title.text else entry.title,
                        content = if (isEditable) content.text else entry.content,
                        showRuled = true,
                        editable = isEditable,
                        titleValue = title,
                        contentValue = content,
                        onTitleChange = { title = it },
                        onContentChange = { content = it },
                        backgroundColor = paperBg,
                        textColor = textColor,
                        isDark = isDark
                    )
                }
            }

            // Save Button Logic
            val density = LocalDensity.current
            val imePx = WindowInsets.ime.getBottom(density)
            val imeDp = with(density) { imePx.toDp() }
            val basePadding = if (imeDp == 0.dp) 18.dp else 0.dp

            FloatingActionButton(
                onClick = {
                    val t = title.text.trim()
                    val c = content.text.trim()
                    coroutineScope.launch {
                        if (isNewEntry) {
                            viewModel.addEntry(t, c)
                        } else {
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
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = basePadding + imeDp, end = 18.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save")
            }
        }
    }
}

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
    onContentChange: (TextFieldValue) -> Unit = {},
    backgroundColor: Color,
    textColor: Color,
    isDark: Boolean
) {
    // Ruled Lines: Subtle Beige in Light Mode / Subtle Grey in Dark Mode
    val lineColor = if (isDark) Color(0xFF333333) else Color(0xFFEDE7DC)
    val placeholderColor = if (isDark) Color(0xFF777777) else Color.Gray

    // Border: Only visible in Dark Mode to separate card from background
    val borderStroke = if (isDark) BorderStroke(1.dp, Color(0xFF333333)) else null

    Box(
        modifier = modifier
            .padding(18.dp)
            .shadow(
                elevation = if (isDark) 0.dp else 2.dp, // Shadows look bad in dark mode, remove them
                shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
            .then(
                if (borderStroke != null) Modifier.border(borderStroke, MaterialTheme.shapes.medium) else Modifier
            )
    ) {
        val lineHeightSp = 28.sp

        // Draw Ruled Lines
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

        Column(modifier = Modifier.padding(18.dp).fillMaxSize()) {
            if (editable) {
                // Title Field
                OutlinedTextField(
                    value = titleValue,
                    onValueChange = { onTitleChange(it.copy()) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Title...", color = placeholderColor) },
                    singleLine = false,
                    textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = textColor),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        cursorColor = textColor,
                        selectionColors = TextSelectionColors(
                            handleColor = textColor,
                            backgroundColor = textColor.copy(alpha = 0.3f)
                        )
                    )
                )

                Spacer(Modifier.height(8.dp))

                // Content Field
                OutlinedTextField(
                    value = contentValue,
                    onValueChange = { onContentChange(it.copy()) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    placeholder = { Text("Start writing here...", color = placeholderColor) },
                    textStyle = TextStyle(fontSize = 16.sp, color = textColor),
                    maxLines = Int.MAX_VALUE,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        cursorColor = textColor,
                        selectionColors = TextSelectionColors(
                            handleColor = textColor,
                            backgroundColor = textColor.copy(alpha = 0.3f)
                        )
                    )
                )
            } else {
                // Read-Only View
                Text(
                    text = if (title.isBlank()) "Untitled" else title,
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (content.isBlank()) "Start writing here..." else content,
                    style = TextStyle(fontSize = 16.sp, color = textColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}