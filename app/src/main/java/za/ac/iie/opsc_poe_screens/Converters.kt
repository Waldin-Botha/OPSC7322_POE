package za.ac.iie.opsc_poe_screens

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room TypeConverters for converting between Date and Long (timestamp).
 *
 * Room does not support storing Date objects directly,
 * so these converters transform Date to Long and vice versa.
 */
class Converters {

    /**
     * Converts a timestamp (Long) from the database into a Date object.
     *
     * @param value The timestamp in milliseconds
     * @return A Date object or null if value is null
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Converts a Date object into a timestamp (Long) for storage in the database.
     *
     * @param date The Date object
     * @return The timestamp in milliseconds or null if date is null
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
