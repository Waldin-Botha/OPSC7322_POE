package za.ac.iie.opsc_poe_screens

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main Room database class for the app.
 * Contains all entities and provides DAOs for database operations.
 */
@Database(
    entities = [AccountEntity::class, CategoryEntity::class, TransactionEntity::class, GoalEntity::class, UserEntity::class],
    version = 2,  // Incremented version for schema changes
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // DAOs
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * Get a singleton instance of the database.
         */
        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
