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

package bisq.http_api.rest_api.domain.settings;

import bisq.dto.common.currency.MarketDto;

import javax.annotation.Nullable;
import java.util.Set;

public record SettingsChangeRequest(
        @Nullable Boolean isTacAccepted,
        @Nullable Boolean tradeRulesConfirmed,
        @Nullable Boolean closeMyOfferWhenTaken,
        @Nullable String languageCode,
        @Nullable Set<String> supportedLanguageCodes,
        @Nullable Double maxTradePriceDeviation,
        @Nullable MarketDto selectedMarket,
        @Nullable Integer numDaysAfterRedactingTradeData,
        @Nullable Boolean useAnimations
) {
}
