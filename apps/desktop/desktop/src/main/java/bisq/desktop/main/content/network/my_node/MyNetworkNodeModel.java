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

package bisq.desktop.main.content.network.my_node;

import bisq.desktop.common.view.Model;
import bisq.common.network.TransportType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
@Getter
public class MyNetworkNodeModel implements Model {
    private final Set<TransportType> supportedTransportTypes;

    public MyNetworkNodeModel(Set<TransportType> supportedTransportTypes,
                              boolean clearNetDisabled,
                              boolean torDisabled,
                              boolean i2pDisabled) {
        this.supportedTransportTypes = supportedTransportTypes;
    }
}
