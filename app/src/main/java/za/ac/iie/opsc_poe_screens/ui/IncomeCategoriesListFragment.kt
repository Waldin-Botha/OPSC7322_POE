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

/**
 * Fragment for displaying and managing income categories using Firebase.
 */
class IncomeCategoriesListFragment : Fragment() {

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

        // Initialize Repository and ViewModel using the new Firebase-aware factory
        repository = FirebaseRepository()
        val factory = CategoriesViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CategoriesViewModel::class.java]

        setupRecyclerView()
        setupObservers()

        // FAB click to add a new category (for income)
        binding.fabAdd.setOnClickListener {
            showEditDialog(null)
        }

        // Trigger the initial data load from the ViewModel
        viewModel.loadCategories(currentUserId)

        return root
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter(
            mutableListOf(),
            onEdit = { category -> showEditDialog(category) },
            onDelete = { category -> confirmDelete(category) }
        )
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.incomeCategories.observe(viewLifecycleOwner) { categories ->
            adapter.updateCategories(categories)
        }

        // Observe for any errors from the ViewModel
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotBlank()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEditDialog(category: Category?) {
        // We pass 'isIncome = true' to the dialog
        EditCategoryDialog(category, isIncome = true) { updatedCategory ->
            // The duplicate check logic is handled in the callback
            val allCurrentCategories = viewModel.incomeCategories.value ?: emptyList()
            val duplicateExists = allCurrentCategories.any {
                it.name.equals(updatedCategory.name, ignoreCase = true) && it.id != updatedCategory.id
            }

            if (duplicateExists) {
                Toast.makeText(requireContext(), "An income category with this name already exists.", Toast.LENGTH_SHORT).show()
            } else {
                // No duplicate, proceed to save via ViewModel
                if (category == null) {
                    viewModel.addCategory(currentUserId, updatedCategory)
                } else {
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
                    // Category is safe to delete, show the confirmation dialog
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
        super.onDestroyView()
        _binding = null
    }
}
