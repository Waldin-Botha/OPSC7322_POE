package za.ac.iie.opsc_poe_screens

data class AccountWithTransactions(
    val account: AccountEntity,
    val totalIncome: Float,
    val totalExpenses: Float
)