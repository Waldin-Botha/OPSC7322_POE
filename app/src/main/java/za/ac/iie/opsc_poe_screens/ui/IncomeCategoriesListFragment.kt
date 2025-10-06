package za.ac.iie.opsc_poe_screens.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.ac.iie.opsc_poe_screens.*
import za.ac.iie.opsc_poe_screens.databinding.FragmentCategoriesListBinding

/**
 * Fragment for displaying and managing income categories.
 * Allows adding, editing, and deleting categories.
 * Deletion is prevented if the category is used in any transactions,
 * also prevents adding a category name that already exists. (Happens on the EditCategoryDialog)
 */
class CategoriesListFragment : Fragment() {

    // View binding for the fragment layout
    private var _binding: FragmentCategoriesListBinding? = null
    private val binding get() = _binding!!

    // RecyclerView adapter and ViewModel for categories
    private lateinit var adapter: CategoryAdapter
    private lateinit var viewModel: CategoriesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using ViewBinding
        _binding = FragmentCategoriesListBinding.inflate(inflater, container, false)

        // Setup ViewModel with DAO
        val dao = AppDatabase.getDatabase(requireContext()).categoryDao()
        val factory = CategoriesViewModelFactory(dao)
        viewModel = ViewModelProvider(this, factory)[CategoriesViewModel::class.java]

        // Setup RecyclerView adapter with edit and delete callbacks
        adapter = CategoryAdapter(
            mutableListOf(),
            onEdit = { category -> showEditDialog(category) },
            onDelete = { category -> confirmDelete(category) }
        )

        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter

        // Floating action button to add a new category
        binding.fabAdd.setOnClickListener {
            showEditDialog(null) // null = new category
        }

        // Observe income categories from the database and update the adapter
        viewModel.incomeCategories.observe(viewLifecycleOwner) { categories ->
            adapter.updateCategories(categories)
        }

        return binding.root
    }

    /**
     * Opens the edit dialog for adding or editing a category.
     * @param category The category to edit, or null to add a new one.
     */
    private fun showEditDialog(category: CategoryEntity?) {
        EditCategoryDialog(category, isIncome = true) { updatedCategory ->
            if (category == null) {
                // Adding a new category
                viewModel.addCategory(updatedCategory)
            } else {
                // Updating an existing category (preserve ID)
                viewModel.updateCategory(updatedCategory.copy(id = category.id))
            }
        }.show(parentFragmentManager, "EditCategoryDialog")
    }

    /**
     * Confirms deletion of a category.
     * Prevents deletion if category is used in any transactions.
     */
    private fun confirmDelete(category: CategoryEntity) {
        val categoryDao = AppDatabase.getDatabase(requireContext()).categoryDao()

        viewLifecycleOwner.lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) {
                categoryDao.getTransactionCountForCategory(category.id)
            }

            if (count > 0) {
                // Category is in use, show informative toast
                Toast.makeText(
                    requireContext(),
                    "Cannot delete '${category.name}' – it is used by $count transaction(s).",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Category safe to delete, show delete confirmation dialog
                DeleteCategoryDialog(category, viewModel).show(
                    parentFragmentManager,
                    "DeleteCategoryDialog"
                )
            }
        }
    }

    override fun onDestroyView() {
        // Avoid memory leaks
        _binding = null
        super.onDestroyView()
    }
}
