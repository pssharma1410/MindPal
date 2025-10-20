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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalListScreen(
    navController: NavController,
    viewModel: JournalViewModel
) {
    val entries by viewModel.entries.collectAsState()
    val creamBg = Color(0xFFFFFBF3)
    val cardBg = Color(0xFFFFFFFF)
    val accent = Color(0xFFF7DFA0)

    Scaffold(
        containerColor = creamBg,
        topBar = {
            TopAppBar(
                title = { Text("Journal", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = creamBg),
                modifier = Modifier.statusBarsPadding()
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
                    .padding(padding),
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
