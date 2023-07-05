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

package bisq.desktop.components.controls;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j

public class UnorderedList extends TextList {
    private static final String REGEX = "- ";
    private static final String MARK = "•";

    public UnorderedList(String text, String style, String regex, String mark) {
        this(text, style, 7, 0, regex, mark);
    }

    public UnorderedList(String text, String style) {
        this(text, style, 7, 0, REGEX, MARK);
    }

    public UnorderedList(String text, @Nullable String style, double gap, double vSpacing, String regex, String mark) {
        super(text, style, gap, vSpacing, regex, mark);
    }
}