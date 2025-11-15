package za.ac.iie.opsc_poe_screens

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class EditCategoryDialog(
    private val category: Category?,
    private val isIncome: Boolean,
    private val onSave: (Category) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_edit_category, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val ivIcon = view.findViewById<ImageView>(R.id.ivIconPreview)
        val btnChangeIcon = view.findViewById<Button>(R.id.btnChangeIcon)
        val btnSave = view.findViewById<Button>(R.id.btnSaveCategory)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)


        // Create a temporary 'Category' object to hold changes.
        // If we are editing, use the passed-in category.
        // If we are creating a new one, create a default blank object.
        var tempCategory = category ?: Category(
            id = "", // Let Firebase generate the ID
            name = "",
            iconId = R.drawable.ic_gifts_foreground, // A default icon
            isIncome = isIncome
        )

        etName.setText(tempCategory.name)
        ivIcon.setImageResource(tempCategory.iconId)

        btnChangeIcon.setOnClickListener {

            IconPickerDialog { selectedIconId ->
                tempCategory = tempCategory.copy(iconId = selectedIconId)
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

            tempCategory = tempCategory.copy(name = name)

            onSave(tempCategory)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        return dialog
    }
}