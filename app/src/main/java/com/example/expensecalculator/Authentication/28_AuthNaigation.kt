package com.example.expensecalculator.Authentication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AuthNavigator(onLoginSuccess: () -> Unit) { // Accept the callback
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    LaunchedEffect(Unit) {
        if(authViewModel.isLoggedIn)onLoginSuccess()
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            // Pass the callback down to the LoginScreen
            LoginScreen(
                navController = navController,
                viewModel = authViewModel,
                onLoginSuccess = onLoginSuccess
            )
        }
        composable("register") {
            RegisterScreen(navController = navController, viewModel = authViewModel)
        }
    }
}
