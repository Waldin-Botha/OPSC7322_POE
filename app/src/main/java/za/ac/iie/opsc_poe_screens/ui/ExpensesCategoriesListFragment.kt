package za.ac.iie.opsc_poe_screens.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import za.ac.iie.opsc_poe_screens.*
import za.ac.iie.opsc_poe_screens.databinding.FragmentCategoriesListBinding

class ExpensesCategoriesListFragment : Fragment() {

    private var _binding: FragmentCategoriesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CategoryAdapter
    private lateinit var viewModel: CategoriesViewModel
    private lateinit var currentUserId: String
    private lateinit var repository: FirebaseRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesListBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val userId = UserSession.currentUserId

        // Check if the userId is null (user is not logged in).
        if (userId == null) {
            Toast.makeText(requireContext(), "Your session has expired. Please sign in again.", Toast.LENGTH_LONG).show()

            // Redirect to SignIn and clear the back stack so the user can't come back here
            val intent = Intent(requireContext(), SignIn::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            return root // Stop executing further code in this method
        }

        // If we reach here, userId is valid. Store it in the class variable.
        currentUserId = userId

        // Initialize Repository and ViewModel
        repository = FirebaseRepository()
        val factory = CategoriesViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CategoriesViewModel::class.java]

        setupRecyclerView()
        setupObservers()

        // FAB click to add a new category
        binding.fabAdd.setOnClickListener {
            showEditDialog(null)
        }

        // Trigger initial data load
        viewModel.loadCategories(currentUserId)

        return root
    }

    private fun setupRecyclerView() {
        // You may need to update your CategoryAdapter to use the new `Category` model
        adapter = CategoryAdapter(
            mutableListOf(),
            onEdit = { category -> showEditDialog(category) },
            onDelete = { category -> confirmDelete(category) }
        )
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter
    }

    private fun setupObservers() {
        // Observe expense categories LiveData
        viewModel.expenseCategories.observe(viewLifecycleOwner) { categories ->
            adapter.updateCategories(categories)
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotBlank()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEditDialog(category: Category?) {
        EditCategoryDialog(category, isIncome = false) { updatedCategory ->
            // This is the 'onSave' callback from the dialog.

            // Get the current list of categories from the ViewModel's LiveData
            val allCurrentCategories = viewModel.expenseCategories.value ?: emptyList()

            // Check if another category with the same name exists.
            // We must also check that it's not the same category we are currently editing.
            val duplicateExists = allCurrentCategories.any {
                it.name.equals(updatedCategory.name, ignoreCase = true) && it.id != updatedCategory.id
            }

            if (duplicateExists) {
                Toast.makeText(requireContext(), "A category with this name already exists.", Toast.LENGTH_SHORT).show()
            } else {
                // No duplicate found, proceed to save via the ViewModel.
                if (category == null) {
                    // Adding new category
                    viewModel.addCategory(currentUserId, updatedCategory)
                } else {
                    // Updating existing category
                    viewModel.updateCategory(currentUserId, updatedCategory)
                }
            }
        }.show(parentFragmentManager, "EditCategoryDialog")
    }

    private fun confirmDelete(category: Category) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Fetch all transactions to check if the category is in use
                val allTransactions = repository.getAllUserTransactions(currentUserId)
                val count = allTransactions.count { it.categoryId == category.id }

                if (count > 0) {
                    // Category is in use, show informative toast
                    Toast.makeText(
                        requireContext(),
                        "Cannot delete '${category.name}' – it is used by $count transaction(s).",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Category safe to delete, show delete confirmation dialog
                    // You may need to update DeleteCategoryDialog as well
                    DeleteCategoryDialog(category, viewModel, currentUserId).show(
                        parentFragmentManager,
                        "DeleteCategoryDialog"
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error checking transactions: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
