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
package bisq.api.dto.mappings.chat.two_party;

import bisq.api.dto.chat.SendRejectionDto;
import bisq.chat.priv.SendRejection;

public class SendRejectionDtoMapping {
    // toBisq2Model not provided: nothing consumes it, matching the sibling mappings in this package.

    /**
     * An explicit switch rather than {@code valueOf(value.name())}: the dto is the wire contract, and
     * a renamed domain constant must fail to compile here rather than change what clients receive.
     */
    public static SendRejectionDto fromBisq2Model(SendRejection value) {
        return switch (value) {
            case MY_PROFILE_BANNED -> SendRejectionDto.MY_PROFILE_BANNED;
            case PEER_BANNED -> SendRejectionDto.PEER_BANNED;
        };
    }
}
