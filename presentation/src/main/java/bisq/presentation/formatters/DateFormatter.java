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
        return timeFormatter.format(date);
    }
}
