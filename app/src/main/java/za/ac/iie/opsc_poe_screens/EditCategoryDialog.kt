package za.ac.iie.opsc_poe_screens

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditCategoryDialog(
    private val category: CategoryEntity?,
    private val isIncome: Boolean,
    private val onSave: (CategoryEntity) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_edit_category, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val ivIcon = view.findViewById<ImageView>(R.id.ivIconPreview)
        val btnChangeIcon = view.findViewById<Button>(R.id.btnChangeIcon)
        val btnSave = view.findViewById<Button>(R.id.btnSaveCategory)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        val database = AppDatabase.getDatabase(requireContext())
        val categoryDao = database.categoryDao()

        var cat = category ?: CategoryEntity(
            id = 0,
            name = "",
            iconId = R.drawable.ic_gifts_foreground,
            isIncome = isIncome,
            userId = UserSession.currentUserId
        )

        etName.setText(cat.name)
        ivIcon.setImageResource(cat.iconId)

        btnChangeIcon.setOnClickListener {
            // Open the IconPickerDialog and get the selected icon
            IconPickerDialog { selectedIconId ->
                cat = cat.copy(iconId = selectedIconId)
                ivIcon.setImageResource(selectedIconId)
            }.show(parentFragmentManager, "icon_picker")
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Launch a coroutine to handle the entire database interaction
            lifecycleScope.launch {
                // 1. Check for a duplicate on a background thread
                val existingCategory = categoryDao.getPossibleCategoryByName(name, UserSession.currentUserId)

                // 2. Perform the check and update the UI from the Main thread
                withContext(Dispatchers.Main) {
                    // Check if a category was found AND that it's not the one we are currently editing
                    if (existingCategory != null && existingCategory.id != cat.id) {
                        Toast.makeText(requireContext(), "A category with this name already exists", Toast.LENGTH_SHORT).show()
                        etName.text.clear()
                    } else {
                        // No duplicate found, proceed to save
                        cat = cat.copy(name = name)

                        // Launch a new coroutine for the update operation
                        lifecycleScope.launch(Dispatchers.IO) {
                            categoryDao.updateCategory(cat)
                        }

                        // Notify the listener and dismiss the dialog
                        onSave(cat)
                        dialog.dismiss()
                    }
                }
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        return dialog
    }
}