package com.psgcreations.mindjournalai.ui.home

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.psgcreations.mindjournalai.ui.journal.JournalViewModel
import com.psgcreations.mindjournalai.util.shareNotes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Delete
import com.psgcreations.mindjournalai.util.Mood // Assuming this import is correct

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun JournalListScreen(
    navController: NavController,
    viewModel: JournalViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val entries by viewModel.entries.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- NOTIFICATION PERMISSION LOGIC (UNCHANGED) ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission granted
            }
        }
    )

    LaunchedEffect(Unit) {
        // 1. Request Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. SYNC TOKEN (Add this line)
        viewModel.checkFcmToken()
    }
    // --- NOTIFICATION PERMISSION LOGIC END ---

    // --- DARK MODE THEME COLORS ---
    val isDark = isSystemInDarkTheme()

    // Background: Cream vs Deep Charcoal
    val screenBg = if (isDark) Color(0xFF121212) else Color(0xFFFFFBF3)

    // Card: White vs Dark Grey Surface (Slightly Lighter for visibility)
    val cardBg = if (isDark) Color(0xFF1F1E24) else Color(0xFFFFFFFF)

    // Text Primary: Black vs Off-White
    val textPrimary = if (isDark) Color(0xFFE6E1E5) else Color.Black

    // Text Secondary: DarkGray vs LightGray
    val textSecondary = if (isDark) Color(0xFFC9C5CA) else Color.DarkGray

    // Date Color: Gray vs Muted Gray
    val textDate = if (isDark) Color(0xFF938F99) else Color.Gray

    // Accent: Keep the same or slightly desaturate for dark mode
    // 🔥 NEW: Using a more soothing accent color for the highlight/mood stripe
    val accent = Color(0xFF80CBC4) // Teal/Minty color

    // --- OCR LOGIC (UNCHANGED) ---
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    val scannedTextState =
        savedStateHandle?.getStateFlow<String?>("ocr_result", null)?.collectAsState()
    val scannedText = scannedTextState?.value

    LaunchedEffect(scannedText) {
        scannedText?.let { text ->
            savedStateHandle?.remove<String>("ocr_result")
            val encodedText = Uri.encode(text)
            navController.navigate("journal_add?content=$encodedText")
        }
    }

    // --- APP UPDATE LOGIC (UNCHANGED) ---
    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }
    val installStateUpdatedListener = remember {
        InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "A new update is ready.",
                        actionLabel = "Restart",
                        duration = SnackbarDuration.Indefinite
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        appUpdateManager.completeUpdate()
                    }
                }
            }
        }
    }

    DisposableEffect(appUpdateManager) {
        appUpdateManager.registerListener(installStateUpdatedListener)
        onDispose {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        }
    }

    LaunchedEffect(activity) {
        if (activity == null) return@LaunchedEffect
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo, AppUpdateType.FLEXIBLE, activity, 0
                )
            }
        }
    }

    // --- DELETE DIALOG (UNCHANGED) ---
    if (showDeleteDialog) {
        val count = viewModel.selectedIds.size
        val itemLabel = if (count == 1) "note" else "notes"

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${if (count == 1) "Note" else "Notes"}?") },
            text = {
                Text("Are you sure you want to delete $count $itemLabel? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val selectedNotes = entries.filter { it.id in viewModel.selectedIds }
                    selectedNotes.forEach { viewModel.deleteEntry(it) }
                    viewModel.clearSelection()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = screenBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Adding SnackbarHost
        topBar = {
            TopAppBar(
                title = {
                    if (viewModel.isSelectionMode) {
                        Text(
                            "${viewModel.selectedIds.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        // 🔥 MODIFIED: A more engaging title with an emoji
                        Text(
                            "Mind Journal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold, // Making the title bolder
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    if (viewModel.isSelectionMode) {
                        // ❌ Cancel Selection
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection", tint = textPrimary)
                        }

                        // 🗑 Delete Selected
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = Color.Red // Use a strong color for delete
                            )
                        }

                        // 📤 Share Selected
                        IconButton(onClick = {
                            val selectedNotes = entries.filter { it.id in viewModel.selectedIds }
                            shareNotes(context, selectedNotes)
                            viewModel.clearSelection()
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = textPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBg,
                    titleContentColor = textPrimary
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // OCR Button (Smaller and secondary)
                SmallFloatingActionButton(
                    onClick = { navController.navigate("ocr_scan") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = "Scan Page")
                }

                // New Entry Button (Primary)
                FloatingActionButton(
                    onClick = { navController.navigate("journal_add") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Entry")
                }
            }
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No entries yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to create your first note", color = textDate)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), // Increased padding
                verticalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(entries, key = { it.id }) { entry ->
                    var pressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "scale") // Increased scale effect
                    val elevation by animateDpAsState(if (pressed) 4.dp else if (isDark) 2.dp else 8.dp, label = "elevation") // Increased shadow effect
                    val isSelected = entry.id in viewModel.selectedIds
                    // 🔥 MODIFIED: Selection color is Material's tertiary for a soft look
                    val bgColor = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f) else cardBg

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .shadow(elevation, RoundedCornerShape(16.dp)) // Rounder corners
                            .clip(RoundedCornerShape(16.dp))
                            .combinedClickable(
                                onClick = {
                                    if (viewModel.isSelectionMode) {
                                        viewModel.toggleSelection(entry.id)
                                    } else {
                                        navController.navigate("journal_edit/${entry.id}")
                                    }
                                },
                                onLongClick = {
                                    Log.d("viewModel123","long pressed")
                                    viewModel.startSelection(entry.id)
                                }
                            ),
                        color = bgColor,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = elevation
                    ) {
                        // 🔥 NEW: Main Content Row - Calendar Icon on Left, Details on Right
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInteropFilter {
                                    when (it.action) {
                                        android.view.MotionEvent.ACTION_DOWN -> pressed = true
                                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> pressed = false
                                    }
                                    false
                                },
                            verticalAlignment = Alignment.CenterVertically // Center items vertically
                        ) {
                            // 1. Calendar/Date Icon (on the very left)
                            Box(modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp)) {
                                CalendarTypeIcon(entry.createdAt)
                            }

                            // 2. Journal Details Column (Title, Mood, Content, Date)
                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .weight(1f)
                            ) {
                                // 🔥 MODIFIED: Title and Mood in one primary row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Mood Emoji (Bigger, more prominent)
                                    Text(
                                        text = entry.mood,
                                        fontSize = 24.sp,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    // Title
                                    Text(
                                        text = entry.title.ifBlank { "Untitled" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), // Bolder Title
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = textPrimary
                                    )
                                }

                                Spacer(Modifier.height(4.dp)) // Reduced spacing

                                // Content Preview
                                Text(
                                    entry.content.ifBlank { "—" }.take(100), // Shorter preview
                                    style = MaterialTheme.typography.bodySmall, // Smaller body font
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = textSecondary
                                )
                                Spacer(Modifier.height(8.dp))

                                // Date/Edited Status (Aligned to the start for better flow)
                                val formattedDate = SimpleDateFormat(
                                    "MMM dd, yyyy • hh:mm a",
                                    Locale.getDefault()
                                ).format(Date(entry.createdAt))
                                val isEdited = entry.createdAt != entry.updatedAt

                                Text(
                                    text = if (isEdited) {
                                        val updatedFormatted = SimpleDateFormat(
                                            "MMM dd, yyyy • hh:mm a",
                                            Locale.getDefault()
                                        ).format(Date(entry.updatedAt))
                                        "Edited: $updatedFormatted" // Show "Edited" status
                                    } else {
                                        formattedDate
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isEdited) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else textDate // Highlight edited text
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

// 🔥 MODIFIED: CalendarTypeIcon is now visually tighter and more distinct.
@Composable
fun CalendarTypeIcon(date: Long) {
    val cal = Calendar.getInstance().apply { timeInMillis = date }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    // Use 'EEE' for short day name (e.g., 'Mon') or 'MMM' for month if preferred
    // I'm changing it to Day of Week to save space and add info.
    val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time).uppercase()

    Column(
        modifier = Modifier
            .width(50.dp)
            .height(60.dp) // Fixed height for uniformity
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant), // Use a surface variant color
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween // Space things out
    ) {

        // 🔥 TOP BAR (Day of Week)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp) // Taller bar
                .clip(
                    RoundedCornerShape(
                        topStart = 10.dp,
                        topEnd = 10.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
                .background(MaterialTheme.colorScheme.primary), // Accent color for the top
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayOfWeek,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 0.5.sp
                )
            )
        }

        // 🔥 DAY (big text) - Center
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(top = 2.dp)
        )

        // 🔥 MONTH (Removed to keep it cleaner and save space, date is already in card)
        // Leaving a small padding at the bottom instead
        Spacer(Modifier.height(4.dp))
    }
}