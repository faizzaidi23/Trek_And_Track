package com.example.expensecalculator.TripManager

import android.util.Log
import com.example.expensecalculator.tripData.CategoryWithExpenses
import com.example.expensecalculator.tripData.CompleteTripDetails
import com.example.expensecalculator.tripData.ExpenseCategory
import com.example.expensecalculator.tripData.ExpenseSplit
import com.example.expensecalculator.tripData.ExpenseWithSplits
import com.example.expensecalculator.tripData.SettlementPayment
import com.example.expensecalculator.tripData.Trip
import com.example.expensecalculator.tripData.TripDao
import com.example.expensecalculator.tripData.TripExpense
import com.example.expensecalculator.tripData.TripParticipant
import com.example.expensecalculator.tripData.TripPhoto
import kotlinx.coroutines.flow.Flow

class TripRepository(private val tripDao: TripDao) {

    private val firestoreSync = FirestoreTripSync()

    fun getAllTrips(userId: String): Flow<List<Trip>> = tripDao.showAllTrips(userId)

    suspend fun addTripWithParticipants(
        title: String,
        participantNames: List<String>,
        tripIconUri: String? = null,
        currency: String = "INR",
        createdBy: String = ""
    ) {
        Log.d("TripRepo", "Creating trip with createdBy: $createdBy")
        // 1. Save to Room first
        val newTrip = Trip(title = title, tripIconUri = tripIconUri, currency = currency, createdBy = createdBy)
        val tripId = tripDao.addTrip(newTrip)
        val participants = participantNames.map { name ->
            TripParticipant(tripId = tripId.toInt(), participantName = name)
        }
        tripDao.addParticipants(participants)

        // 2. Sync to Firestore
        try {
            val firestoreId = firestoreSync.syncTripToFirestore(
                newTrip.copy(id = tripId.toInt()),
                participants
            )
            // 3. Save firestoreId back to Room
            tripDao.updateFirestoreId(tripId.toInt(), firestoreId)
        } catch (_: Exception) {
            // Firestore sync failed but Room has data — offline support works
        }
    }

    suspend fun deleteTripCompletely(trip: Trip) {
        tripDao.deleteTripCompletely(trip)
        try {
            trip.firestoreId?.let { firestoreSync.deleteTripFromFirestore(it) }
        } catch (_: Exception) { }
    }

    suspend fun addExpenseWithSplits(expense: TripExpense, splits: List<ExpenseSplit>) {
        tripDao.addExpenseWithSplits(expense, splits)
        try {
            val trip = tripDao.getCompleteTripDetails(expense.tripId)
            trip?.trip?.firestoreId?.let { firestoreId ->
                firestoreSync.syncExpenseToFirestore(firestoreId, expense, splits)
            }
        } catch (_: Exception) { }
    }

    suspend fun updateTripWithParticipants(tripId: Int, title: String, participantNames: List<String>, tripIconUri: String? = null, currency: String = "INR") {
        val existingTrip = getCompleteTripDetails(tripId)?.trip ?: Trip(id = tripId, title = title, currency = currency)
        val tripToUpdate = existingTrip.copy(title = title, tripIconUri = tripIconUri ?: existingTrip.tripIconUri, currency = currency)
        tripDao.updateTripWithParticipants(tripToUpdate, participantNames)
        try {
            val participants = tripDao.getCompleteTripDetails(tripId)?.participants ?: emptyList()
            existingTrip.firestoreId?.let {
                firestoreSync.syncTripToFirestore(tripToUpdate, participants)
            }
        } catch (_: Exception) { }
    }

    suspend fun getCompleteTripDetails(tripId: Int): CompleteTripDetails? = tripDao.getCompleteTripDetails(tripId)
    fun getCompleteTripDetailsFlow(tripId: Int): Flow<CompleteTripDetails?> = tripDao.getCompleteTripDetailsFlow(tripId)
    suspend fun updateTripIcon(tripId: Int, iconUri: String) {
        val existingTrip = getCompleteTripDetails(tripId)?.trip ?: return
        tripDao.updateTrip(existingTrip.copy(tripIconUri = iconUri))
    }
    fun getExpenseWithSplitsById(expenseId: Int): Flow<ExpenseWithSplits?> = tripDao.getExpenseWithSplitsByIdFlow(expenseId)


    suspend fun deleteExpense(expense: TripExpense) = tripDao.deleteExpense(expense)


    suspend fun addPhoto(photo: TripPhoto): Long = tripDao.addPhoto(photo)


    suspend fun deletePhoto(photo: TripPhoto) = tripDao.deletePhoto(photo)


    suspend fun addSettlementPayment(payment: SettlementPayment): Long = tripDao.addSettlementPayment(payment)


    suspend fun deleteSettlementPayment(payment: SettlementPayment) = tripDao.deleteSettlementPayment(payment)


    fun getSettlementPaymentsByTripId(tripId: Int): Flow<List<SettlementPayment>> = tripDao.getSettlementPaymentsByTripIdFlow(tripId)


    suspend fun updateTripCurrency(tripId: Int, currency: String) = tripDao.updateTripCurrency(tripId, currency)


    suspend fun addCategory(category: ExpenseCategory): Long = tripDao.addCategory(category)



    suspend fun deleteCategory(category: ExpenseCategory) = tripDao.deleteCategory(category)


    fun getCategoriesWithExpenses(tripId: Int): Flow<List<CategoryWithExpenses>> = tripDao.getCategoriesWithExpensesFlow(tripId)


    suspend fun createDefaultCategories(tripId: Int) = tripDao.createDefaultCategories(tripId)



}