package com.psgcreations.mindjournalai.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.psgcreations.mindjournalai.ui.auth.AuthScreen
import com.psgcreations.mindjournalai.ui.auth.AuthViewModel
import com.psgcreations.mindjournalai.ui.auth.RegisterScreen
import com.psgcreations.mindjournalai.ui.home.JournalEntryScreen
import com.psgcreations.mindjournalai.ui.home.JournalListScreen
import com.psgcreations.mindjournalai.ui.journal.JournalViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavGraph(
    navController: NavHostController,
    context: Context,
    innerPadding: PaddingValues
) {
    val firebaseAuth = FirebaseAuth.getInstance()
    val startDestination = if (firebaseAuth.currentUser != null) "journal_list" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable("login") {
            val viewModel: AuthViewModel = hiltViewModel()
            AuthScreen(navController, viewModel, context)
        }

        composable(
            route = "register?email={email}",
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val prefilledEmail = backStackEntry.arguments?.getString("email") ?: ""
            val viewModel: AuthViewModel = hiltViewModel()
            RegisterScreen(navController, viewModel, context, prefilledEmail)
        }

        composable("journal_list") {
            val viewModel = hiltViewModel<JournalViewModel>()
            JournalListScreen(navController, viewModel)
        }

        composable("journal_add") {
            val viewModel = hiltViewModel<JournalViewModel>()
            JournalEntryScreen(navController, viewModel, entryId = null)
        }

        composable(
            route = "journal_edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id")
            val viewModel = hiltViewModel<JournalViewModel>()
            JournalEntryScreen(navController, viewModel, entryId = id)
        }
    }
}

