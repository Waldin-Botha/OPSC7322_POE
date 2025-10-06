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
 * RecyclerView Adapter for displaying transactions along with their account and category info.
 *
 * @param transactions List of transactions with associated account and category details
 * @param onEdit Callback for editing a transaction
 * @param onDelete Callback for deleting a transaction
 */
class TransactionAdapter(
    private var transactions: MutableList<TransactionWithAccountAndCategory>,
    private val onEdit: (TransactionWithAccountAndCategory) -> Unit,
    private val onDelete: (TransactionWithAccountAndCategory) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    /**
     * ViewHolder class for a transaction item.
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
        val txn = transactions[position]

        // Display category icon and name. Fallback to default if null
        txn.category?.let {
            holder.ivCategory.setImageResource(it.iconId)
            holder.tvCategory.text = it.name
        } ?: run {
            holder.ivCategory.setImageResource(android.R.drawable.ic_menu_help)
            holder.tvCategory.text = "Uncategorized"
        }

        // Display transaction amount
        holder.tvAmount.text = "R ${txn.transaction.amount}"

        // Format transaction date
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        holder.tvDate.text = sdf.format(txn.transaction.date)

        // Set background color based on income/expense
        val bgColor = if (txn.transaction.amount >= 0) {
            ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_light)
        } else {
            ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_light)
        }
        holder.llRoot.setBackgroundColor(bgColor)

        // Button listeners
        holder.btnDelete.setOnClickListener { onDelete(txn) }
        holder.btnEdit.setOnClickListener { onEdit(txn) }
    }

    /**
     * Remove a specific transaction from the list and notify the RecyclerView.
     */
    fun removeTransaction(txn: TransactionWithAccountAndCategory) {
        val position = transactions.indexOf(txn)
        if (position != -1) {
            transactions.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    /**
     * Update the adapter with a new transaction list.
     */
    fun updateTransactions(newList: List<TransactionWithAccountAndCategory>) {
        transactions.clear()
        transactions.addAll(newList)
        notifyDataSetChanged()
    }
}
