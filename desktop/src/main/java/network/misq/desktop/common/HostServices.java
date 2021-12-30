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

package network.misq.desktop.common;

import lombok.NonNull;
import network.misq.common.annotations.LateInit;

import static java.util.Objects.requireNonNull;

public class HostServices {
    @LateInit
    private static javafx.application.HostServices delegate;

    public static void init(@NonNull javafx.application.HostServices hostServices) {
        HostServices.delegate = hostServices;
    }

    public static void openUri(String uri) {
        requireNonNull(delegate).showDocument(uri);
    }
}