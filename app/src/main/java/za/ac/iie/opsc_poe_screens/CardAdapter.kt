package za.ac.iie.opsc_poe_screens

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import za.ac.iie.opsc_poe_screens.databinding.CardItemBinding
import kotlin.math.abs

class CardAdapter(
    private var cards: MutableList<AccountWithTransactions>,
    private val onAddClick: () -> Unit,
    private val onCardClick: (AccountWithTransactions) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    inner class CardViewHolder(val binding: CardItemBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val TYPE_CARD = 0
        private const val TYPE_ADD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == cards.size) TYPE_ADD else TYPE_CARD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = CardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardViewHolder(binding)
    }

    override fun getItemCount(): Int = cards.size + 1 // +1 for Add button

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_CARD) {
            val accountWithTransactions = cards[position]
            val account = accountWithTransactions.account

            val context = holder.itemView.context
            val baseColor = try {
                ContextCompat.getColor(context, account.Colour)
            } catch (e: Exception) {
                account.Colour
            }
            val darkerColor = createDarkerColor(baseColor, 0.7f)


            val income = accountWithTransactions.totalIncome

            val totalExpenses = accountWithTransactions.totalExpenses


            val balance = account.Balance

            // Set card border color
            val bgDrawable = holder.binding.cardInnerLayout.background.mutate() as android.graphics.drawable.GradientDrawable
            bgDrawable.setStroke(5, baseColor)
            holder.binding.cardInnerLayout.background = bgDrawable

            // Apply text and radial colors
            holder.binding.cardTitle.text = account.AccountName
            holder.binding.cardBalance.text = String.format("R%.2f", balance)
            holder.binding.cardTitle.setTextColor(baseColor)
            holder.binding.cardBalance.setTextColor(baseColor)

            // Radial view shows the *flow* of money, so we use the absolute value of expenses for the visual
            holder.binding.radialBalanceView.setArcColors(baseColor, darkerColor)
            holder.binding.radialBalanceView.setBalances(income, abs(totalExpenses))

            // Manage visibility
            holder.binding.addIcon.visibility = View.GONE
            holder.binding.radialBalanceView.visibility = View.VISIBLE
            holder.binding.cardBalance.visibility = View.VISIBLE
            holder.binding.cardTitle.visibility = View.VISIBLE

            holder.itemView.setOnClickListener { onCardClick(accountWithTransactions) }

        } else {
            // "Add Account" card
            val context = holder.itemView.context
            val addColor = ContextCompat.getColor(context, R.color.light_grey)
            val bgDrawable = holder.binding.cardInnerLayout.background.mutate() as android.graphics.drawable.GradientDrawable
            bgDrawable.setStroke(3, addColor)
            holder.binding.cardInnerLayout.background = bgDrawable

            holder.binding.cardTitle.visibility = View.VISIBLE
            holder.binding.cardTitle.text = "Add Account"
            holder.binding.cardTitle.setTextColor(addColor)

            holder.binding.addIcon.visibility = View.VISIBLE
            holder.binding.addIcon.setColorFilter(addColor)

            // Set balance text to be empty for the "Add" card
            holder.binding.cardBalance.visibility = View.VISIBLE
            holder.binding.cardBalance.text = ""
            holder.binding.cardBalance.setTextColor(Color.BLACK)

            holder.binding.radialBalanceView.visibility = View.GONE

            holder.itemView.setOnClickListener { onAddClick() }
        }
    }

    private fun createDarkerColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    fun updateCards(newCards: List<AccountWithTransactions>) {
        cards.clear()
        cards.addAll(newCards)
        notifyDataSetChanged()
    }
}