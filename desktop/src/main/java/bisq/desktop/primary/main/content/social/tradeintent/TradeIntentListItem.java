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
import bisq.network.NetworkId;
import bisq.presentation.formatters.DateFormatter;
import bisq.social.intent.TradeIntent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

// Note: tradeintent package will likely get removed
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
@Getter
public class TradeIntentListItem implements TableItem {
    private final TradeIntent tradeIntent;
    private final NetworkId networkId;
    @EqualsAndHashCode.Include
    private final String id;
    private final String dateString;
    private final String bid;
    private final String ask;
    private final Date date;
    private final String userName;

    TradeIntentListItem(TradeIntent tradeIntent) {
        this.tradeIntent = tradeIntent;
        networkId = tradeIntent.maker().getNetworkId();
        id = tradeIntent.id();
        userName = "TODO";//tradeIntent.maker().userName();
        ask = tradeIntent.ask();
        bid = tradeIntent.bid();
        date = new Date(tradeIntent.date());
        dateString = DateFormatter.formatDateTime(date);
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
}
