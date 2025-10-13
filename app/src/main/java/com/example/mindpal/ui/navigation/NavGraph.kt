package com.example.mindpal.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mindpal.ui.auth.AuthViewModel
import com.example.mindpal.ui.auth.GoogleSignInScreen
import com.example.mindpal.ui.home.HomeScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    context: Context,
    innerPadding: androidx.compose.foundation.layout.PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = Modifier.padding(innerPadding)
    ) {
        composable("login") {
            val viewModel: AuthViewModel = hiltViewModel()
            GoogleSignInScreen(navController, viewModel, context)
        }

        composable("home") {
            HomeScreen()
        }
    }
}
