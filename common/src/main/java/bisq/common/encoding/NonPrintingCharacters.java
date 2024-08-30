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

package bisq.common.encoding;

import lombok.Getter;

public enum NonPrintingCharacters {
    // See: https://en.wikipedia.org/wiki/Delimiter#ASCII_delimited_text
    UNIT_SEPARATOR((char) 0x1f),
    RECORD_SEPARATOR((char) 0x1e),
    FILE_SEPARATOR((char) 0x1c);

    @Getter
    private final char nonPrintingChar;

    NonPrintingCharacters(char nonPrintingChar) {
        this.nonPrintingChar = nonPrintingChar;
    }
}
