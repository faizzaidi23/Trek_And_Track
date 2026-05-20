package com.example.expensecalculator.Data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expensecalculator.tripData.ExpenseSplit
import com.example.expensecalculator.tripData.Trip
import com.example.expensecalculator.tripData.TripDao
import com.example.expensecalculator.tripData.TripExpense
import com.example.expensecalculator.tripData.TripParticipant
import com.example.expensecalculator.tripData.TripPhoto
import com.example.expensecalculator.tripData.SettlementPayment
import com.example.expensecalculator.tripData.ExpenseCategory
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
        ExpenseCategory::class
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

        fun getDatabase(context: Context, userId: String): ExpenseDatabase {
            Log.d("Database", "getDatabase called with userId: $userId, dbName: expense_db_$userId")
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_db_$userId"
                )
                    .addMigrations(MIGRATION_16_17)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun clearInstance() {
            INSTANCE = null
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