package com.psgcreations.mindjournalai.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.draw.scale
import com.psgcreations.mindjournalai.ui.journal.JournalViewModel
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val creamBg = Color(0xFFFFFBF3)
    val cardBg = Color(0xFFFFFFFF)
    val accent = Color(0xFFF7DFA0)

    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }

    // Listener for FLEXIBLE update state changes
    val installStateUpdatedListener = remember {
        InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                // Update downloaded! Show snackbar to restart.
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

    // Register and unregister the listener
    DisposableEffect(appUpdateManager) {
        appUpdateManager.registerListener(installStateUpdatedListener)
        onDispose {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        }
    }

    // Check for update once when the screen is first composed
    LaunchedEffect(activity) {
        if (activity == null) return@LaunchedEffect

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val isUpdateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isFlexibleUpdateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

            if (isUpdateAvailable && isFlexibleUpdateAllowed) {
                // Start the flexible update
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    activity,
                    0 // Request code can be 0 for flexible updates
                )
            }
        }
    }

    Scaffold(
        containerColor = creamBg,

        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "Journal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = creamBg,
                    titleContentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("journal_add") }) {
                Icon(Icons.Default.Add, contentDescription = "New")
            }
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No entries yet", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to create your first note", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(entries, key = { it.id }) { entry ->
                    var pressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(targetValue = if (pressed) 0.995f else 1f, animationSpec = spring(stiffness = 500f))
                    val elevation by animateDpAsState(targetValue = if (pressed) 2.dp else 6.dp, animationSpec = tween(durationMillis = 150))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .shadow(elevation, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        color = cardBg,
                        shape = RoundedCornerShape(12.dp),
                        onClick = { navController.navigate("journal_edit/${entry.id}") },
                        tonalElevation = elevation
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInteropFilter {
                                    when (it.action) {
                                        android.view.MotionEvent.ACTION_DOWN -> {
                                            pressed = true
                                        }
                                        android.view.MotionEvent.ACTION_UP,
                                        android.view.MotionEvent.ACTION_CANCEL -> {
                                            pressed = false
                                        }
                                    }
                                    false
                                }
                        ) {
                            // accent stripe
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .fillMaxHeight()
                                    .background(accent)
                            )

                            Column(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        entry.title.ifBlank { "Untitled" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = entry.content.ifBlank { "—" }.take(160),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.DarkGray
                                )
                                Spacer(Modifier.height(10.dp))
                                val formatted = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                                    .format(Date(entry.timestamp))
                                // timestamp is top-right — but visually anchored bottom-right in card
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(formatted, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
