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

package bisq.persistence.backup;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.Optional;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public class BackupFileInfo implements Comparable<BackupFileInfo> {
    public static Optional<BackupFileInfo> from(String fileName, String fileNameWithDate) {
        String formattedDate = fileNameWithDate.replace(fileName + "_", "");
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(formattedDate, BackupService.DATE_FORMAT);
            return Optional.of(new BackupFileInfo(localDateTime, fileNameWithDate));
        } catch (Exception e) {
            log.error("Could not resolve date from file {}", fileNameWithDate, e);
            return Optional.empty();
        }
    }

    private final LocalDateTime localDateTime;
    private final String fileNameWithDate;

    public BackupFileInfo(LocalDateTime localDateTime, String fileNameWithDate) {
        this.localDateTime = localDateTime;
        this.fileNameWithDate = fileNameWithDate;
    }

    public LocalDate getLocalDate() {
        return localDateTime.toLocalDate();
    }

    public int getMinutes() {
        return getLocalDateTime().getMinute();
    }

    public int getHour() {
        return getLocalDateTime().getHour();
    }

    public int getDay() {
        return getLocalDate().getDayOfWeek().getValue();
    }

    public int getWeek() {
        return getLocalDate().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    public int getMonth() {
        return getLocalDate().getMonthValue();
    }

    public int getYear() {
        return getLocalDate().getYear();
    }

    // Sort backups by date, most recent first
    @Override
    public int compareTo(BackupFileInfo o) {
        return o.getLocalDateTime().compareTo(localDateTime);
    }
}
