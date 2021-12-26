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

package network.misq.presentation.formatters;

import network.misq.common.locale.LocaleRepository;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateFormatter {
    public static String formatDateTime(Date date) {
        return formatDateTime(date, true);
    }

    public static String formatDateTime(Date date, boolean useLocaleAndLocalTimezone) {
        DateFormat dateInstance = DateFormat.getDateInstance(DateFormat.DEFAULT, LocaleRepository.getDefaultLocale());
        DateFormat timeInstance = DateFormat.getTimeInstance(DateFormat.DEFAULT, LocaleRepository.getDefaultLocale());
        if (!useLocaleAndLocalTimezone) {
            dateInstance.setTimeZone(TimeZone.getTimeZone("UTC"));
            timeInstance.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        return formatDateTime(date, dateInstance, timeInstance);
    }

    public static String formatDateTime(Date date, DateFormat dateFormatter, DateFormat timeFormatter) {
        if (date != null) {
            return dateFormatter.format(date) + " " + timeFormatter.format(date);
        } else {
            return "";
        }
    }
}
