package com.example.expensecalculator.Data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expensecalculator.tripData.ExpenseSplit // --- 1. ADD: Import for the new entity
import com.example.expensecalculator.tripData.Trip
import com.example.expensecalculator.tripData.TripDao
import com.example.expensecalculator.tripData.TripExpense
import com.example.expensecalculator.tripData.TripParticipant
import com.example.expensecalculator.tripData.TripPhoto
import com.example.expensecalculator.tripData.SettlementPayment // --- 2. ADD: Import for the SettlementPayment entity
import com.example.expensecalculator.tripData.ExpenseCategory // Import for the ExpenseCategory entity
import java.util.Date

@TypeConverters(Converters::class)
@Database(
    entities = [
        Account::class,
        Expense::class,
        Trip::class,
        TripParticipant::class,
        TripExpense::class,
        ExpenseSplit::class,
        TripPhoto::class,
        SettlementPayment::class,
        ExpenseCategory::class // Add ExpenseCategory entity
    ],
    version = 17,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_db"
                )
                .addMigrations(MIGRATION_16_17)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE trips ADD COLUMN firestoreId TEXT")
        database.execSQL("ALTER TABLE trips ADD COLUMN createdBy TEXT")
    }
}

class Converters {
    @androidx.room.TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @androidx.room.TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}