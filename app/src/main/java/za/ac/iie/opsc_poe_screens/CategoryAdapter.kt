package za.ac.iie.opsc_poe_screens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for displaying categories.
 *
 * @param categories List of categories to display
 * @param onEdit Callback when a category item is clicked for editing
 * @param onDelete Callback when the delete button is clicked
 */
class CategoryAdapter(
    private var categories: MutableList<Category>,
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    /**
     * ViewHolder for a category item
     */
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgIcon)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteTransaction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val category = categories[position]

        // Set category icon and name
        holder.imgIcon.setImageResource(category.iconId)
        holder.tvName.text = category.name

        // Handle click on the item to edit
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onEdit(categories[pos])
        }

        // Handle delete button click
        holder.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                val categoryToDelete = categories[pos]
                if (categoryToDelete.name == "Transfer"){
                    val context = holder.itemView.context
                    Toast.makeText(context, "Cannot delete Transfer category", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                onDelete(categoryToDelete)
            }
        }
    }

    override fun getItemCount(): Int = categories.size

    /**
     * Updates the categories list and refreshes the RecyclerView.
     *
     * @param newCategories List of new categories
     */
    fun updateCategories(newCategories: List<Category>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }
}
