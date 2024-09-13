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

import bisq.common.formatter.SimpleTimeFormatter;
import bisq.i18n.Res;

import java.text.DateFormat;
import java.util.Date;

public class TimeFormatter {
    public static String formatDuration(long duration) {
        return SimpleTimeFormatter.formatDuration(duration);
    }

    public static String getAgeInSeconds(long duration) {
        return SimpleTimeFormatter.getAgeInSeconds(duration);
    }

    public static long getAgeInDays(long date) {
        return SimpleTimeFormatter.getAgeInDays(date);
    }


    public static String formatTime(Date date) {
        return SimpleTimeFormatter.formatTime(date);
    }

    public static String formatTime(Date date, boolean useLocaleAndLocalTimezone) {
        return SimpleTimeFormatter.formatTime(date, useLocaleAndLocalTimezone);
    }

    public static String formatTime(Date date, DateFormat timeFormatter) {
        return SimpleTimeFormatter.formatTime(date, timeFormatter);
    }


    public static String formatVideoDuration(long duration) {
        long sec = duration / 1000;
        long min = sec / 60;
        sec = sec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public static String formatAge(long duration) {
        if (duration < 0) {
            return Res.get("data.na");
        }
        long sec = duration / 1000;
        long min = sec / 60;
        sec = sec % 60;
        long hours = min / 60;
        min = min % 60;
        long days = hours / 24;
        hours = hours % 24;
        if (days > 0) {
            String dayString = Res.getPluralization("temporal.day", days);
            return String.format("%s, %d hours, %d min, %d sec", dayString, hours, min, sec);
        } else if (hours > 0) {
            return String.format("%d hours, %d min, %d sec", hours, min, sec);
        } else {
            return String.format("%d min, %d sec", min, sec);
        }
    }

    public static String formatAgeInDays(long date) {
        long totalDays = getAgeInDays(date);
        long years = totalDays / 365;
        long days = totalDays - years * 365;
        String dayString = Res.getPluralization("temporal.day", days);
        if (years > 0) {
            String yearString = Res.getPluralization("temporal.year", years);
            return yearString + ", " + dayString;
        } else {
            return dayString;
        }
    }
}
