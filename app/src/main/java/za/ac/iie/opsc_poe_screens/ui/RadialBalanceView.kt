package za.ac.iie.opsc_poe_screens.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.min

/**
 * Custom view that displays a radial balance chart showing income vs. expenses.
 * - A background ring shows the full circle.
 * - Colors for income and expense arcs can be set dynamically.
 */
class RadialBalanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#555555") // A medium-dark grey
        style = Paint.Style.STROKE
        strokeWidth = 40f
    }

    private val paintIncome = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 40f
        strokeCap = Paint.Cap.ROUND
        // Set a default color which will be overridden
        color = ContextCompat.getColor(context, android.R.color.holo_green_light)
    }

    private val paintExpense = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 40f
        strokeCap = Paint.Cap.ROUND
        // Set a default color which will be overridden
        color = ContextCompat.getColor(context, android.R.color.holo_red_light)
    }

    private var incomeAngle: Float = 0f
    private var expenseAngle: Float = 0f

    /**
     * Updates the view with the latest income and expense values.
     * @param income Amount of income.
     * @param expense Amount of expense (as a positive value).
     */
    fun setBalances(income: Float, expense: Float) {
        val total = income + expense
        if (total > 0) {
            incomeAngle = (income / total) * 360f
            expenseAngle = (expense / total) * 360f
        } else {
            incomeAngle = 0f
            expenseAngle = 0f
        }
        // Request the view to redraw itself with the new angles
        invalidate()
    }

    /**
     * Sets the colors for the income and expense arcs.
     * @param incomeColor The color for the income portion of the arc.
     * @param expenseColor The color for the expense portion of the arc.
     */
    fun setArcColors(incomeColor: Int, expenseColor: Int) {
        paintIncome.color = incomeColor
        paintExpense.color = expenseColor
        invalidate() // Redraw the view with the new colors
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (min(width, height) / 2f) - (backgroundPaint.strokeWidth / 2f)

        val oval = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // 1. Draw the background ring first
        canvas.drawArc(oval, 0f, 360f, false, backgroundPaint)

        // 2. Draw income arc (starting at top (-90 degrees), clockwise)
        if (incomeAngle > 0) {
            canvas.drawArc(oval, -90f, incomeAngle, false, paintIncome)
        }

        // 3. Draw expense arc immediately after the income arc
        if (expenseAngle > 0) {
            canvas.drawArc(oval, -90f + incomeAngle, expenseAngle, false, paintExpense)
        }
    }
}