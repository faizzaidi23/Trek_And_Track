package com.example.expensecalculator.tripData

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation


@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val days: Int? = null,
    val expenditure: Double? = null,
    val tripIconUri: String? = null,
    val currency: String = "INR", // Base currency for the trip (ISO code)
    val firestoreId:String?=null,
    val createdBy:String?=null
)

@Entity(
    tableName = "trip_participants",
    foreignKeys = [ForeignKey(entity = Trip::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)]
)
data class TripParticipant(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tripId: Int,
    val participantName: String,
    val contactNumber: String? = null,
    val email: String? = null
)

// Expense Category Entity
@Entity(
    tableName = "expense_categories",
    foreignKeys = [ForeignKey(entity = Trip::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)]
)
data class ExpenseCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tripId: Int,
    val categoryName: String,
    val iconName: String = "Category", // For future icon support
    val color: String? = null // Color in hex format - nullable, no default
)

@Entity(
    tableName = "tripExpense",
    foreignKeys = [
        ForeignKey(entity = Trip::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExpenseCategory::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class TripExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tripId: Int,
    val expenseName: String,
    val amount: Double,
    val paidBy: String, // The name of the participant who paid
    val date: String? = null,
    val splitType: String = "EQUALLY",
    val categoryId: Int? = null // Foreign key to ExpenseCategory
)

// This is the crucial new table to track splits
@Entity(
    tableName = "expense_splits",
    foreignKeys = [
        ForeignKey(entity = TripExpense::class, parentColumns = ["id"], childColumns = ["expenseId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class ExpenseSplit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val expenseId: Int, // Links this split to a specific expense
    val participantName: String, // The name of the person who has a share in this expense
    val shareAmount: Double // The amount of this person's share
)

//  For storing trip photos
@Entity(
    tableName = "trip_photos",
    foreignKeys = [ForeignKey(entity = Trip::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)]
)
data class TripPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tripId: Int,
    val photoUri: String, // URI/path to the photo
    val caption: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// Helper class to bundle an expense with its splits and category
data class ExpenseWithSplits(
    @Embedded val expense: TripExpense,
    @Relation(
        parentColumn = "id",
        entityColumn = "expenseId"
    )
    val splits: List<ExpenseSplit>,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: ExpenseCategory? = null
)

// Helper class for category with its expenses
data class CategoryWithExpenses(
    @Embedded val category: ExpenseCategory,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId",
        entity = TripExpense::class
    )
    val expenses: List<ExpenseWithSplits>
)

data class CompleteTripDetails(
    @Embedded val trip: Trip,
    @Relation(
        parentColumn = "id",
        entityColumn = "tripId"
    )
    val participants: List<TripParticipant>,
    @Relation(
        entity = TripExpense::class,
        parentColumn = "id",
        entityColumn = "tripId"
    )
    val expensesWithSplits: List<ExpenseWithSplits>,
    @Relation(
        parentColumn = "id",
        entityColumn = "tripId"
    )
    val photos: List<TripPhoto> = emptyList(),
    @Relation(
        parentColumn = "id",
        entityColumn = "tripId"
    )
    val categories: List<ExpenseCategory> = emptyList()
)