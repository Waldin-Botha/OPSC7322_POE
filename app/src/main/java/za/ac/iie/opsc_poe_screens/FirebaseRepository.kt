package za.ac.iie.opsc_poe_screens

import android.icu.util.Calendar
import android.net.Uri
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.text.format
import kotlin.text.sumOf

class FirebaseRepository {

    // --- SERVICE REFERENCES ---
    // Change Firestore to Realtime Database
    private val db = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference


    companion object {
        fun initPersistence() {
            Firebase.database.setPersistenceEnabled(true)
        }
    }

    // ========================================
    // MANUAL AUTHENTICATION METHODS
    // ========================================

    suspend fun manualSignIn(username: String, password: String): String {
        // Find the user by their username
        val snapshot = db.child("users").orderByChild("username").equalTo(username).get().await()

        if (!snapshot.exists()) {
            throw Exception("Username not found.")
        }
        // Since usernames are unique, there will be only one child
        val userNode = snapshot.children.first()
        val userPassword = userNode.child("password").getValue(String::class.java)

        if (userPassword == password) {
            val userId = userNode.key!! // Get the user's ID

            // This runs AFTER a successful login, so userId is guaranteed to be valid.
            db.child("users").child(userId).child("accounts").keepSynced(true)
            db.child("users").child(userId).child("transactions").keepSynced(true)
            db.child("users").child(userId).child("goals").keepSynced(true)

            return userId // Return the user's ID
        } else {
            throw Exception("Invalid password.")
        }
    }

    suspend fun manualSignUp(username: String, password: String): String {
        // Check if username already exists
        val existingUser = db.child("users").orderByChild("username").equalTo(username).get().await()
        if (existingUser.exists()) {
            throw Exception("Username is already taken.")
        }

        // Generate a new unique ID FIRST
        val newUserId = db.child("users").push().key ?: throw Exception("Could not create user ID.")

        // Create a new user object INCLUDING THE NEW ID
        val newUser = User(
            uid = newUserId,
            username = username,
            password = password
        )

        // Save the complete User object to the database under its new ID
        db.child("users").child(newUserId).setValue(newUser).await()
        return newUserId
    }

    suspend fun updateUserPassword(userId: String, newPassword: String) {
        if (userId.isBlank()) throw Exception("User ID is missing.")
        db.child("users").child(userId).child("password").setValue(newPassword).await()
    }

    suspend fun deleteUserAccount(userId: String) {
        if (userId.isBlank()) throw Exception("User ID is missing.")
        // This will delete the user and all their sub-data (accounts, goals, etc.)
        db.child("users").child(userId).removeValue().await()
    }

    // ========================================
    // USER SETUP & DEFAULT DATA
    // ========================================

    suspend fun createDefaultUserData(userId: String) {
        // In Realtime DB, we set the entire list at once.
        createDefaultAccountsForUser(userId)
        createDefaultCategoriesForUser(userId)
        createDefaultGoalsForUser(userId)
    }

    private suspend fun createDefaultAccountsForUser(userId: String) {
        val defaultAccounts = mutableListOf<Account>()
        val account1Id = db.child("users").child(userId).child("accounts").push().key!!
        val account2Id = db.child("users").child(userId).child("accounts").push().key!!

        defaultAccounts.add(Account(id = account1Id, accountName = "Bank", /*balance = 0.0,*/ colour = android.graphics.Color.parseColor("#388E3C")))
        defaultAccounts.add(Account(id = account2Id, accountName = "Savings", /*balance = 0.0,*/ colour = android.graphics.Color.parseColor("#1976D2")))

        // Set the data as a map of ID -> Account Object
        val accountsMap = defaultAccounts.associateBy { it.id }
        db.child("users").child(userId).child("accounts").setValue(accountsMap).await()
    }

    private suspend fun createDefaultCategoriesForUser(userId: String) {
        val defaultCategories = mutableListOf(
            Category(name = "Salary", iconId = R.drawable.ic_dollar_foreground, isIncome = true),
            Category(name = "Gift", iconId = R.drawable.ic_present_foreground, isIncome = true),
            Category(name = "Investment", iconId = R.drawable.ic_investment_foreground, isIncome = true),
            Category(name = "Other", iconId = R.drawable.ic_more_foreground, isIncome = true),
            Category(name = "Transfer", iconId = R.drawable.ic_transfer_foreground, isIncome = true),
            Category(name = "Groceries", iconId = R.drawable.ic_food_foreground, isIncome = false),
            Category(name = "Transport", iconId = R.drawable.ic_car_foreground, isIncome = false),
            Category(name = "Shopping", iconId = R.drawable.ic_shopping_foreground, isIncome = false),
            Category(name = "Bills", iconId = R.drawable.ic_bills_foreground, isIncome = false),
            Category(name = "Other", iconId = R.drawable.ic_more_foreground, isIncome = false),
            Category(name = "Transfer", iconId = R.drawable.ic_transfer_foreground, isIncome = false)
        )

        // Assign unique IDs before uploading
        val categoriesWithIds = defaultCategories.map {
            it.copy(id = db.child("users").child(userId).child("categories").push().key!!)
        }
        val categoriesMap = categoriesWithIds.associateBy { it.id }
        db.child("users").child(userId).child("categories").setValue(categoriesMap).await()
    }

    private suspend fun createDefaultGoalsForUser(userId: String) {
        val defaultGoals = mutableListOf(
            Goal(goalName = "Save Money", description = "Save up to R1500", amount = 1500.0, currentAmount = 0.0),
            Goal(goalName = "Buy Groceries", description = "Buy at least R800 groceries", amount = 800.0, currentAmount = 0.0)
        )
        val goalsWithIds = defaultGoals.map {
            it.copy(id = db.child("users").child(userId).child("goals").push().key!!)
        }
        val goalsMap = goalsWithIds.associateBy { it.id }
        db.child("users").child(userId).child("goals").setValue(goalsMap).await()
    }

    // ========================================
    // ACCOUNT METHODS
    // ========================================

    suspend fun addAccount(userId: String, account: Account): String {
        val accountsRef = db.child("users").child(userId).child("accounts")
        val newId = accountsRef.push().key!!
        val newAccount = account.copy(id = newId)
        accountsRef.child(newId).setValue(newAccount).await()
        return newId
    }

    suspend fun createAccountAndDefaultGoal(
        userId: String,
        accountName: String,
        initialDeposit: Double,
        maxMonthlySpend: Double,
        colour: Int
    ) {
        val userRef = db.child("users").child(userId)

        // 1. Create the new Account with a starting balance of 0
        val newAccountRef = userRef.child("accounts").push()
        val newAccountId = newAccountRef.key ?: throw Exception("Could not create account ID.")

        val newAccount = Account(
            id = newAccountId,
            accountName = accountName,
            /*balance = 0.0,*/
            colour = colour,
            maxMonthlySpend = maxMonthlySpend
        )
        newAccountRef.setValue(newAccount).await()

        // 2. If the user entered a starting balance, create an "Initial Deposit" transaction
        if (initialDeposit > 0) {
            // Find the "Transfer" category or use a default.
            val transferCategory = getUserCategories(userId).find { it.name == "Transfer" }
            val transferCategoryId = transferCategory?.id ?: "cat_transfer_default"

            val initialTransaction = FinancialTransaction(
                id = "", // The addTransaction function will generate this
                amount = initialDeposit, // Positive amount for the income transaction
                description = "Initial Deposit",
                accountId = newAccountId,
                categoryId = transferCategoryId,
                isRecurring = false,
                date = Date() // Use the current date for the deposit
            )

            addTransaction(userId, initialTransaction)
        }

        // 3. Only create a spending goal if a limit was set
        if (maxMonthlySpend > 0) {
            val newGoalRef = userRef.child("goals").push()
            val newGoalId = newGoalRef.key ?: throw Exception("Could not create goal ID.")
            val monthYearFormat = SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
            val currentMonthYear = monthYearFormat.format(Date())

            val defaultGoal = Goal(
                id = newGoalId,
                goalName = "$accountName Spending",
                description = "Automatic monthly spending limit for $accountName.",
                amount = maxMonthlySpend,
                accountId = newAccountId,
                goalType = GoalType.SPENDING.name,
                monthYear = currentMonthYear,
                currentAmount = 0.0,
                completed = false,
                bonus = false
            )
            newGoalRef.setValue(defaultGoal).await()
        }

        //Process the goals for the new account
        processGoalsForAccount(userId, newAccountId)
    }

    suspend fun getUserAccounts(userId: String): List<Account> {
        val snapshot = db.child("users").child(userId).child("accounts").get().await()
        return snapshot.children.mapNotNull { it.getValue(Account::class.java) }
    }

    suspend fun getAccountById(userId: String, accountId: String): Account? {
        val snapshot = db.child("users").child(userId).child("accounts").child(accountId).get().await()
        return snapshot.getValue(Account::class.java)
    }

    // ========================================
    // TRANSACTION METHODS
    // ========================================

    suspend fun addTransaction(userId: String, transaction: FinancialTransaction): String {
        val transactionsRef = db.child("users").child(userId).child("transactions")
        val newId = transactionsRef.push().key!!
        val newTransaction = transaction.copy(id = newId)
        transactionsRef.child(newId).setValue(newTransaction).await()

        // PROCESS GOALS after adding transaction
        processGoalsForAccount(userId, newTransaction.accountId)

        return newId
    }

    suspend fun getTransactionById(userId: String, transactionId: String): FinancialTransaction? {
        val snapshot = db.child("users").child(userId).child("transactions").child(transactionId).get().await()
        return snapshot.getValue(FinancialTransaction::class.java)
    }

    suspend fun getTransactionsForAccount(userId: String, accountId: String): List<FinancialTransaction> {
        val snapshot = db.child("users").child(userId).child("transactions")
            .orderByChild("accountId").equalTo(accountId).get().await()
        // Manual sort after fetching, as Realtime DB can only order by one key
        return snapshot.children.mapNotNull { it.getValue(FinancialTransaction::class.java) }.sortedByDescending { it.date }
    }

    suspend fun getAllUserTransactions(userId: String): List<FinancialTransaction> {
        val snapshot = db.child("users").child(userId).child("transactions").get().await()
        return snapshot.children.mapNotNull { it.getValue(FinancialTransaction::class.java) }.sortedByDescending { it.date }
    }

    suspend fun updateTransaction(userId: String, transaction: FinancialTransaction) {
        if (transaction.id.isNotBlank()) {
            db.child("users").child(userId).child("transactions").child(transaction.id).setValue(transaction).await()

            // PROCESS GOALS after updating transaction
            processGoalsForAccount(userId, transaction.accountId)

        }
    }

    /**
    * Atomically deletes a transaction and updates the corresponding account's balance.
    * Uses a Realtime Database transaction to ensure data consistency.
    */
    suspend fun deleteTransactionAndUpdateBalance(userId: String, transactionToDelete: FinancialTransaction, account: Account) {
        val userRef = db.child("users").child(userId)

        try {
            // This is a "warm-up" read. It forces the SDK to fetch the latest user data
            // from the server before the transaction starts. This ensures the transaction
            // operates on the freshest data, preventing the "first-try-fails" issue.
            userRef.get().await()
        } catch (e: Exception) {
            throw Exception("Failed to sync with database. Check connection.", e)
        }

        // Use suspendCancellableCoroutine to wrap the callback-based API
        suspendCancellableCoroutine<Boolean> { continuation ->
            userRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
                override fun doTransaction(currentData: MutableData): com.google.firebase.database.Transaction.Result {
                    // 1. Get the current balance from the transaction data
                    val accountNode = currentData.child("accounts").child(account.id)
                    val currentBalance = accountNode.child("balance").getValue(Double::class.java)
                        ?: return com.google.firebase.database.Transaction.abort() // Abort if account doesn't exist

                    // 2. Check if the transaction to be deleted still exists
                    val transactionNode = currentData.child("transactions").child(transactionToDelete.id)
                    if (!transactionNode.hasChildren()) {
                        // Transaction already deleted, nothing to do.
                        return com.google.firebase.database.Transaction.abort()
                    }

                    // 3. Calculate the new balance
                    // Deleting a transaction means reversing its effect.
                    // If amount was -50 (expense), new balance is current + 50.
                    // If amount was +100 (income), new balance is current - 100.
                    val newBalance = currentBalance - transactionToDelete.amount

                    // 4. Update the account balance
                    accountNode.child("balance").value = newBalance

                    // 5. Delete the transaction by setting its node to null
                    transactionNode.value = null

                    // 6. Commit the changes
                    return com.google.firebase.database.Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        // Technical failure
                        continuation.resumeWithException(Exception("Update failed: ${error.message}"))
                    } else if (!committed) {
                        // Transaction was aborted (e.g., account or transaction didn't exist)
                        continuation.resumeWithException(Exception("Update failed: Could not find account or transaction."))
                    } else {
                        // Success
                        continuation.resume(true) { /* Handle cancellation */ }
                    }
                }
            })
        }

        // PROCESS GOALS *after* the suspendCancellableCoroutine has successfully completed.
        // This ensures the database state is consistent before we recalculate.
        processGoalsForAccount(userId, transactionToDelete.accountId)
    }

    suspend fun deleteTransaction(userId: String, transactionId: String) {
        val userRef = db.child("users").child(userId)

        try {
            // This is a "warm-up" read. It forces the SDK to fetch the latest user data
            // from the server before the transaction starts. This ensures the transaction
            // operates on the freshest data, preventing the "first-try-fails" issue.
            userRef.get().await()
        } catch (e: Exception) {
            // If we can't even read the user's data, we can't do a transfer.
            throw Exception("Failed to sync with database. Check connection.", e)
        }

        db.child("users").child(userId).child("transactions").child(transactionId).removeValue().await()
    }


    /**
     * Recalculates all active goals for a specific account based on the transactions of the current month.
     * This should be called whenever a transaction for this account is added, updated, or deleted.
     */
    private suspend fun processGoalsForAccount(userId: String, accountId: String) {
        // 1. Define the current month-year string (e.g., "2025-11")
        val monthYearFormat = SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val currentMonthYear = monthYearFormat.format(Date())

        // 2. Get all goals for the user linked to this account for the current month
        val allGoals = getAllGoals(userId)
        val relevantGoals = allGoals.filter {
            it.accountId == accountId && it.monthYear == currentMonthYear
        }

        if (relevantGoals.isEmpty()) {
            return // Nothing to do
        }

        // 3. Get all transactions for the account that occurred in the current month
        val allTransactionsForAccount = getTransactionsForAccount(userId, accountId)
        val cal = Calendar.getInstance()
        val transactionsForCurrentMonth = allTransactionsForAccount.filter {
            cal.time = it.date
            val transactionMonthYear = monthYearFormat.format(cal.time)
            transactionMonthYear == currentMonthYear
        }

        // 4. Calculate total income and expenses for the month
        val totalIncome = transactionsForCurrentMonth.filter { it.amount > 0 }.sumOf { it.amount }
        val totalExpenses = transactionsForCurrentMonth.filter { it.amount < 0 }.sumOf { abs(it.amount) }

        // 5. Iterate through the relevant goals, update them, and check for completion
        val updatedGoals = mutableListOf<Goal>()
        for (goal in relevantGoals) {
            val updatedGoal = goal.copy() // Create a mutable copy

            when (GoalType.valueOf(goal.goalType)) {
                GoalType.SPENDING -> {
                    updatedGoal.currentAmount = totalExpenses
                    // A spending goal is "complete" (successful) if you are UNDER budget.
                    // We can add a check for the end of the month later if needed.
                    updatedGoal.completed = updatedGoal.currentAmount < updatedGoal.amount
                }
                GoalType.SAVINGS -> {
                    updatedGoal.currentAmount = totalIncome
                    // A savings goal is complete if your income meets or exceeds the target.
                    updatedGoal.completed = updatedGoal.currentAmount >= updatedGoal.amount
                }
            }
            updatedGoals.add(updatedGoal)
        }

        // 6. Save all the updated goals back to the database in a single operation
        val updates = mutableMapOf<String, Any>()
        for (goal in updatedGoals) {
            // We create a map of the updates to send them all at once
            updates["/users/$userId/goals/${goal.id}"] = goal
        }
        // Perform a multi-path update
        db.updateChildren(updates).await()
    }


    // ========================================
    // TRANSFER METHODS
    // ========================================

    suspend fun transferFunds(userId: String, fromAccount: Account, toAccount: Account, amount: Double) {
        val userRef = db.child("users").child(userId)
        val transactionsRef = userRef.child("transactions")

        try {
            // This is a "warm-up" read. It forces the SDK to fetch the latest user data
            // from the server before the transaction starts. This ensures the transaction
            // operates on the freshest data, preventing the "first-try-fails" issue.
            userRef.get().await()
        } catch (e: Exception) {
            // If we can't even read the user's data, we can't do a transfer.
            throw Exception("Failed to sync with database before transfer. Check connection.", e)
        }

        suspendCancellableCoroutine<Boolean> { continuation ->
            userRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
                override fun doTransaction(currentData: MutableData): com.google.firebase.database.Transaction.Result {


                    // Get all transactions from the current state of the database
                    val allTransactionsNode = currentData.child("transactions")
                    val allTransactions = allTransactionsNode.children.mapNotNull { it.getValue(FinancialTransaction::class.java) }

                    // Calculate the live balance for the 'from' account
                    val fromAccountLiveBalance = allTransactions
                        .filter { it.accountId == fromAccount.id }
                        .sumOf { it.amount }

                    // Perform the funds check using the live balance
                    if (fromAccountLiveBalance < amount) {
                        // Abort the transaction if funds are insufficient. This is the cause of the error.
                        return com.google.firebase.database.Transaction.abort()
                    }

                    // Get the category for the transfer
                    val transferCategory = currentData.child("categories").children.find { it.child("name").getValue(String::class.java) == "Transfer" }
                    val transferCategoryId = transferCategory?.key ?: "cat_transfer_default"

                    // Create the two new transfer transactions
                    val expenseId = transactionsRef.push().key!!
                    val incomeId = transactionsRef.push().key!!

                    val expenseTransaction = FinancialTransaction(
                        id = expenseId,
                        amount = -amount,
                        description = "Transfer to ${toAccount.accountName}",
                        accountId = fromAccount.id,
                        categoryId = transferCategoryId,
                        date = Date()
                    )
                    val incomeTransaction = FinancialTransaction(
                        id = incomeId,
                        amount = amount,
                        description = "Transfer from ${fromAccount.accountName}",
                        accountId = toAccount.id,
                        categoryId = transferCategoryId,
                        date = Date()
                    )

                    // Add the new transactions to the database state
                    allTransactionsNode.child(expenseId).value = expenseTransaction
                    allTransactionsNode.child(incomeId).value = incomeTransaction

                    // Commit the changes
                    return com.google.firebase.database.Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        continuation.resumeWithException(Exception("Transfer failed: ${error.message}"))
                    } else if (!committed) {
                        // This is the error you were getting.
                        continuation.resumeWithException(Exception("Transfer failed: Insufficient funds or data contention."))
                    } else {
                        continuation.resume(true) { }
                    }
                }
            })
        }

        // PROCESS GOALS for *both* accounts after the transfer has successfully completed.
        processGoalsForAccount(userId, fromAccount.id)
        processGoalsForAccount(userId, toAccount.id)
    }


    // ========================================
    // CATEGORY METHODS
    // ========================================

    suspend fun getUserCategories(userId: String): List<Category> {
        val snapshot = db.child("users").child(userId).child("categories").get().await()
        return snapshot.children.mapNotNull { it.getValue(Category::class.java) }
    }

    suspend fun addCategory(userId: String, category: Category): String {
        val categoriesRef = db.child("users").child(userId).child("categories")
        val newId = categoriesRef.push().key!!
        val newCategory = category.copy(id = newId)
        categoriesRef.child(newId).setValue(newCategory).await()
        return newId
    }

    suspend fun updateCategory(userId: String, category: Category) {
        db.child("users").child(userId).child("categories").child(category.id).setValue(category).await()
    }

    suspend fun deleteCategory(userId: String, categoryId: String) {
        db.child("users").child(userId).child("categories").child(categoryId).removeValue().await()
    }

    // ========================================
    // GOAL METHODS
    // ========================================

    suspend fun getAllGoals(userId: String): List<Goal> {
        val snapshot = db.child("users").child(userId).child("goals").get().await()
        return snapshot.children.mapNotNull { it.getValue(Goal::class.java) }
    }

    suspend fun addGoal(userId: String, goal: Goal): String {
        val goalsRef = db.child("users").child(userId).child("goals")
        val newId = goalsRef.push().key!!
        val newGoal = goal.copy(id = newId)
        goalsRef.child(newId).setValue(newGoal).await()
        return newId
    }

    suspend fun updateGoal(userId: String, goal: Goal) {
        if (goal.id.isNotBlank()) {
            db.child("users").child(userId).child("goals").child(goal.id).setValue(goal).await()
        }
    }

    suspend fun deleteGoal(userId: String, goalId: String) {
        if (goalId.isNotBlank()) {
            db.child("users").child(userId).child("goals").child(goalId).removeValue().await()
        }
    }

    // ========================================
    // FILE STORAGE METHODS (NO CHANGE NEEDED)
    // ========================================

    suspend fun uploadFile(userId: String, fileUri: Uri): String {
        // This logic uses Firebase Storage and is independent of the database choice.
        val fileRef = storage.child("receipts/$userId/${UUID.randomUUID()}_${fileUri.lastPathSegment}")
        fileRef.putFile(fileUri).await()
        return fileRef.downloadUrl.await().toString()
    }
}