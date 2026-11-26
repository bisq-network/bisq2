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

package bisq.desktop_app;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * What the popup of a failed start which a change of bisq.conf fixes says, and which changes it offers.
 *
 * @param choices at most two, because the popup has two buttons for them next to the button which only shuts down
 */
record StartupFailurePopupContent(String headline, String message, List<Choice> choices) {
    StartupFailurePopupContent {
        checkArgument(choices.size() <= 2, "The popup offers at most two choices, not %s", choices.size());
        choices = List.copyOf(choices);
    }

    /**
     * @param configEntries what Bisq appends to bisq.conf before it shuts down
     */
    record Choice(String buttonText, Map<String, Object> configEntries) {
        Choice {
            configEntries = Map.copyOf(configEntries);
        }
    }
}
