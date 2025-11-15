package za.ac.iie.opsc_poe_screens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * RecyclerView Adapter for displaying transactions along with their category info.
 * This adapter now uses the 'TransactionDetails' data class.
 *
 * @param transactions List of transactions with associated category details
 * @param onEdit Callback for editing a transaction
 * @param onDelete Callback for deleting a transaction
 */
class TransactionAdapter(
    // --- START OF CHANGES ---
    private var transactions: MutableList<TransactionDetails>,
    private val onEdit: (TransactionDetails) -> Unit,
    private val onDelete: (TransactionDetails) -> Unit
    // --- END OF CHANGES ---
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    /**
     * ViewHolder class for a transaction item.
     * (No changes needed in this inner class)
     */
    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCategory: ImageView = view.findViewById(R.id.ivCategoryIcon)
        val tvCategory: TextView = view.findViewById(R.id.tvCategoryName)
        val tvAmount: TextView = view.findViewById(R.id.tvTransactionAmount)
        val llRoot: View = view.findViewById(R.id.llRoot)
        val tvDate: TextView = view.findViewById(R.id.tvTransactionDate)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteTransaction)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditTransaction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun getItemCount(): Int = transactions.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        // 'txn' is now a 'TransactionDetails' object
        val transactionDetails = transactions[position]

        // Display category icon and name. Fallback to default if null
        // The logic here remains the same as 'TransactionDetails' also has a nullable 'category'
        transactionDetails.category?.let {
            holder.ivCategory.setImageResource(it.iconId)
            holder.tvCategory.text = it.name
        } ?: run {
            holder.ivCategory.setImageResource(android.R.drawable.ic_menu_help)
            holder.tvCategory.text = "Uncategorized"
        }

        // Display transaction amount (accessing the 'transaction' property of our details class)
        holder.tvAmount.text = "R ${"%.2f".format(transactionDetails.transaction.amount)}"

        // Format transaction date
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.tvDate.text = sdf.format(transactionDetails.transaction.date)

        // Set background color based on income/expense
        val bgColor = if (transactionDetails.transaction.amount >= 0) {
            ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_light)
        } else {
            ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_light)
        }
        holder.llRoot.setBackgroundColor(bgColor)

        // Button listeners now pass the 'TransactionDetails' object
        holder.btnDelete.setOnClickListener { onDelete(transactionDetails) }
        holder.btnEdit.setOnClickListener { onEdit(transactionDetails) }
    }

    /**
     * Update the adapter with a new transaction list.
     * (This method signature is the other key change)
     */
    fun updateTransactions(newList: List<TransactionDetails>) {
        transactions.clear()
        transactions.addAll(newList)
        notifyDataSetChanged()
    }
}
