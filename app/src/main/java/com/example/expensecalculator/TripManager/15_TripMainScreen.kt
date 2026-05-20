package com.example.expensecalculator.TripManager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.expensecalculator.ThemePreferences
import com.example.expensecalculator.tripData.Trip
import com.example.expensecalculator.ui.theme.IconBackground
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripMainScreen(
    navController: NavController,
    viewModel: TripViewModel,
    themePreferences: ThemePreferences
) {
    val trips by viewModel.allTrips.collectAsState(initial = emptyList())
    val isDarkMode by themePreferences.isDarkModeEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "All Trips",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                "More Options",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isDarkMode) "Light Mode" else "Dark Mode") },
                                onClick = {
                                    scope.launch {
                                        themePreferences.setDarkMode(!isDarkMode)
                                    }
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        "Toggle Theme",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = {
                                    scope.launch {
                                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                        // restart activity
                                        (context as android.app.Activity).recreate()
                                    }
                                    menuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_trip") },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Trip",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        // Dotted background pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = Color(0x22FFFFFF)
            val dotRadius = 1.dp.toPx()
            val spacing = 20.dp.toPx()
            for (x in 0 until size.width.toInt() step spacing.toInt()) {
                for (y in 0 until size.height.toInt() step spacing.toInt()) {
                    drawCircle(dotColor, radius = dotRadius, center = Offset(x.toFloat(), y.toFloat()))
                }
            }
        }

        if (trips.isEmpty()) {
            // 4. Updated Empty State with light text for dark background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "No Trips Yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap the '+' button to add your first trip.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                items(trips) { trip ->
                    SimpleTripItem(
                        trip = trip,
                        onClick = { navController.navigate("trip_detail/${trip.id}") }
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleTripItem(
    trip: Trip,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(IconBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = trip.title.firstOrNull()?.toString()?.uppercase(Locale.getDefault()) ?: "T",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "View Trip",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}