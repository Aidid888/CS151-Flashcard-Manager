package cs151.application.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for converting between UTC and local time for display purposes.
 * Database stores all timestamps in UTC. This class converts them to local time
 * for the user interface.
 */
public class DateTimeUtil {

    private static final DateTimeFormatter DB_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Converts a UTC timestamp from the database to local time for display.
     *
     * @param utcTimestamp timestamp in UTC format "yyyy-MM-dd HH:mm:ss"
     * @return timestamp converted to local time, or original string if parsing fails
     */
    public static String utcToLocal(String utcTimestamp) {
        if (utcTimestamp == null || utcTimestamp.isEmpty()) {
            return utcTimestamp;
        }

        try {
            // Parse as UTC
            LocalDateTime utcDateTime = LocalDateTime.parse(utcTimestamp, DB_FORMAT);
            ZonedDateTime utcZoned = utcDateTime.atZone(ZoneId.of("UTC"));

            // Convert to local timezone
            ZonedDateTime localZoned = utcZoned.withZoneSameInstant(ZoneId.systemDefault());

            // Format for display
            return localZoned.format(DISPLAY_FORMAT);
        } catch (DateTimeParseException e) {
            // If parsing fails, return original string
            return utcTimestamp;
        }
    }

    /**
     * Converts a local time input to UTC for database storage.
     *
     * @param localTimestamp timestamp in local time format "yyyy-MM-dd HH:mm:ss"
     * @return timestamp converted to UTC, or original string if parsing fails
     */
    public static String localToUtc(String localTimestamp) {
        if (localTimestamp == null || localTimestamp.isEmpty()) {
            return localTimestamp;
        }

        try {
            // Parse as local time
            LocalDateTime localDateTime = LocalDateTime.parse(localTimestamp, DISPLAY_FORMAT);
            ZonedDateTime localZoned = localDateTime.atZone(ZoneId.systemDefault());

            // Convert to UTC
            ZonedDateTime utcZoned = localZoned.withZoneSameInstant(ZoneId.of("UTC"));

            // Format for database
            return utcZoned.format(DB_FORMAT);
        } catch (DateTimeParseException e) {
            // If parsing fails, return original string
            return localTimestamp;
        }
    }
}