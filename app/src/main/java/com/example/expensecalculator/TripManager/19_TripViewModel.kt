package com.example.expensecalculator.TripManager

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensecalculator.Storage.ExportFormat
import com.example.expensecalculator.network.RetrofitClient
import com.example.expensecalculator.repository.ExchangeRateRepository
import com.example.expensecalculator.tripData.CompleteTripDetails
import com.example.expensecalculator.tripData.ExpenseCategory
import com.example.expensecalculator.tripData.CategoryWithExpenses
import com.example.expensecalculator.tripData.ExpenseSplit
import com.example.expensecalculator.tripData.ExpenseWithSplits
import com.example.expensecalculator.tripData.SettlementPayment
import com.example.expensecalculator.tripData.Trip
import com.example.expensecalculator.tripData.TripExpense
import com.example.expensecalculator.tripData.TripParticipant
import com.example.expensecalculator.tripData.TripPhoto
import com.example.expensecalculator.util.CurrencyUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripViewModel(
    private val repository: TripRepository,
    context: Context
) : ViewModel() {

    private val exportManager = TripExportManager(context.applicationContext)

    // Exchange rate repository for currency conversion
    private val exchangeRateRepository = ExchangeRateRepository(
        RetrofitClient.exchangeRateApi,
        context.getSharedPreferences("exchange_rates", Context.MODE_PRIVATE)
    )

    // Current exchange rate for display
    private val _currentExchangeRate = MutableStateFlow<Double?>(null)
    val currentExchangeRate: StateFlow<Double?> = _currentExchangeRate.asStateFlow()

    // Loading state for exchange rate fetch
    private val _isLoadingExchangeRate = MutableStateFlow(false)
    val isLoadingExchangeRate: StateFlow<Boolean> = _isLoadingExchangeRate.asStateFlow()

    val allTrips = repository.getAllTrips(
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    )

    private val _completeTripDetails = MutableStateFlow<CompleteTripDetails?>(null)
    val completeTripDetails: StateFlow<CompleteTripDetails?> = _completeTripDetails.asStateFlow()

    val currentTripExpenses: StateFlow<List<TripExpense>> = _completeTripDetails.map {
        it?.expensesWithSplits?.map { expenseWithSplits -> expenseWithSplits.expense } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentTripParticipants: StateFlow<List<TripParticipant>> = _completeTripDetails.map {
        it?.participants ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())



    val tripBalances: StateFlow<Map<String, Double>> = _completeTripDetails.map { details ->
        if (details == null) {
            emptyMap()
        } else {
            val balances = mutableMapOf<String, Double>()
            // 1. Initialize balances for all participants to 0
            details.participants.forEach { participant ->
                balances[participant.participantName] = 0.0
            }

            // 2. Process each expense
            details.expensesWithSplits.forEach { expenseWithSplits ->
                val expense = expenseWithSplits.expense
                val paidBy = expense.paidBy
                val totalAmount = expense.amount

                // Add the full amount to the person who paid
                balances[paidBy] = (balances[paidBy] ?: 0.0) + totalAmount

                // Subtract each person's share from their balance
                expenseWithSplits.splits.forEach { split ->
                    val participantName = split.participantName
                    val shareAmount = split.shareAmount
                    balances[participantName] = (balances[participantName] ?: 0.0) - shareAmount
                }
            }
            balances
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // Settlement payments for current trip
    private val _settlementPayments = MutableStateFlow<List<SettlementPayment>>(emptyList())
    val settlementPayments: StateFlow<List<SettlementPayment>> = _settlementPayments.asStateFlow()

    // Adjusted balances after settlement payments
    val adjustedBalances: StateFlow<Map<String, Double>> = combine(
        tripBalances,
        settlementPayments
    ) { baseBalances, payments ->
        val adjusted = baseBalances.toMutableMap()
        payments.forEach { payment ->
            adjusted[payment.fromParticipant] = (adjusted[payment.fromParticipant] ?: 0.0) + payment.amount
            adjusted[payment.toParticipant] = (adjusted[payment.toParticipant] ?: 0.0) - payment.amount
        }
        adjusted
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // Optimized settlements calculated from adjusted balances
    val optimizedSettlements: StateFlow<List<Settlement>> = adjustedBalances.map { balances ->
        SettlementOptimizer.calculateOptimizedSettlements(balances)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    fun saveTrip(tripId: Int?, title: String, participantNames: List<String>, tripIconUri: String? = null, currency: String = "INR") {
        viewModelScope.launch {
            if (tripId == null || tripId == -1) {
                repository.addTripWithParticipants(title, participantNames, tripIconUri, currency, FirebaseAuth.getInstance().currentUser?.uid ?: "")
            } else {
                repository.updateTripWithParticipants(tripId, title, participantNames, tripIconUri, currency)
            }
        }
    }

    //  Update trip icon
    fun updateTripIcon(tripId: Int, iconUri: String) {
        viewModelScope.launch {
            repository.updateTripIcon(tripId, iconUri)
        }
    }

    suspend fun getTripById(tripId: Int): CompleteTripDetails? {
        return repository.getCompleteTripDetails(tripId)
    }

    fun deleteTripCompletely(trip: Trip) {
        viewModelScope.launch {
            repository.deleteTripCompletely(trip)
        }
    }

    fun setCurrentTrip(tripId: Int) {
        viewModelScope.launch {
            repository.getCompleteTripDetailsFlow(tripId).collect { details ->
                _completeTripDetails.value = details
            }
        }
        loadSettlementPayments(tripId)
    }

    fun clearCurrentTrip() {
        _completeTripDetails.value = null
    }

    fun addExpense(
        expenseName: String,
        amount: Double,
        paidBy: String,
        participantsInSplit: List<String>,
        categoryId: Int? = null
    ) {
        val tripId = _completeTripDetails.value?.trip?.id ?: return
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            val expense = TripExpense(
                tripId = tripId,
                expenseName = expenseName,
                amount = amount,
                paidBy = paidBy,
                date = currentDate,
                categoryId = categoryId
            )

            val shareAmount = amount / participantsInSplit.size
            val splits = participantsInSplit.map { participantName ->
                ExpenseSplit(
                    expenseId = 0, // This will be set by the DAO
                    participantName = participantName,
                    shareAmount = shareAmount
                )
            }
            repository.addExpenseWithSplits(expense, splits)
        }
    }

    fun deleteExpense(expense: TripExpense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun getExpenseWithSplitsById(expenseId: Int): StateFlow<ExpenseWithSplits?> {
        return repository.getExpenseWithSplitsById(expenseId)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    }


    val currentTripPhotos: StateFlow<List<TripPhoto>> = _completeTripDetails.map {
        it?.photos ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // CATEGORY OPERATIONS
    val currentTripCategories: StateFlow<List<ExpenseCategory>> = _completeTripDetails.map {
        it?.categories ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _categoriesWithExpenses = MutableStateFlow<List<CategoryWithExpenses>>(emptyList())
    val categoriesWithExpenses: StateFlow<List<CategoryWithExpenses>> = _categoriesWithExpenses.asStateFlow()

    fun loadCategoriesWithExpenses(tripId: Int) {
        viewModelScope.launch {
            repository.getCategoriesWithExpenses(tripId).collect { categories ->
                _categoriesWithExpenses.value = categories
            }
        }
    }

    fun addCategory(categoryName: String) {
        val tripId = _completeTripDetails.value?.trip?.id ?: return
        viewModelScope.launch {
            val category = ExpenseCategory(
                tripId = tripId,
                categoryName = categoryName,
                color = null // No color assigned
            )
            repository.addCategory(category)
        }
    }


    fun deleteCategory(category: ExpenseCategory) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // PHOTO OPERATIONS
    fun addPhoto(photoUri: String, caption: String? = null) {
        val tripId = _completeTripDetails.value?.trip?.id ?: return
        viewModelScope.launch {
            val photo = TripPhoto(
                tripId = tripId,
                photoUri = photoUri,
                caption = caption
            )
            repository.addPhoto(photo)
        }
    }

    fun deletePhoto(photo: TripPhoto) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
        }
    }

    // SETTLEMENT PAYMENT OPERATIONS
    fun addSettlementPayment(fromParticipant: String, toParticipant: String, amount: Double) {
        val tripId = _completeTripDetails.value?.trip?.id ?: return
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            val payment = SettlementPayment(
                tripId = tripId,
                fromParticipant = fromParticipant,
                toParticipant = toParticipant,
                amount = amount,
                date = currentDate
            )
            repository.addSettlementPayment(payment)
        }
    }

    fun deleteSettlementPayment(payment: SettlementPayment) {
        viewModelScope.launch {
            repository.deleteSettlementPayment(payment)
        }
    }

    private fun loadSettlementPayments(tripId: Int) {
        viewModelScope.launch {
            repository.getSettlementPaymentsByTripId(tripId).collect { payments ->
                _settlementPayments.value = payments
            }
        }
    }

    // CURRENCY OPERATIONS
    /**
     * Get exchange rate between two currencies
     */
    fun getExchangeRate(fromCurrency: String, toCurrency: String, onResult: (Double?) -> Unit) {
        viewModelScope.launch {
            _isLoadingExchangeRate.value = true
            try {
                val rate = exchangeRateRepository.getRate(fromCurrency, toCurrency)
                _currentExchangeRate.value = rate
                onResult(rate)
            } catch (e: Exception) {
                _currentExchangeRate.value = null
                onResult(null)
            } finally {
                _isLoadingExchangeRate.value = false
            }
        }
    }

    /**
     * Update trip currency
     */
    fun updateTripCurrency(tripId: Int, currency: String) {
        viewModelScope.launch {
            repository.updateTripCurrency(tripId, currency)
        }
    }

    /**
     * Format amount with trip's currency
     */
    fun formatAmount(amount: Double): String {
        val currency = _completeTripDetails.value?.trip?.currency ?: "INR"
        return CurrencyUtils.format(amount, currency)
    }

    /**
     * Refresh exchange rates for current trip currency
     */
    fun refreshExchangeRates(onComplete: (Boolean) -> Unit) {
        val currency = _completeTripDetails.value?.trip?.currency ?: "INR"
        viewModelScope.launch {
            try {
                exchangeRateRepository.refreshRates(currency)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    //EXPORT OPERATIONS
    fun exportTrip(format: ExportFormat, onComplete: (Uri?) -> Unit) {
        // Check if Android version supports the export functionality
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onComplete(null)
            return
        }

        val tripDetails = _completeTripDetails.value ?: run {
            onComplete(null)
            return
        }

        viewModelScope.launch {
            val uri = when (format) {
                ExportFormat.CSV -> exportManager.exportTripToCSV(tripDetails)
                ExportFormat.PDF -> exportManager.exportTripToPDF(tripDetails)
                ExportFormat.EXCEL -> exportManager.exportTripToExcel(tripDetails)
            }
            onComplete(uri)
        }
    }



    init {
        Log.d("TripViewModel", "UID for trips: ${FirebaseAuth.getInstance().currentUser?.uid}")
    }
}

class TripViewModelFactory(
    private val repository: TripRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TripViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}