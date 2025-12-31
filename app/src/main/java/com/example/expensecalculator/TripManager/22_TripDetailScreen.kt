package com.example.expensecalculator.TripManager

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.expensecalculator.tripData.TripExpense
import com.example.expensecalculator.tripData.TripParticipant
import com.example.expensecalculator.tripData.TripPhoto
import com.example.expensecalculator.ui.theme.IconBackground
import com.example.expensecalculator.ui.theme.PositiveBalanceColor
import com.example.expensecalculator.ui.theme.NegativeBalanceColor
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    navController: NavController,
    viewModel: TripViewModel,
    tripId: Int
) {
    val context = LocalContext.current
    val completeTripDetails by viewModel.completeTripDetails.collectAsState()
    val currentTripParticipants by viewModel.currentTripParticipants.collectAsState()
    val tripBalances by viewModel.tripBalances.collectAsState()
    val adjustedBalances by viewModel.adjustedBalances.collectAsState()
    val currentTripPhotos by viewModel.currentTripPhotos.collectAsState()
    val optimizedSettlements by viewModel.optimizedSettlements.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportResultUri by remember { mutableStateOf<Uri?>(null) }
    var showExportResult by remember { mutableStateOf(false) }
    var showDeleteTripDialog by remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistent URI permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission already granted or not available
            }
            viewModel.addPhoto(it.toString())
        }
    }

    // Trip icon picker launcher
    val tripIconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistent URI permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission already granted or not available
            }
            viewModel.updateTripIcon(tripId, it.toString())
        }
    }

    // Export dialog
    if (showExportDialog) {
        TripExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                viewModel.exportTrip(format) { uri ->
                    exportResultUri = uri
                    showExportResult = true
                }
            }
        )
    }

    // Show export result
    if (showExportResult) {
        HandleExportResult(exportResultUri)
        showExportResult = false
    }

    LaunchedEffect(tripId) {
        viewModel.setCurrentTrip(tripId)
        viewModel.loadCategoriesWithExpenses(tripId)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearCurrentTrip() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* No title */ },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Go back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle Search */ }) {
                        Icon(
                            Icons.Default.Search,
                            "Search",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
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
                                text = { Text("Export") },
                                onClick = {
                                    menuExpanded = false
                                    showExportDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.FileDownload, "Export Icon") }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    menuExpanded = false
                                    navController.navigate("add_trip?tripId=$tripId")
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, "Edit Icon") }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteTripDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Delete Icon",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trip Header
                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { tripIconPickerLauncher.launch("image/*") }
                        .background(IconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    val tripIconUri = completeTripDetails?.trip?.tripIconUri
                    if (tripIconUri != null) {
                        AsyncImage(
                            model = tripIconUri.toUri(),
                            contentDescription = "Trip Icon",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = rememberVectorPainter(Icons.Default.Umbrella),
                            contentDescription = "Default Trip Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(13.dp))
                Text(
                    text = completeTripDetails?.trip?.title ?: "Loading...",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tap icon to change",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.background,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            height = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Expenses", fontSize = 15.sp) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Categories",fontSize=13.sp) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Balances", fontSize = 15.sp) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Tab(
                        selected = selectedTabIndex == 3,
                        onClick = { selectedTabIndex = 3 },
                        text = { Text("Photos",fontSize=15.sp) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Tab Content
                when (selectedTabIndex) {
                    0 -> {
                        val currencySymbol = completeTripDetails?.trip?.currency?.let {
                            com.example.expensecalculator.util.CurrencyCode.fromCode(it).symbol
                        } ?: "₹"
                        ExpensesContent(
                            expensesWithSplits = completeTripDetails?.expensesWithSplits ?: emptyList(),
                            currencySymbol = currencySymbol,
                            onDeleteExpense = { viewModel.deleteExpense(it) },
                            onExpenseClick = { expenseId ->
                                navController.navigate("trip_expense_detail/$expenseId")
                            }
                        )
                    }
                    1 -> {
                        val currencySymbol = completeTripDetails?.trip?.currency?.let {
                            com.example.expensecalculator.util.CurrencyCode.fromCode(it).symbol
                        } ?: "₹"
                        val categoriesWithExpenses by viewModel.categoriesWithExpenses.collectAsState()

                        CategoriesContent(
                            categoriesWithExpenses = categoriesWithExpenses,
                            currencySymbol = currencySymbol,
                            onAddCategory = { categoryName -> viewModel.addCategory(categoryName) },
                            onDeleteCategory = { category -> viewModel.deleteCategory(category) },
                            onExpenseClick = { expenseId ->
                                navController.navigate("trip_expense_detail/$expenseId")
                            }
                        )
                    }
                    2 -> {
                        val currencySymbol = completeTripDetails?.trip?.currency?.let {
                            com.example.expensecalculator.util.CurrencyCode.fromCode(it).symbol
                        } ?: "₹"
                        val settlementPayments by viewModel.settlementPayments.collectAsState()

                        BalancesWithHistoryScreen(
                            balances = adjustedBalances,
                            settlements = optimizedSettlements,
                            settlementPayments = settlementPayments,
                            currencySymbol = currencySymbol,
                            onRecordPayment = { from, to, amount ->
                                viewModel.addSettlementPayment(from, to, amount)
                            },
                            onDeletePayment = { payment ->
                                viewModel.deleteSettlementPayment(payment)
                            }
                        )
                    }
                    3 -> PhotosContent(
                        photos = currentTripPhotos,
                        onAddPhoto = { imagePickerLauncher.launch("image/*") },
                        onDeletePhoto = { viewModel.deletePhoto(it) }
                    )
                }
            }

            // Custom FAB - only show for Expenses, Categories, and Photos tabs
            if (selectedTabIndex == 0 || selectedTabIndex == 1 || selectedTabIndex == 3) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FloatingActionButton(
                        onClick = {
                            when (selectedTabIndex) {
                                0 -> showAddExpenseDialog = true
                                1 -> showAddCategoryDialog = true
                                3 -> imagePickerLauncher.launch("image/*")
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Add,
                            when (selectedTabIndex) {
                                0 -> "Add Expense"
                                1 -> "Add Category"
                                else -> "Add Photo"
                            },
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        when (selectedTabIndex) {
                            0 -> "Add Expense"
                            1 -> "Add Category"
                            else -> "Add Photo"
                        },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialogWithSplits(
            participants = currentTripParticipants,
            categories = viewModel.currentTripCategories.collectAsState().value,
            onDismiss = { showAddExpenseDialog = false },
            onAddExpense = { expenseName, amount, paidBy, participantsInSplit, categoryId ->
                viewModel.addExpense(expenseName, amount, paidBy, participantsInSplit, categoryId)
                showAddExpenseDialog = false
            },
            onCreateCategory = { categoryName ->
                viewModel.addCategory(categoryName)
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onAddCategory = { categoryName ->
                viewModel.addCategory(categoryName)
                showAddCategoryDialog = false
            }
        )
    }

    // Delete trip confirmation dialog
    if (showDeleteTripDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTripDialog = false },
            title = { Text("Delete Trip") },
            text = { Text("Are you sure you want to delete this trip? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        completeTripDetails?.trip?.let {
                            viewModel.deleteTripCompletely(it)
                            navController.popBackStack()
                        }
                        showDeleteTripDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTripDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExpensesContent(
    expensesWithSplits: List<com.example.expensecalculator.tripData.ExpenseWithSplits>,
    currencySymbol: String,
    onDeleteExpense: (TripExpense) -> Unit,
    onExpenseClick: (Int) -> Unit = {}
) {
    if (expensesWithSplits.isEmpty()) {
        TripDetailEmptyState(
            icon = Icons.Default.Search,
            title = "No Expenses Yet",
            subtitle = "Add an expense by tapping on the \"+\" to start\ntracking and splitting your expenses"
        )
    } else {
        val totalExpenses = expensesWithSplits.sumOf { it.expense.amount }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Total Expenses",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            "$currencySymbol${"%,.2f".format(totalExpenses)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            val groupedExpenses = expensesWithSplits.groupBy { it.expense.date }
            groupedExpenses.forEach { (date, expenses) ->
                item {
                    Text(
                        text = date ?: "No Date",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }
                items(expenses) { expenseWithSplits ->
                    ExpenseCard(
                        expense = expenseWithSplits.expense,
                        currencySymbol = currencySymbol,
                        onDelete = { onDeleteExpense(expenseWithSplits.expense) },
                        onEdit = { onExpenseClick(expenseWithSplits.expense.id) },
                        onClick = { onExpenseClick(expenseWithSplits.expense.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceItem(name: String, balance: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(IconBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.firstOrNull()?.toString() ?: "",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            name,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        val color = if (balance > 0) PositiveBalanceColor
                    else if (balance < 0) NegativeBalanceColor
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        val sign = if (balance > 0) "+" else ""
        Text(
            text = "$sign₹${"%,.2f".format(abs(balance))}",
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun TripDetailEmptyState(icon: ImageVector, title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ExpenseCard(
    expense: TripExpense,
    currencySymbol: String,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    "Expense Icon",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(IconBackground)
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        expense.expenseName,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Paid by: ${expense.paidBy}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$currencySymbol${"%,.0f".format(expense.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = { onEdit() }) {
                Icon(
                    Icons.Default.Edit,
                    "Edit",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { onDelete() }) {
                Icon(
                    Icons.Default.DeleteOutline,
                    "Delete",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialogWithSplits(
    participants: List<TripParticipant>,
    categories: List<com.example.expensecalculator.tripData.ExpenseCategory>,
    onDismiss: () -> Unit,
    onAddExpense: (String, Double, String, List<String>, Int?) -> Unit,
    onCreateCategory: (String) -> Unit
) {
    var expenseName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf(participants.firstOrNull()) }
    var selectedCategory by remember { mutableStateOf<com.example.expensecalculator.tripData.ExpenseCategory?>(null) }
    var splitParticipants by remember { mutableStateOf(participants.map { it.participantName }.toSet()) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val splitValue = if (splitParticipants.isNotEmpty()) amountValue / splitParticipants.size else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Expense", fontWeight = FontWeight.Bold) },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = expenseName,
                    onValueChange = { expenseName = it },
                    label = { Text("Title *") }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount *") }
                )

                var paidByExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = paidByExpanded,
                    onExpandedChange = { paidByExpanded = !paidByExpanded }
                ) {
                    OutlinedTextField(
                        value = paidBy?.participantName ?: "Select Payer",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid By *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = paidByExpanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = paidByExpanded,
                        onDismissRequest = { paidByExpanded = false }
                    ) {
                        participants.forEach { participant ->
                            DropdownMenuItem(
                                text = { Text(participant.participantName) },
                                onClick = {
                                    paidBy = participant
                                    paidByExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category selection dropdown - always shown
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.categoryName ?: "No Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category (Optional)") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        // Add new category option
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create New Category", fontWeight = FontWeight.Medium)
                                }
                            },
                            onClick = {
                                categoryExpanded = false
                                showCreateCategoryDialog = true
                            }
                        )

                        if (categories.isNotEmpty()) {
                            HorizontalDivider()
                        }

                        DropdownMenuItem(
                            text = { Text("No Category") },
                            onClick = {
                                selectedCategory = null
                                categoryExpanded = false
                            }
                        )

                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.categoryName) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "Split",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                participants.forEach { participant ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val currentSelection = splitParticipants.toMutableSet()
                            if (splitParticipants.contains(participant.participantName)) {
                                currentSelection.remove(participant.participantName)
                            } else {
                                currentSelection.add(participant.participantName)
                            }
                            splitParticipants = currentSelection
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = splitParticipants.contains(participant.participantName),
                            onCheckedChange = { isChecked ->
                                val currentSelection = splitParticipants.toMutableSet()
                                if (isChecked) {
                                    currentSelection.add(participant.participantName)
                                } else {
                                    currentSelection.remove(participant.participantName)
                                }
                                splitParticipants = currentSelection
                            }
                        )
                        Text(participant.participantName, modifier = Modifier.weight(1f))
                        Text("₹${"%.2f".format(splitValue)}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (expenseName.isNotBlank() && amountValue > 0 && paidBy != null && splitParticipants.isNotEmpty()) {
                        onAddExpense(expenseName, amountValue, paidBy!!.participantName, splitParticipants.toList(), selectedCategory?.id)
                    }
                },
                enabled = expenseName.isNotBlank() && amountValue > 0 && paidBy != null && splitParticipants.isNotEmpty()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    // Create category dialog from expense dialog
    if (showCreateCategoryDialog) {
        CreateCategoryFromExpenseDialog(
            onDismiss = { showCreateCategoryDialog = false },
            onCategoryCreated = { categoryName ->
                onCreateCategory(categoryName)
                showCreateCategoryDialog = false
            }
        )
    }
}

@Composable
fun PhotosContent(
    photos: List<TripPhoto>,
    onAddPhoto: () -> Unit,
    onDeletePhoto: (TripPhoto) -> Unit
) {
    if (photos.isEmpty()) {
        TripDetailEmptyState(
            icon = Icons.Default.PhotoLibrary,
            title = "No Photos Yet",
            subtitle = "Add photos of your trip by tapping\nthe \"+\" button below"
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(photos) { photo ->
                PhotoGridItem(
                    photo = photo,
                    onDelete = { onDeletePhoto(photo) }
                )
            }
        }
    }
}

@Composable
fun PhotoGridItem(
    photo: TripPhoto,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFullScreenPhoto by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { showFullScreenPhoto = true }
    ) {
        AsyncImage(
            model = photo.photoUri.toUri(),
            contentDescription = "Trip photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Delete icon overlay - smaller size
        IconButton(
            onClick = {
                showDeleteDialog = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Delete photo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
        }
    }

    // Full screen photo dialog
    if (showFullScreenPhoto) {
        FullScreenPhotoDialog(
            photoUri = photo.photoUri,
            onDismiss = { showFullScreenPhoto = false }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Photo") },
            text = { Text("Are you sure you want to delete this photo?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FullScreenPhotoDialog(
    photoUri: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = photoUri.toUri(),
                    contentDescription = "Full screen photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(16.dp)
    )
}

// CATEGORIES CONTENT IMPLEMENTATION
@Composable
fun CategoriesContent(
    categoriesWithExpenses: List<com.example.expensecalculator.tripData.CategoryWithExpenses>,
    currencySymbol: String,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (com.example.expensecalculator.tripData.ExpenseCategory) -> Unit,
    onExpenseClick: (Int) -> Unit
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    if (categoriesWithExpenses.isEmpty()) {
        TripDetailEmptyState(
            icon = Icons.Default.Category,
            title = "No Categories Yet",
            subtitle = "Categories will appear here once you add expenses\nwith category assignments"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categoriesWithExpenses) { categoryWithExpenses ->
                CategoryCard(
                    categoryWithExpenses = categoryWithExpenses,
                    currencySymbol = currencySymbol,
                    onDeleteCategory = onDeleteCategory,
                    onExpenseClick = onExpenseClick
                )
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onAddCategory = { categoryName ->
                onAddCategory(categoryName)
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
fun CategoryCard(
    categoryWithExpenses: com.example.expensecalculator.tripData.CategoryWithExpenses,
    currencySymbol: String,
    onDeleteCategory: (com.example.expensecalculator.tripData.ExpenseCategory) -> Unit,
    onExpenseClick: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val category = categoryWithExpenses.category
    val expenses = categoryWithExpenses.expenses
    val totalAmount = expenses.sumOf { it.expense.amount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Category Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        category.categoryName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "${expenses.size} expense${if (expenses.size != 1) "s" else ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Text(
                    "$currencySymbol${"%,.2f".format(totalAmount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Expanded expenses list
            if (expanded && expenses.isNotEmpty()) {
                HorizontalDivider()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    expenses.forEach { expenseWithSplits ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onExpenseClick(expenseWithSplits.expense.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    expenseWithSplits.expense.expenseName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    "Paid by: ${expenseWithSplits.expense.paidBy}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                "$currencySymbol${"%,.2f".format(expenseWithSplits.expense.amount)}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        if (expenseWithSplits != expenses.last()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Category", fontWeight = FontWeight.Bold) },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name *") },
                placeholder = { Text("e.g., Food, Transport") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onAddCategory(categoryName.trim())
                    }
                },
                enabled = categoryName.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCategoryFromExpenseDialog(
    onDismiss: () -> Unit,
    onCategoryCreated: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Category", fontWeight = FontWeight.Bold) },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name *") },
                placeholder = { Text("e.g., Food, Transport") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onCategoryCreated(categoryName.trim())
                    }
                },
                enabled = categoryName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
