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

package bisq.trade.bisq_easy;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceProviderInfo;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.util.StringUtils;
import bisq.contract.ContractService;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.security.SecurityService;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest;

import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Take offer requests for the offers and contracts of {@link BisqEasyTradeTestData}, and the services of a maker which
 * accepts them.
 */
final class BisqEasyTakeOfferRequestTestFixtures {
    // The price at which QUOTE_AMOUNT buys BASE_AMOUNT
    private static final double MARKET_PRICE = 100_000;

    private BisqEasyTakeOfferRequestTestFixtures() {
    }

    /**
     * Stubs the services of the maker so that the maker accepts a take offer request for the offer: the offer is in an
     * offerbook message, the market price is the price of the contract amounts, and the contract signature is verified.
     *
     * @return the offerbook channel which holds the offer
     */
    static BisqEasyOfferbookChannel makerAcceptsTakeOfferRequests(ServiceProvider serviceProvider,
                                                                  Identity makerIdentity,
                                                                  BisqEasyOffer offer) {
        when(serviceProvider.getIdentityService().findAnyIdentityById(makerIdentity.getId()))
                .thenReturn(Optional.of(makerIdentity));

        BisqEasyOfferbookChannel offerbookChannel = new BisqEasyOfferbookChannel(offer.getMarket());
        offerbookChannel.addChatMessage(new BisqEasyOfferbookMessage(offerbookChannel.getId(),
                offer.getMakersUserProfileId(),
                Optional.of(offer),
                Optional.empty(),
                Optional.empty(),
                System.currentTimeMillis(),
                false));
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels())
                .thenReturn(new ObservableSet<>(Set.of(offerbookChannel)));

        Market market = offer.getMarket();
        MarketPrice marketPrice = new MarketPrice(PriceQuote.fromFiatPrice(MARKET_PRICE, market.getQuoteCurrencyCode()),
                System.currentTimeMillis(),
                mock(MarketPriceProviderInfo.class));
        ObservableHashMap<Market, MarketPrice> marketPriceByMarket = new ObservableHashMap<>();
        marketPriceByMarket.put(market, marketPrice);
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPrice(market)).thenReturn(Optional.of(marketPrice));
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByMarket);
        when(serviceProvider.getBondedRolesService().getMarketPriceService()).thenReturn(marketPriceService);
        when(serviceProvider.getSettingsService().getMaxTradePriceDeviation().get()).thenReturn(0.05);

        when(serviceProvider.getContractService()).thenReturn(new ContractService(mock(SecurityService.class)));
        return offerbookChannel;
    }

    static BisqEasyTakeOfferRequest takeOfferRequest(String tradeId,
                                                     Identity takerIdentity,
                                                     BisqEasyContract contract) {
        try {
            return new BisqEasyTakeOfferRequest(StringUtils.createUid(),
                    tradeId,
                    BisqEasyProtocol.VERSION,
                    takerIdentity.getNetworkId(),
                    contract.getMaker().getNetworkId(),
                    contract,
                    new ContractService(mock(SecurityService.class))
                            .signContract(contract, takerIdentity.getKeyBundle().getKeyPair()));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
