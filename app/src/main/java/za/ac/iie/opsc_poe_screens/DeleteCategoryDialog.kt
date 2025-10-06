package za.ac.iie.opsc_poe_screens

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment

/**
 * Dialog to confirm deletion of a category.
 * Uses CategoriesViewModel to perform the deletion.
 */
class DeleteCategoryDialog(
    private val category: CategoryEntity,
    private val viewModel: CategoriesViewModel
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_delete_category, null)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvCategoryName = view.findViewById<TextView>(R.id.tvCategoryName)
        val btnDeleteCategory = view.findViewById<Button>(R.id.btnSaveCategory)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        // Set dialog texts
        tvTitle.text = "Delete Category"
        tvCategoryName.text = "Are you sure you want to delete '${category.name}'?"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        // Delete button
        btnDeleteCategory.setOnClickListener {
            try {
                viewModel.deleteCategory(category)
                Toast.makeText(requireContext(), "Category deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to delete category", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        // Cancel button
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }
}
