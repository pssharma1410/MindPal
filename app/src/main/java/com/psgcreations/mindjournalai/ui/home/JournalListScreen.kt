package com.psgcreations.mindjournalai.ui.home

import android.app.Activity
import android.net.Uri
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
import androidx.compose.material.icons.filled.DocumentScanner
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
import androidx.navigation.NavController
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.psgcreations.mindjournalai.ui.journal.JournalViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    // --- DARK MODE THEME COLORS ---
    val isDark = isSystemInDarkTheme()

    // Background: Cream vs Deep Charcoal
    val screenBg = if (isDark) Color(0xFF121212) else Color(0xFFFFFBF3)

    // Card: White vs Dark Grey Surface
    val cardBg = if (isDark) Color(0xFF1C1B1F) else Color(0xFFFFFFFF)

    // Text Primary: Black vs Off-White
    val textPrimary = if (isDark) Color(0xFFE6E1E5) else Color.Black

    // Text Secondary: DarkGray vs LightGray
    val textSecondary = if (isDark) Color(0xFFC9C5CA) else Color.DarkGray

    // Date Color: Gray vs Muted Gray
    val textDate = if (isDark) Color(0xFF938F99) else Color.Gray

    // Accent: Keep the same or slightly desaturate for dark mode
    val accent = Color(0xFFF7DFA0)

    // --- OCR LOGIC (UNCHANGED) ---
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    val scannedTextState = savedStateHandle?.getStateFlow<String?>("ocr_result", null)?.collectAsState()
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

    Scaffold(
        containerColor = screenBg, // Dynamic Background
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        "Journal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBg, // Dynamic TopBar
                    titleContentColor = textPrimary // Dynamic Title
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { navController.navigate("ocr_scan") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = "Scan Page")
                }

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
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No entries yet", style = MaterialTheme.typography.titleMedium, color = textSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to create your first note", color = textDate)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(entries, key = { it.id }) { entry ->
                    var pressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (pressed) 0.995f else 1f)
                    // Reduce elevation in Dark Mode as shadows are invisible
                    val elevation by animateDpAsState(if (pressed) 2.dp else if (isDark) 0.dp else 6.dp)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .shadow(elevation, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        color = cardBg, // Dynamic Card Color
                        shape = RoundedCornerShape(12.dp),
                        onClick = { navController.navigate("journal_edit/${entry.id}") },
                        tonalElevation = elevation
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInteropFilter {
                                    when (it.action) {
                                        android.view.MotionEvent.ACTION_DOWN -> pressed = true
                                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> pressed = false
                                    }
                                    false
                                }
                        ) {
                            Box(modifier = Modifier.width(8.dp).fillMaxHeight().background(accent))
                            Column(modifier = Modifier.padding(14.dp).weight(1f)) {
                                Text(
                                    entry.title.ifBlank { "Untitled" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = textPrimary // Dynamic Text
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    entry.content.ifBlank { "—" }.take(160),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    color = textSecondary // Dynamic Text
                                )
                                Spacer(Modifier.height(10.dp))
                                val formatted = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(entry.timestamp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Text(formatted, style = MaterialTheme.typography.labelSmall, color = textDate)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}