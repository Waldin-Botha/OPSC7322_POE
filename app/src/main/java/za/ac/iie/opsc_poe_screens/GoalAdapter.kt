package za.ac.iie.opsc_poe_screens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView Adapter for displaying a list of Goals.
 * Each item shows the goal name, progress, and provides edit/delete actions.
 *
 * @param goals Mutable list of GoalEntity objects to display.
 * @param onEdit Callback invoked when the edit button is clicked for a goal.
 * @param onDelete Callback invoked when the delete button is clicked for a goal.
 */
class GoalAdapter(
    private val goals: MutableList<GoalEntity>,
    private val onEdit: (GoalEntity) -> Unit,
    private val onDelete: (GoalEntity) -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    /**
     * ViewHolder class representing each Goal item in the RecyclerView.
     */
    class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGoalName: TextView = view.findViewById(R.id.tvGoalName)
        val pbGoalProgress: ProgressBar = view.findViewById(R.id.pbGoalProgress)
        val tvProgressText: TextView = view.findViewById(R.id.tvProgressText)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditGoal)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteGoal)
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

        holder.tvGoalName.text = goal.GoalName

        // Calculate progress percentage
        val progressPercent = if (goal.Amount > 0) goal.CurrentAmount * 100 / goal.Amount else 0
        holder.pbGoalProgress.progress = progressPercent

        // Display current / target amounts
        holder.tvProgressText.text = "${goal.CurrentAmount} / ${goal.Amount}"

        // Attach click listeners
        holder.btnEdit.setOnClickListener { onEdit(goal) }
        holder.btnDelete.setOnClickListener { onDelete(goal) }
    }

    /**
     * Updates the list of goals and refreshes the RecyclerView.
     */
    fun updateGoals(newList: List<GoalEntity>) {
        goals.clear()
        goals.addAll(newList)
        notifyDataSetChanged()
    }
}
