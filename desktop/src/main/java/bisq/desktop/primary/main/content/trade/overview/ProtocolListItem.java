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
    private final String markets;
    private final String marketsInfo;
    private final String securityInfo;
    private final Pair<Long, Long> tradeLimits;
    private final String tradeLimitsInfo;
    private final String privacyInfo;
    private final String convenienceInfo;
    private final String costInfo;
    private final String speedInfo;

    ProtocolListItem(SwapProtocol.Type swapProtocolType,
                     NavigationTarget navigationTarget,
                     String markets,
                     String marketsInfo,
                     String securityInfo,
                     Pair<Long, Long> tradeLimits,
                     String tradeLimitsInfo,
                     String privacyInfo,
                     String convenienceInfo,
                     String costInfo,
                     String speedInfo

    ) {
        this.swapProtocolType = swapProtocolType;
        this.navigationTarget = navigationTarget;
        this.markets = markets;
        this.marketsInfo = marketsInfo;
        this.securityInfo = securityInfo;
        this.tradeLimits = tradeLimits;
        this.tradeLimitsInfo = tradeLimitsInfo;
        this.privacyInfo = privacyInfo;
        this.convenienceInfo = convenienceInfo;
        this.costInfo = costInfo;
        this.speedInfo = speedInfo;
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
}
