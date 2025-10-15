package com.example.mindpal.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mindpal.ui.auth.AuthScreen
import com.example.mindpal.ui.auth.AuthViewModel
import com.example.mindpal.ui.auth.RegisterScreen
import com.example.mindpal.ui.home.HomeScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    context: Context,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = Modifier.padding(innerPadding)
    ) {
        // Login screen
        composable("login") {
            val viewModel: AuthViewModel = hiltViewModel()
            AuthScreen(navController, viewModel, context)
        }

        // Register screen
        composable("register") {
            val viewModel: AuthViewModel = hiltViewModel()
            RegisterScreen(navController, viewModel, context)
        }

        // Home screen
        composable("home") {
            HomeScreen()
        }
    }
}
