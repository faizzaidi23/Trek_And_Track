package com.faiz.trekandtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.expensecalculator.Authentication.AuthNavigator
import com.example.expensecalculator.Data.ExpenseDatabase
import com.example.expensecalculator.ExpenseRepository
import com.example.expensecalculator.ExpenseViewModel
import com.example.expensecalculator.ExpenseViewModelFactory
import com.example.expensecalculator.NavGraph
import com.example.expensecalculator.ThemePreferences
import com.example.expensecalculator.TripManager.TripRepository
import com.example.expensecalculator.TripManager.TripViewModel
import com.example.expensecalculator.TripManager.TripViewModelFactory
import com.example.expensecalculator.ui.theme.ExpenseCalculatorTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePreferences = ThemePreferences(this)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by themePreferences.isDarkModeEnabled.collectAsState(initial = false)
            var currentUserId by remember {
                mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid)
            }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    val newUid = auth.currentUser?.uid
                    if (newUid != currentUserId) {
                        ExpenseDatabase.clearInstance()
                        currentUserId = newUid
                    }
                }
                FirebaseAuth.getInstance().addAuthStateListener(listener)
                onDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
            }

            ExpenseCalculatorTheme(darkTheme = isDarkMode) {
                if (currentUserId != null) {
                    val db = remember(currentUserId) {
                        ExpenseDatabase.getDatabase(this@MainActivity, currentUserId!!)
                    }
                    val expenseViewModelFactory = remember(currentUserId) {
                        ExpenseViewModelFactory(ExpenseRepository(db.accountDao(), db.expenseDao()))
                    }
                    val tripViewModelFactory = remember(currentUserId) {
                        TripViewModelFactory(TripRepository(db.tripDao()), this@MainActivity)
                    }
                    val expenseViewModel: ExpenseViewModel = viewModel(
                        factory = expenseViewModelFactory,
                        key = currentUserId
                    )
                    val tripViewModel: TripViewModel = viewModel(
                        factory = tripViewModelFactory,
                        key = currentUserId
                    )
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        expenseViewModel = expenseViewModel,
                        tripViewModel = tripViewModel,
                        themePreferences = themePreferences
                    )
                } else {
                    AuthNavigator(onLoginSuccess = {
                        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    })
                }
            }
        }
    }
}