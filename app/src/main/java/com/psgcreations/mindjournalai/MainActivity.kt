package com.psgcreations.mindjournalai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.psgcreations.mindjournalai.ui.navigation.AppNavGraph
import com.psgcreations.mindjournalai.ui.theme.MindPalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 1. Declare NavController here so we can access it from onNewIntent
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MindPalTheme {
                // 2. Initialize it inside setContent
                navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    AppNavGraph(
                        navController = navController,
                        context = this,
                        innerPadding = innerPadding
                    )

                    // 3. Handle "Cold Start" (App was killed)
                    LaunchedEffect(Unit) {
                        handleNotificationNavigation(intent)
                    }
                }
            }
        }
    }

    // 4. Handle "Warm Start" (App was in background)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update the activity's intent to the new one
        setIntent(intent)
        // Trigger navigation manually
        handleNotificationNavigation(intent)
    }

    // 5. Shared Logic to Navigate
    private fun handleNotificationNavigation(intent: Intent?) {
        val destination = intent?.getStringExtra("screen")

        if (destination == "journal_add") {
            val user = FirebaseAuth.getInstance().currentUser
            // Only navigate if user is logged in
            if (user != null) {
                // Check if navController is initialized to avoid crashes
                if (::navController.isInitialized) {
                    navController.navigate("journal_add")
                    // Remove extra so it doesn't happen again on rotation
                    intent.removeExtra("screen")
                }
            }
        }
    }
}