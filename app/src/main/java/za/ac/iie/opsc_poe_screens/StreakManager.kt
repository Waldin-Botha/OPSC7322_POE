package za.ac.iie.opsc_poe_screens

import android.content.Context
import java.util.Calendar

object StreakManager {

    private const val PREFS_NAME = "StreakPrefs"
    private const val KEY_STREAK_COUNT = "streak_count"
    private const val KEY_LAST_LOGIN_DAY = "last_login_day"
    private const val KEY_LAST_LOGIN_YEAR = "last_login_year"

    /**
     * Call this method immediately after a successful user login.
     * It checks the last login date and updates the streak accordingly.
     */
    fun updateUserStreak(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val today = Calendar.getInstance()
        val currentDayOfYear = today.get(Calendar.DAY_OF_YEAR)
        val currentYear = today.get(Calendar.YEAR)

        val lastLoginDay = prefs.getInt(KEY_LAST_LOGIN_DAY, -1)
        val lastLoginYear = prefs.getInt(KEY_LAST_LOGIN_YEAR, -1)
        var currentStreak = prefs.getInt(KEY_STREAK_COUNT, 0)

        if (lastLoginDay == -1 || lastLoginYear == -1) {
            // First login ever for this user
            currentStreak = 1
        } else {
            val yesterday = Calendar.getInstance()
            yesterday.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayDayOfYear = yesterday.get(Calendar.DAY_OF_YEAR)
            val yesterdayYear = yesterday.get(Calendar.YEAR)

            if (lastLoginYear == currentYear && lastLoginDay == currentDayOfYear) {
                // User already logged in today, do nothing to the streak.
            } else if (lastLoginYear == yesterdayYear && lastLoginDay == yesterdayDayOfYear) {
                // Consecutive day login, increment streak.
                currentStreak++
            } else {
                // The streak is broken, reset to 1.
                currentStreak = 1
            }
        }

        // Save the updated streak and today's date
        editor.putInt(KEY_STREAK_COUNT, currentStreak)
        editor.putInt(KEY_LAST_LOGIN_DAY, currentDayOfYear)
        editor.putInt(KEY_LAST_LOGIN_YEAR, currentYear)
        editor.apply()
    }

    /**
     * Retrieves the current streak count.
     */
    fun getStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_STREAK_COUNT, 0)
    }
}