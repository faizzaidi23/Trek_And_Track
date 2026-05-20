package com.example.expensecalculator.TripManager

import com.example.expensecalculator.tripData.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreTripSync {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid get() = auth.currentUser?.uid ?: ""

    // Sync trip to Firestore after creating in Room
    suspend fun syncTripToFirestore(trip: Trip, participants: List<TripParticipant>): String {
        val tripData = hashMapOf(
            "title" to trip.title,
            "currency" to trip.currency,
            "createdBy" to uid,
            "tripIconUri" to trip.tripIconUri,
            "participants" to participants.map { it.participantName }
        )

        val docRef = if (trip.firestoreId != null) {
            db.collection("trips").document(trip.firestoreId)
                .also { it.set(tripData).await() }
        } else {
            db.collection("trips").add(tripData).await()
        }

        return docRef.id
    }

    // Sync expense to Firestore
    suspend fun syncExpenseToFirestore(firestoreTripId: String, expense: TripExpense, splits: List<ExpenseSplit>) {
        val expenseData = hashMapOf(
            "expenseName" to expense.expenseName,
            "amount" to expense.amount,
            "paidBy" to expense.paidBy,
            "date" to expense.date,
            "splitType" to expense.splitType,
            "categoryId" to expense.categoryId,
            "splits" to splits.map { split ->
                hashMapOf(
                    "participantName" to split.participantName,
                    "shareAmount" to split.shareAmount
                )
            }
        )
        db.collection("trips")
            .document(firestoreTripId)
            .collection("expenses")
            .add(expenseData)
            .await()
    }

    // Delete trip from Firestore
    suspend fun deleteTripFromFirestore(firestoreId: String) {
        db.collection("trips").document(firestoreId).delete().await()
    }

    // Delete expense from Firestore
    suspend fun deleteExpenseFromFirestore(firestoreTripId: String, firestoreExpenseId: String) {
        db.collection("trips")
            .document(firestoreTripId)
            .collection("expenses")
            .document(firestoreExpenseId)
            .delete()
            .await()
    }
}