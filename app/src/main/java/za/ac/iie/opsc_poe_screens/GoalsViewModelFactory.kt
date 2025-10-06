package za.ac.iie.opsc_poe_screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// The Factory now needs the userId as well.
class GoalsViewModelFactory(
    private val dao: GoalDao,
    private val userId: Int
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoalsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pass the userId to the ViewModel's constructor.
            return GoalsViewModel(dao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}