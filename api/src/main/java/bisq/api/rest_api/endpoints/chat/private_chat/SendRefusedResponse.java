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

package bisq.api.rest_api.endpoints.chat.private_chat;

import bisq.api.dto.chat.SendRejectionDto;

/**
 * Body of a 409 from the send endpoints. {@code rejection} says why the send was refused, so a client
 * can act on the reason — say which side is banned — without matching the prose in {@code message},
 * which is free to change.
 */
public record SendRefusedResponse(SendRejectionDto rejection, String message) {
}
