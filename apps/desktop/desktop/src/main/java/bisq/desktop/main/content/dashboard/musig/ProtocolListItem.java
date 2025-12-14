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

package bisq.desktop.main.content.dashboard.musig;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.util.StringUtils;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@Slf4j
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProtocolListItem {
    @EqualsAndHashCode.Include
    private final TradeAppsAttributes.Type tradeAppsAttributesType;
    @EqualsAndHashCode.Include
    private final NavigationTarget navigationTarget;
    @EqualsAndHashCode.Include
    private final TradeProtocolType tradeProtocolType;
    @EqualsAndHashCode.Include
    private final String maxTradeLimit;

    private final String protocolsName;
    private final String basicInfo;
    private final String markets;
    private final String securityInfo;
    private final String privacyInfo;
    private final String convenienceInfo;
    private final String iconId;

    ProtocolListItem(TradeAppsAttributes.Type tradeAppsAttributesType,
                     NavigationTarget navigationTarget,
                     TradeProtocolType tradeProtocolType,
                     String maxTradeLimit) {
        this.tradeAppsAttributesType = tradeAppsAttributesType;
        this.navigationTarget = navigationTarget;
        this.tradeProtocolType = tradeProtocolType;
        this.maxTradeLimit = maxTradeLimit;

        String name = tradeAppsAttributesType.name();
        this.privacyInfo = Res.get("tradeApps.overview.privacy." + name);
        protocolsName = Res.get("tradeApps." + name);
        this.basicInfo = Res.get("tradeApps.overview." + name);
        this.markets = Res.get("tradeApps.overview.markets." + name);
        this.securityInfo = Res.get("tradeApps.overview.security." + name);
        this.convenienceInfo = Res.get("tradeApps.overview.convenience." + name);
        this.iconId = "protocol-" + StringUtils.snakeCaseToKebapCase(name.toLowerCase(Locale.ROOT), Locale.ROOT);
    }

}
