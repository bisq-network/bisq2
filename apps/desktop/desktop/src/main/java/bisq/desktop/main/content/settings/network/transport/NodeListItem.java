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

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.table.ActivatableTableItem;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.common.Address;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.security.keys.KeyBundleService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
public class NodeListItem implements ActivatableTableItem {
    @EqualsAndHashCode.Include
    @Getter
    private final Node node;
    @Getter
    private final StringProperty date = new SimpleStringProperty();
    @Getter
    private final String address, keyId, type, nodeTag, nodeTagTooltip;
    @Getter
    private final StringProperty numConnections = new SimpleStringProperty();
    private final Node.Listener listener;

    public NodeListItem(Node node, KeyBundleService keyBundleService, IdentityService identityService) {
        this.node = node;
        keyId = node.getNetworkId().getKeyId();
        type = identityService.findActiveIdentity(node.getNetworkId())
                .map(i -> Res.get("settings.network.nodes.type.active"))
                .or(() -> identityService.findRetiredIdentityByNetworkId(node.getNetworkId())
                        .map(i -> Res.get("settings.network.nodes.type.retired")))
                .orElseGet(() -> keyBundleService.isDefaultKeyId(node.getNetworkId().getKeyId()) ?
                        Res.get("settings.network.nodes.type.default") :
                        Res.get("data.na"));

        String identityTag = identityService.findAnyIdentityByNetworkId(node.getNetworkId())
                .map(Identity::getTag)
                .orElse("default");
        nodeTagTooltip = Res.get("settings.network.header.nodeTag.tooltip", identityTag);
        nodeTag = identityTag.contains("-") ? identityTag.split("-")[0] : identityTag;

        address = node.findMyAddress().map(Address::getFullAddress).orElse(Res.get("data.na"));

        numConnections.set(String.valueOf(node.getAllConnections().count()));
        listener = new Node.Listener() {
            @Override
            public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
            }

            @Override
            public void onConnection(Connection connection) {
                UIThread.run(() -> numConnections.set(String.valueOf(node.getAllConnections().count())));
            }

            @Override
            public void onDisconnect(Connection connection, CloseReason closeReason) {
                UIThread.run(() -> numConnections.set(String.valueOf(node.getAllConnections().count())));
            }

            @Override
            public void onStateChange(Node.State state) {
            }
        };
    }

    @Override
    public void onActivate() {
        node.addListener(listener);
    }

    @Override
    public void onDeactivate() {
        node.removeListener(listener);
    }

    public int compareAddress(NodeListItem other) {
        return address.compareTo(other.getAddress());
    }

    public int compareKeyId(NodeListItem other) {
        return keyId.compareTo(other.getKeyId());
    }

    public int compareType(NodeListItem other) {
        return type.compareTo(other.getType());
    }

    public int compareNodeTag(NodeListItem other) {
        return nodeTag.compareTo(other.getNodeTag());
    }

    public int compareNumConnections(NodeListItem other) {
        return Long.compare(other.getNode().getAllConnections().count(), node.getAllConnections().count());
    }
}
