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

package bisq.desktop.common.utils;

import bisq.network.NetworkService;

import java.util.Optional;

public class SendMessageResultUtil {

    public static Optional<String> findAnyErrorMsg(NetworkService.SendMessageResult result) {
        return result.entrySet().stream()
                .filter(e -> e.getValue().getErrorMsg().isPresent())
                .map(e -> e.getKey().name() + ": " + e.getValue().getErrorMsg().get())
                .findAny();
    }
}