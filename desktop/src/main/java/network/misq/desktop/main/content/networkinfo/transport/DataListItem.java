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

package network.misq.desktop.main.content.networkinfo.transport;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.desktop.components.table.TableItem;
import network.misq.i18n.Res;
import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.services.data.AuthenticatedTextPayload;
import network.misq.network.p2p.services.data.NetworkPayload;
import network.misq.presentation.formatters.DateFormatter;
import network.misq.presentation.formatters.TimeFormatter;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Getter
public class DataListItem implements TableItem {
    private final NetworkPayload networkPayload;
    private final String received;
    private final String content;
    private final String networkIdInfo;
    private final String ttl;
    private final Date date;
    private final Optional<NetworkId> networkId;

    public DataListItem(NetworkPayload networkPayload) {
        this.networkPayload = networkPayload;
        date = new Date();
        this.received = DateFormatter.formatDateTime(date);
        if (networkPayload instanceof AuthenticatedTextPayload authenticatedTextPayload) {
            content = authenticatedTextPayload.getText();
            networkId = Optional.of(authenticatedTextPayload.getNetworkId());
            networkIdInfo = networkId.toString();
            ttl = TimeFormatter.formatTime(authenticatedTextPayload.getMetaData().getTtl());
        } else {
            content = networkPayload.toString();
            networkIdInfo = Res.common.get("na");
            ttl = Res.common.get("na");
            networkId = Optional.empty();
        }
    }

    public int compareDate(DataListItem other) {
        return date.compareTo(other.getDate());
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
/*

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataListItem that = (DataListItem) o;
        return Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }
*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataListItem that = (DataListItem) o;
        return Objects.equals(networkPayload, that.networkPayload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkPayload);
    }
}
