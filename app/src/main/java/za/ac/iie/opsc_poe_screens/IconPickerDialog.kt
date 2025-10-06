package za.ac.iie.opsc_poe_screens

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class IconPickerDialog (
    private val onPicked: (Int) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_icon_picker, null)
        val rvIcons = view.findViewById<RecyclerView>(R.id.rvIcons)

        rvIcons.layoutManager = GridLayoutManager(requireContext(), 3)
        rvIcons.adapter = IconAdapter(Icons.icons) { iconId ->
            onPicked(iconId)
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }
}