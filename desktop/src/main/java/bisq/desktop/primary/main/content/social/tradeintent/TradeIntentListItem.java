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

package bisq.desktop.primary.main.content.social.tradeintent;

import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.network.p2p.NetworkId;
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedNetworkIdPayload;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Getter
class TradeIntentListItem implements TableItem {
    private final NetworkPayload networkPayload;
    private final String dateString;
    private final String bid;
    private final String ask;
    private final String ttl;
    private final Date date;
    private final Optional<NetworkId> networkId;

    TradeIntentListItem(NetworkPayload networkPayload) {
        this.networkPayload = networkPayload;
        if (networkPayload instanceof AuthenticatedNetworkIdPayload payload &&
                payload.getData() instanceof TradeIntent tradeIntent) {
            networkId = Optional.of(payload.getNetworkId());
            ttl = TimeFormatter.formatTime(payload.getMetaData().getTtl());
            ask = tradeIntent.ask();
            bid = tradeIntent.bid();
            date = new Date(tradeIntent.date());
            dateString = DateFormatter.formatDateTime(date);
        } else {
            ask = Res.common.get("na");
            ttl = Res.common.get("na");
            networkId = Optional.empty();
            bid = networkPayload.toString();
            dateString = Res.common.get("na");
            date = new Date(0);
        }
    }

    int compareDate(TradeIntentListItem other) {
        return date.compareTo(other.getDate());
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeIntentListItem that = (TradeIntentListItem) o;
        return Objects.equals(networkPayload, that.networkPayload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkPayload);
    }
}
