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

package bisq.desktop.main.content.trade_apps.overview;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.data.Pair;
import bisq.common.util.StringUtils;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@EqualsAndHashCode
public class ProtocolListItem implements TableItem {

    private final TradeAppsAttributes.Type tradeAppsAttributesType;
    private final NavigationTarget navigationTarget;
    private final TradeProtocolType tradeProtocolType;
    private final String protocolsName;
    private final String basicInfo;
    private final String markets;
    private final String securityInfo;
    private final Pair<Long, Long> tradeLimits;
    private final String privacyInfo;
    private final String convenienceInfo;
    private final String releaseDate;
    private final String iconId;

    ProtocolListItem(TradeAppsAttributes.Type tradeAppsAttributesType,
                     NavigationTarget navigationTarget,
                     TradeProtocolType tradeProtocolType,
                     Pair<Long, Long> tradeLimits,
                     String releaseDate) {
        this.tradeAppsAttributesType = tradeAppsAttributesType;
        this.navigationTarget = navigationTarget;
        this.tradeProtocolType = tradeProtocolType;
        String name = tradeAppsAttributesType.name();
        protocolsName = Res.get("tradeApps." + name);
        this.basicInfo = Res.get("tradeApps.overview." + name);
        this.markets = Res.get("tradeApps.overview.markets." + name);
        this.securityInfo = Res.get("tradeApps.overview.security." + name);
        this.tradeLimits = tradeLimits;
        this.privacyInfo = Res.get("tradeApps.overview.privacy." + name);
        this.convenienceInfo = Res.get("tradeApps.overview.convenience." + name);
        this.releaseDate = releaseDate;

        this.iconId = "protocol-" + StringUtils.snakeCaseToKebapCase(name.toLowerCase());
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
}
