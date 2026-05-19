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

        val db = ExpenseDatabase.getDatabase(this)

        val accountDao = db.accountDao()
        val expenseDao = db.expenseDao()
        val tripDao = db.tripDao()

        val expenseRepository = ExpenseRepository(accountDao = accountDao, expenseDao = expenseDao)
        val tripRepository = TripRepository(tripDao = tripDao)


        val expenseViewModelFactory = ExpenseViewModelFactory(repository = expenseRepository)
        val tripViewModelFactory = TripViewModelFactory(repository = tripRepository, context = this)

        themePreferences = ThemePreferences(this)

        enableEdgeToEdge()
        setContent {
            val isDarkMode by themePreferences.isDarkModeEnabled.collectAsState(initial = false)
            var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

            ExpenseCalculatorTheme(darkTheme = isDarkMode) {
                if (isLoggedIn) {
                    val expenseViewModel: ExpenseViewModel = viewModel(factory = expenseViewModelFactory)
                    val tripViewModel: TripViewModel = viewModel(factory = tripViewModelFactory)
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        expenseViewModel = expenseViewModel,
                        tripViewModel = tripViewModel,
                        themePreferences = themePreferences
                    )
                } else {
                    AuthNavigator(onLoginSuccess = { isLoggedIn = true })
                }
            }
        }
    }
}