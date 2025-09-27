/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.presentation.formatters;

import bisq.common.locale.LocaleRepository;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateFormatter {
    public static String formatDateTime(long date) {
        return formatDateTime(new Date(date));
    }

    public static String formatDateTime(Date date) {
        return formatDateTime(date, DateFormat.DEFAULT, DateFormat.DEFAULT, true, " ");
    }

    public static String formatDateTime(Date date,
                                        int dateFormat,
                                        int timeFormat,
                                        boolean useLocalTimezone,
                                        String delimiter) {
        if (date == null) {
            return "";
        }
        String formattedDate = formatDate(date, dateFormat, useLocalTimezone);
        String formattedTime = formatTime(date, timeFormat, useLocalTimezone);
        return formattedDate + delimiter + formattedTime;
    }

    public static String formatDateTimeNoSeconds(long date) {
        return formatDateTimeNoSeconds(new Date(date));
    }

    public static String formatDateTimeNoSeconds(Date date) {
        // Use SHORT for time to omit seconds
        return formatDateTime(date, DateFormat.DEFAULT, DateFormat.SHORT, true, ", ");
    }

    public static String formatDate(long date) {
        return formatDate(new Date(date));
    }

    public static String formatDate(Date date) {
        return formatDate(date, DateFormat.DEFAULT, true);
    }

    public static String formatDate(Date date, int dateFormat, boolean useLocalTimezone) {
        if (date == null) {
            return "";
        }

        Locale defaultLocale = LocaleRepository.getDefaultLocale();
        DateFormat dateFormatter = DateFormat.getDateInstance(dateFormat, defaultLocale);
        if (!useLocalTimezone) {
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        return dateFormatter.format(date);
    }

    public static String formatTime(long date) {
        return formatTime(new Date(date));
    }

    public static String formatTime(Date date) {
        return formatTime(date, DateFormat.DEFAULT, true);
    }

    public static String formatTime(Date date, int timeFormat, boolean useLocalTimezone) {
        if (date == null) {
            return "";
        }

        Locale defaultLocale = LocaleRepository.getDefaultLocale();
        DateFormat timeFormatter = DateFormat.getTimeInstance(timeFormat, defaultLocale);
        if (!useLocalTimezone) {
            timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        // Java time formatter returns a U+202F (narrow no-break space) before the AM/PM.
        // We replace it with a normal space as it's not printed by our font.
        return timeFormatter.format(date).replace("\u202F"," ");
    }

    /**
     * Formats a date as "d MMM" if it's in the current year, or "d MMM yyyy" if it's in another year.
     * Example: "1 May" (this year), "1 May 2024" (other years).
     * Uses the default locale and local timezone.
     * @param date the date to format
     * @return formatted date string
     */
    public static String formatDayMonthOrDayMonthYear(Date date) {
        if (date == null) {
            return "";
        }
        Locale defaultLocale = LocaleRepository.getDefaultLocale();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        SimpleDateFormat sdf = year == currentYear
                ? new SimpleDateFormat("d MMM", defaultLocale)
                : new SimpleDateFormat("d MMM yyyy", defaultLocale);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);
    }

    public static String formatDayMonthOrDayMonthYear(long date) {
        return formatDayMonthOrDayMonthYear(new Date(date));
    }
}
