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

package bisq.desktop.main.content.settings.network.transport;

import bisq.desktop.common.view.Model;
import bisq.network.common.TransportType;
import bisq.network.p2p.node.Node;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class TransportModel implements Model {
    private final TransportType transportType;
    private final Node defaultNode;
    @Setter
    private String myDefaultNodeAddress;

    public TransportModel(TransportType transportType, Node defaultNode) {
        this.transportType = transportType;
        this.defaultNode = defaultNode;
    }
}