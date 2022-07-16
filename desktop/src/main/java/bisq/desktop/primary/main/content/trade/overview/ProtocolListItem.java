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

package bisq.desktop.primary.main.content.trade.overview;

import bisq.common.data.Pair;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.protocol.SwapProtocol;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@EqualsAndHashCode
public class ProtocolListItem implements TableItem {

    private final SwapProtocol.Type swapProtocolType;
    private final NavigationTarget navigationTarget;
    private final String basicInfo;
    private final String markets;
    private final String marketsInfo;
    private final String securityInfo;
    private final Pair<Long, Long> tradeLimits;
    private final String privacyInfo;
    private final String convenienceInfo;
    private final String costInfo;
    private final String speedInfo;
    private final String releaseDate;
    private final String protocolsName;
    private final String iconId;

    ProtocolListItem(SwapProtocol.Type swapProtocolType,
                     NavigationTarget navigationTarget,
                     String basicInfo,
                     String markets,
                     String marketsInfo,
                     String securityInfo,
                     Pair<Long, Long> tradeLimits,
                     String privacyInfo,
                     String convenienceInfo,
                     String costInfo,
                     String speedInfo,
                     String releaseDate,
                     String iconId) {
        this.swapProtocolType = swapProtocolType;
        this.navigationTarget = navigationTarget;
        this.basicInfo = basicInfo;
        this.markets = markets;
        this.marketsInfo = marketsInfo;
        this.securityInfo = securityInfo;
        this.tradeLimits = tradeLimits;
        this.privacyInfo = privacyInfo;
        this.convenienceInfo = convenienceInfo;
        this.costInfo = costInfo;
        this.speedInfo = speedInfo;
        this.releaseDate = releaseDate;
        this.iconId = iconId;
        protocolsName = Res.get("trade.protocols." + swapProtocolType.name());
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
}
