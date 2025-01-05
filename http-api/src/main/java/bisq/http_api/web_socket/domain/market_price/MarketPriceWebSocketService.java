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

package bisq.http_api.web_socket.domain.market_price;

import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.common.observable.map.ObservableHashMap;
import bisq.dto.DtoMappings;
import bisq.dto.common.monetary.PriceQuoteDto;
import bisq.http_api.web_socket.domain.SimpleObservableWebSocketService;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import bisq.http_api.web_socket.subscription.Topic;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class MarketPriceWebSocketService extends SimpleObservableWebSocketService<ObservableHashMap<Market, MarketPrice>, Map<String, PriceQuoteDto>> {
    private final MarketPriceService marketPriceService;

    public MarketPriceWebSocketService(ObjectMapper objectMapper,
                                          SubscriberRepository subscriberRepository,
                                          BondedRolesService bondedRolesService) {
        super(objectMapper, subscriberRepository, Topic.MARKET_PRICE);
        marketPriceService = bondedRolesService.getMarketPriceService();
    }

    @Override
    protected ObservableHashMap<Market, MarketPrice> getObservable() {
        return marketPriceService.getMarketPriceByCurrencyMap();
    }

    @Override
    protected HashMap<String, PriceQuoteDto> toPayload(ObservableHashMap<Market, MarketPrice> observable) {
        return getObservable()
                .entrySet().stream()
                .filter(MarketPriceWebSocketService::isBaseCurrencyBtc)
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getQuoteCurrencyCode(),
                        entry -> DtoMappings.PriceQuoteMapping.fromBisq2Model(entry.getValue().getPriceQuote()),
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }

    private static boolean isBaseCurrencyBtc(Map.Entry<Market, MarketPrice> entry) {
        // We get altcoin quotes as well which have BTC as quote currency
        return entry.getKey().getBaseCurrencyCode().equals("BTC");
    }

    @Override
    protected Pin setupObserver() {
        return getObservable().addObserver(this::onChange);
    }
}
