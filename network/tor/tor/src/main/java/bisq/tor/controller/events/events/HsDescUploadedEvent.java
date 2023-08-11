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

package bisq.tor.controller.events.events;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class HsDescUploadedEvent {
    private final String hsAddress;
    private final String authType;
    private final String hsDir;

    public static boolean isHsDescMessage(String action) {
        return action.equals("UPLOADED");
    }

    public static HsDescUploadedEvent fromHsDescMessage(String message) {
        String[] items = message.split(" ");
        if (items.length < 4) {
            throw new IllegalStateException("Unknown HS_DESC message: " + message);
        }

        return HsDescUploadedEvent.builder()
                .hsAddress(items[1])
                .authType(items[2])
                .hsDir(items[3])
                .build();
    }
}
