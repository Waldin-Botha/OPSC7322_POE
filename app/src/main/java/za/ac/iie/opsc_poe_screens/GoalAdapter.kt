package za.ac.iie.opsc_poe_screens

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView Adapter for displaying a list of Goals from Firebase.
 * Each item shows the goal name, progress, and provides edit/delete actions.
 *
 * @param goals Mutable list of Goal objects to display.
 * @param onEdit Callback invoked when the edit button is clicked for a goal.
 * @param onDelete Callback invoked when the delete button is clicked for a goal.
 */
class GoalAdapter(
    private val goals: MutableList<Goal>,
    private val onEdit: (Goal) -> Unit,
    private val onDelete: (Goal) -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    /**
     * ViewHolder class representing each Goal item in the RecyclerView.
     * (No changes needed in this inner class)
     */
    class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGoalName: TextView = view.findViewById(R.id.tvGoalName)
        val pbGoalProgress: ProgressBar = view.findViewById(R.id.pbGoalProgress)
        val tvProgressText: TextView = view.findViewById(R.id.tvProgressText)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditGoal)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteGoal)
        val ivBonusGoal : ImageView = view.findViewById(R.id.ivBonusIndicator)

        val ivCompleted : ImageView = view.findViewById(R.id.ivCompletedIndicator)
    }

    /**
     * Inflates the layout for each goal item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.goal_item, parent, false)
        return GoalViewHolder(view)
    }

    /**
     * Returns the total number of goals in the list.
     */
    override fun getItemCount(): Int = goals.size

    /**
     * Binds goal data to each ViewHolder.
     * Displays goal name, progress, and attaches click listeners for edit/delete actions.
     */
    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]


        // Use the new property names from the Firebase 'Goal' data class
        holder.tvGoalName.text = goal.goalName

        if (goal.bonus) {
            holder.ivBonusGoal.visibility = View.VISIBLE
        } else {
            holder.ivBonusGoal.visibility = View.GONE
        }

        // Calculate progress percentage (using Double for more precision)
        val progressPercent = if (goal.amount > 0) {
            (goal.currentAmount * 100 / goal.amount).toInt()
        } else {
            0
        }
        holder.pbGoalProgress.progress = progressPercent

        // Display current / target amounts
        holder.tvProgressText.text = "${goal.currentAmount.toInt()} / ${goal.amount.toInt()}"


        holder.ivCompleted.visibility = View.VISIBLE

        if (goal.completed) {
            // Goal is completed: Set tint to GREEN
            holder.ivCompleted.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark),
                PorterDuff.Mode.SRC_IN
            )
        } else {
            // Goal is not completed: Set tint to GRAY
            holder.ivCompleted.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, R.color.light_grey),
                PorterDuff.Mode.SRC_IN
            )
        }

        // Attach click listeners
        holder.btnEdit.setOnClickListener { onEdit(goal) }
        holder.btnDelete.setOnClickListener { onDelete(goal) }
    }

    /**
     * Updates the list of goals and refreshes the RecyclerView.
     */
    fun updateGoals(newList: List<Goal>) {
        goals.clear()
        goals.addAll(newList)
        notifyDataSetChanged()
    }
}
