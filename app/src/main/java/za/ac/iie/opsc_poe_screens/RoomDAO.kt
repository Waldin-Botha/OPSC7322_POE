package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

//==================================================================================================
// ACCOUNT DAO
//==================================================================================================
@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE userId = :userId")
    suspend fun getAllAccounts(userId: Int): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE userId = :userId")
    fun getAllLiveAccounts(userId: Int): LiveData<List<AccountEntity>>

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET Balance = :newBalance WHERE id = :accountId")
    suspend fun updateBalance(accountId: Int, newBalance: Float)

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountById(accountId: Int): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE accountName = :accountName AND userId = :userId")
    suspend fun getPossibleAccountByName(accountName: String, userId: Int): AccountEntity?

    /**
     * Inserts default accounts for a new user.
     */
    @Transaction
    suspend fun createDefaultAccountsForUser(userId: Int) {
        val accounts = listOf(
            AccountEntity(
                AccountName = "Bank",
                Balance = 0f,
                Colour = android.graphics.Color.parseColor("#388E3C"), // green
                userId = userId
            ),
            AccountEntity(
                AccountName = "Savings",
                Balance = 0f,
                Colour = android.graphics.Color.parseColor("#1976D2"), // blue
                userId = userId
            )
        )
        accounts.forEach { insert(it) }
    }
}

//==================================================================================================
// CATEGORY DAO
//==================================================================================================
@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isIncome = 1 AND userId = :userId")
    suspend fun getIncomeCategories(userId: Int): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE isIncome = 0 AND userId = :userId")
    suspend fun getExpenseCategories(userId: Int): List<CategoryEntity>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE isIncome = 1 AND userId = :userId")
    fun getLiveIncomeCategories(userId: Int): LiveData<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isIncome = 0 AND userId = :userId")
    fun getLiveExpenseCategories(userId: Int): LiveData<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE name = :name AND userId = :userId AND isIncome = :isIncome")
    fun getCategoryByName(name: String, userId: Int, isIncome: Boolean = false): CategoryEntity

    @Query("Select * FROM categories WHERE name = :name AND userId = :userID ")
    suspend fun getPossibleCategoryByName(name: String, userID: Int): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    fun getTransactionCountForCategory(categoryId: Int): Int

    @Transaction
    suspend fun insertDefaultCategoriesForUser(userId: Int): List<Long> {
        val incomeCategories = listOf(
            CategoryEntity(name = "Salary", iconId = R.drawable.ic_dollar_foreground, isIncome = true, userId = userId),
            CategoryEntity(name = "Gift", iconId = R.drawable.ic_present_foreground, isIncome = true, userId = userId),
            CategoryEntity(name = "Investment", iconId = R.drawable.ic_investment_foreground, isIncome = true, userId = userId),
            CategoryEntity(name = "Other", iconId = R.drawable.ic_more_foreground, isIncome = true, userId = userId),
            CategoryEntity(name = "Transfer", iconId = R.drawable.ic_transfer_foreground, isIncome = true, userId = userId)
        )

        val expenseCategories = listOf(
            CategoryEntity(name = "Groceries", iconId = R.drawable.ic_food_foreground, isIncome = false, userId = userId),
            CategoryEntity(name = "Transport", iconId = R.drawable.ic_car_foreground, isIncome = false, userId = userId),
            CategoryEntity(name = "Shopping", iconId = R.drawable.ic_shopping_foreground, isIncome = false, userId = userId),
            CategoryEntity(name = "Bills", iconId = R.drawable.ic_bills_foreground, isIncome = false, userId = userId),
            CategoryEntity(name = "Other", iconId = R.drawable.ic_more_foreground, isIncome = false, userId = userId),
            CategoryEntity(name = "Transfer", iconId = R.drawable.ic_transfer_foreground, isIncome = false, userId = userId)
        )

        val insertedIds = mutableListOf<Long>()
        (incomeCategories + expenseCategories).forEach {
            insertedIds.add(insertCategory(it))
        }
        return insertedIds
    }
}

//==================================================================================================
// TRANSACTION DAO
//==================================================================================================
@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Int): TransactionEntity?

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Transaction
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllTransactionsWithDetails(userId: Int): Flow<List<TransactionWithAccountAndCategory>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND userId = :userId ORDER BY date DESC")
    fun getTransactionsForAccount(accountId: Int, userId: Int): Flow<List<TransactionWithAccountAndCategory>>

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("UPDATE accounts SET balance = balance - :amount WHERE id = :accountId")
    suspend fun adjustAccountBalance(accountId: Int, amount: Float)

    @Transaction
    suspend fun deleteTransactionAndAdjustBalance(transaction: TransactionEntity) {
        adjustAccountBalance(transaction.accountId, -transaction.amount)
        deleteTransaction(transaction)
    }

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND amount > 0")
    fun getTotalIncome(userId: Int): LiveData<Float?>

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND amount < 0")
    fun getTotalExpenses(userId: Int): LiveData<Float?>

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND amount > 0")
    suspend fun getIncomeForAccount(accountId: Int): Float?

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND amount < 0")
    suspend fun getExpensesForAccount(accountId: Int): Float?

    /**
     * Get transactions for an account within a specific date range as LiveData.
     * This is the function that was missing.
     */
    @Transaction
    @Query(
        "SELECT * FROM transactions WHERE accountId = :accountId AND userId = :userId AND date BETWEEN :start AND :end ORDER BY date DESC"
    )
    fun getTransactionsForAccountInRange(
        accountId: Int,
        userId: Int,
        start: Date,
        end: Date
    ): LiveData<List<TransactionWithAccountAndCategory>>
}

//==================================================================================================
// GOAL DAO
//==================================================================================================
@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY id ASC")
    fun getAllGoals(userId: Int): LiveData<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Int): GoalEntity?

    @Transaction
    suspend fun insertDefaultGoalsForUser(userId: Int): List<Long> {
        val defaultGoals = listOf(
            GoalEntity(GoalName = "Save Money", Description = "Save up to R1500", Amount = 1500, CurrentAmount = 0, Completed = false, Bonus = false, userId = userId),
            GoalEntity(GoalName = "Buy Groceries", Description = "Buy at least R800 groceries", Amount = 800, CurrentAmount = 0, Completed = false, Bonus = false, userId = userId)
        )
        val insertedIds = mutableListOf<Long>()
        defaultGoals.forEach {
            insertedIds.add(insertGoal(it))
        }
        return insertedIds
    }
}

//==================================================================================================
// USER DAO
//==================================================================================================
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :password LIMIT 1")
    suspend fun login(username: String, password: String): UserEntity?

    @Update
     fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}