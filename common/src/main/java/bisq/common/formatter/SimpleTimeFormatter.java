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

package bisq.common.formatter;

import bisq.common.locale.LocaleRepository;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class SimpleTimeFormatter {
    public static final long DAY_AS_MS = TimeUnit.DAYS.toMillis(1);

    public static String formatDuration(long duration) {
        if (duration < 1000) {
            return duration + " ms";
        } else {
            long ms, sec, min;
            ms = duration;
            sec = ms / 1000;
            ms = ms % 1000;

            min = sec / 60;
            sec = sec % 60;

            String msString = ms > 0 ? ms + " ms" : "";
            String secString = sec > 0 || min > 0 ? sec + " sec; " : "";
            String minString = min > 0 ? min + " min; " : "";
            return minString + secString + msString;
        }
    }

    public static String getAgeInSeconds(long duration) {
        long sec = duration / 1000;
        return sec + " sec";
    }

    public static long getAgeInDays(long date) {
        return (System.currentTimeMillis() - date) / DAY_AS_MS;
    }

    public static String formatTime(Date date) {
        return formatTime(date, true);
    }

    public static String formatTime(Date date, boolean useLocaleAndLocalTimezone) {
        DateFormat timeInstance = DateFormat.getTimeInstance(DateFormat.SHORT, LocaleRepository.getDefaultLocale());
        if (!useLocaleAndLocalTimezone) {
            timeInstance.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        return formatTime(date, timeInstance);
    }

    public static String formatTime(Date date, DateFormat timeFormatter) {
        if (date != null) {
            return timeFormatter.format(date);
        } else {
            return "";
        }
    }
}
