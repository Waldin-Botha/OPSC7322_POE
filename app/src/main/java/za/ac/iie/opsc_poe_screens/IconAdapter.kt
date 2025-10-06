package za.ac.iie.opsc_poe_screens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView Adapter for displaying a list of icons.
 * Handles click events on individual icons via a callback.
 *
 * @param icons List of drawable resource IDs representing the icons.
 * @param onClick Callback invoked when an icon is clicked, passing the selected icon ID.
 */
class IconAdapter(
    private val icons: List<Int>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<IconAdapter.IconViewHolder>() {

    /**
     * ViewHolder representing a single icon item.
     */
    inner class IconViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val iconId = icons[position]

        // Set icon image
        holder.ivIcon.setImageResource(iconId)

        // Handle click on the entire item
        holder.view.setOnClickListener { onClick(iconId) }
    }

    override fun getItemCount(): Int = icons.size
}
