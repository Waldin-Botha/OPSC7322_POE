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
 * Uses the CategoriesViewModel to perform the deletion.
 */
class DeleteCategoryDialog(
    private val category: Category,
    private val viewModel: CategoriesViewModel,
    private val userId: String? // --- CHANGE: Make userId nullable
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_delete_category, null)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvCategoryName = view.findViewById<TextView>(R.id.tvCategoryName)
        val btnDeleteCategory = view.findViewById<Button>(R.id.btnSaveCategory)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        tvTitle.text = "Delete Category"
        tvCategoryName.text = "Are you sure you want to delete '${category.name}'?"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        btnDeleteCategory.setOnClickListener {
            // Ensure we have a valid userId before trying to delete.
            if (userId.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Cannot delete: User session is invalid.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                return@setOnClickListener
            }

            // This call is now safe.
            viewModel.deleteCategory(userId, category.id)

            Toast.makeText(requireContext(), "'${category.name}' deleted", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }
}