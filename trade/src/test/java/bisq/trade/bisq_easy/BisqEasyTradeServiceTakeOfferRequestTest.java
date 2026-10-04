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
import bisq.bonded_roles.release.AppType;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.util.StringUtils;
import bisq.contract.ContractService;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.security.SecurityService;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferRequest;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTakeOfferResponse;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.user.contact_list.ContactReason;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyerAsTakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.userProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The maker creates the trade of a take offer request under its identity with the key of the offer's maker. The
 * addresses of the maker's network ID can have changed since it published the offer, for example when it added a
 * transport. A request which cannot create a trade of the maker is dropped without an exception and without an answer.
 * The maker verifies a request before it creates the trade, so a rejected request leaves no trade and no contact, see
 * docs/specifications/trade/take-offer-request-validation.md.
 */
class BisqEasyTradeServiceTakeOfferRequestTest {
    private ServiceProvider serviceProvider;
    private IdentityService identityService;
    private BisqEasyTradeService service;
    private Identity makerIdentity;
    private NetworkId offersNetworkId;
    private Identity takerIdentity;
    private BisqEasyContract contract;

    @BeforeEach
    void setUp() {
        serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        makerIdentity = identity("maker");
        // The maker published its offer before it added a transport, so the offer has other addresses of its key.
        offersNetworkId = withOtherAddress(makerIdentity.getNetworkId(), 2000);
        BisqEasyOffer offer = offer("offer-id", offersNetworkId, QUOTE_AMOUNT);
        takerIdentity = identity("taker");
        contract = contract(offer, takerIdentity.getNetworkId(), Optional.empty());

        // Like IdentityService: the lookup by the whole network ID finds the identity only with its current addresses.
        identityService = serviceProvider.getIdentityService();
        when(identityService.findAnyIdentityByNetworkId(makerIdentity.getNetworkId())).thenReturn(Optional.of(makerIdentity));
        when(identityService.findAnyIdentityById(makerIdentity.getId())).thenReturn(Optional.of(makerIdentity));

        addToOfferbook(offer);
        // At this price 100 USD buy the 0.001 BTC of the contract.
        setMarketPrice(offer.getMarket(), PriceQuote.fromFiatPrice(100_000, "USD"));
        when(serviceProvider.getContractService()).thenReturn(new ContractService(mock(SecurityService.class)));
        when(serviceProvider.getSettingsService().getMaxTradePriceDeviation()).thenReturn(new Observable<>(0.05));
        when(serviceProvider.getSettingsService().getCloseMyOfferWhenTaken()).thenReturn(new Observable<>(false));

        service = new BisqEasyTradeService(serviceProvider, AppType.DESKTOP);
        service.initialize().join();
    }

    @AfterEach
    void tearDown() {
        service.shutdown().join();
    }

    @Test
    void honestRequestForAnOfferPublishedUnderEarlierAddressesIsAccepted() throws GeneralSecurityException {
        // The taker addresses the request to the network ID of the offer.
        BisqEasyTakeOfferRequest request = takeOfferRequest(contract, offersNetworkId);

        service.onMessage(request);

        BisqEasyTrade trade = service.findTrade(request.getTradeId()).orElseThrow();
        assertThat(trade.getState()).isEqualTo(
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
        assertThat(trade.getMaker().getContractSignatureData().get()).isNotNull();
        assertThat(trade.getMyIdentity()).isEqualTo(makerIdentity);
        ArgumentCaptor<EnvelopePayloadMessage> sentMessage = ArgumentCaptor.forClass(EnvelopePayloadMessage.class);
        verify(serviceProvider.getNetworkService()).confidentialSend(sentMessage.capture(),
                eq(takerIdentity.getNetworkId()),
                any());
        assertThat(sentMessage.getValue()).isInstanceOf(BisqEasyTakeOfferResponse.class);
        // The taker stored the network ID of the offer for the maker.
        assertThat(((BisqEasyTakeOfferResponse) sentMessage.getValue()).getSender()).isEqualTo(offersNetworkId);
    }

    @Test
    void requestForAnOfferOfAnotherMakerIsDropped() throws GeneralSecurityException {
        // The request reached our key, but its offer belongs to another maker.
        Identity otherMaker = identity("other-maker");
        BisqEasyContract othersContract = contract(offer("other-offer-id", otherMaker.getNetworkId(), QUOTE_AMOUNT),
                takerIdentity.getNetworkId(),
                Optional.empty());
        BisqEasyTakeOfferRequest request = takeOfferRequest(othersContract, offersNetworkId);

        assertThatCode(() -> service.onMessage(request)).doesNotThrowAnyException();

        assertThat(service.findTrade(request.getTradeId())).isEmpty();
        verify(serviceProvider.getNetworkService(), never()).confidentialSend(any(), any(), any());
    }

    @Test
    void requestForAnOfferOfAKeyWithoutIdentityIsDropped() throws GeneralSecurityException {
        Identity keyWithoutIdentity = identity("key-without-identity");
        BisqEasyContract othersContract = contract(offer("other-offer-id", keyWithoutIdentity.getNetworkId(), QUOTE_AMOUNT),
                takerIdentity.getNetworkId(),
                Optional.empty());
        BisqEasyTakeOfferRequest request = takeOfferRequest(othersContract, keyWithoutIdentity.getNetworkId());

        assertThatCode(() -> service.onMessage(request)).doesNotThrowAnyException();

        assertThat(service.findTrade(request.getTradeId())).isEmpty();
        verify(serviceProvider.getNetworkService(), never()).confidentialSend(any(), any(), any());
    }

    @Test
    void requestWhichIsNotAddressedToTheMakerOfItsOfferIsDropped() throws GeneralSecurityException {
        // Our offer under the current addresses, but the taker addressed the request to another of our identities.
        Identity otherOwnIdentity = identity("other-own-identity");
        when(identityService.findAnyIdentityById(otherOwnIdentity.getId())).thenReturn(Optional.of(otherOwnIdentity));
        BisqEasyContract contractOfOurOffer = contract(offer("offer-id", makerIdentity.getNetworkId(), QUOTE_AMOUNT),
                takerIdentity.getNetworkId(),
                Optional.empty());
        BisqEasyTakeOfferRequest request = takeOfferRequest(contractOfOurOffer, otherOwnIdentity.getNetworkId());

        assertThatCode(() -> service.onMessage(request)).doesNotThrowAnyException();

        assertThat(service.findTrade(request.getTradeId())).isEmpty();
    }

    @Test
    void secondRequestForAnExistingTradeIsDropped() throws GeneralSecurityException {
        // A second request with the same offer, taker and take offer date has the trade ID of the first one.
        BisqEasyTakeOfferRequest request = takeOfferRequest(contract, offersNetworkId);
        service.onMessage(request);
        BisqEasyTrade trade = service.findTrade(request.getTradeId()).orElseThrow();
        BisqEasyTradeState stateAfterFirstRequest = trade.getTradeState();

        BisqEasyTakeOfferRequest secondRequest = takeOfferRequest(contract, offersNetworkId);
        assertThatCode(() -> service.onMessage(secondRequest)).doesNotThrowAnyException();

        assertThat(service.getTrades()).containsExactly(trade);
        assertThat(trade.getTradeState()).isEqualTo(stateAfterFirstRequest);
    }

    @Test
    void acceptedRequestAddsTheTakerToTheContacts() throws GeneralSecurityException {
        UserProfile takerProfile = userProfile(takerIdentity.getNetworkId());
        UserProfile makerProfile = userProfile(makerIdentity.getNetworkId());
        enableAutoAddToContactList(takerProfile, makerProfile);

        service.onMessage(takeOfferRequest(contract, offersNetworkId));

        verify(serviceProvider.getUserService().getContactListService())
                .addContactListEntry(takerProfile, makerProfile, ContactReason.BISQ_EASY_TRADE);
    }

    @Test
    void rejectedRequestLeavesNoTradeAndNoContact() throws GeneralSecurityException {
        enableAutoAddToContactList(userProfile(takerIdentity.getNetworkId()),
                userProfile(makerIdentity.getNetworkId()));
        removeOffersFromOfferbook();
        BisqEasyTakeOfferRequest request = takeOfferRequest(contract, offersNetworkId);

        service.onMessage(request);

        assertThat(service.findTrade(request.getTradeId())).isEmpty();
        assertThat(service.findProtocol(request.getTradeId())).isEmpty();
        verify(serviceProvider.getUserService().getContactListService(), never())
                .addContactListEntry(any(), any(), any());
        ArgumentCaptor<BisqEasyReportErrorMessage> report = ArgumentCaptor.forClass(BisqEasyReportErrorMessage.class);
        verify(serviceProvider.getNetworkService()).confidentialSend(report.capture(),
                eq(takerIdentity.getNetworkId()),
                any());
        assertThat(report.getValue().getTradeId()).isEqualTo(request.getTradeId());
        assertThat(report.getValue().getTradeProtocolFailure()).isEqualTo(TradeProtocolFailure.NO_MATCHING_OFFER_FOUND);
    }

    @Test
    void requestWithAnotherTradeIdIsDropped() throws GeneralSecurityException {
        BisqEasyTakeOfferRequest request = takeOfferRequest("another-trade-id", contract, offersNetworkId);

        assertThatCode(() -> service.onMessage(request)).doesNotThrowAnyException();

        assertThat(service.getTrades()).isEmpty();
        verify(serviceProvider.getNetworkService(), never()).confidentialSend(any(), any(), any());
    }

    @Test
    void copyOfRequestForAnExistingTradeIsDroppedWithoutValidation() throws GeneralSecurityException {
        BisqEasyTakeOfferRequest request = takeOfferRequest(contract, offersNetworkId);
        service.onMessage(request);
        BisqEasyTrade trade = service.findTrade(request.getTradeId()).orElseThrow();
        // The validation would reject the copy, as the offer left the offerbook when it was taken
        removeOffersFromOfferbook();
        clearInvocations(serviceProvider.getNetworkService());

        service.onMessage(takeOfferRequest(contract, offersNetworkId));

        assertThat(service.getTrades()).containsExactly(trade);
        verify(serviceProvider.getNetworkService(), never()).confidentialSend(any(), any(), any());
    }

    private BisqEasyTakeOfferRequest takeOfferRequest(BisqEasyContract contract, NetworkId receiver)
            throws GeneralSecurityException {
        return takeOfferRequest(buyerAsTakerTrade(contract, takerIdentity).getId(), contract, receiver);
    }

    private BisqEasyTakeOfferRequest takeOfferRequest(String tradeId, BisqEasyContract contract, NetworkId receiver)
            throws GeneralSecurityException {
        return new BisqEasyTakeOfferRequest(StringUtils.createUid(),
                tradeId,
                BisqEasyProtocol.VERSION,
                takerIdentity.getNetworkId(),
                receiver,
                contract,
                new ContractService(mock(SecurityService.class)).signContract(contract, takerIdentity.getKeyBundle().getKeyPair()));
    }

    // The author of an offerbook message is the maker of its offer.
    private void addToOfferbook(BisqEasyOffer offer) {
        BisqEasyOfferbookMessage offerbookMessage = mock(BisqEasyOfferbookMessage.class);
        when(offerbookMessage.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        when(offerbookMessage.getAuthorUserProfileId()).thenReturn(offer.getMakersUserProfileId());
        BisqEasyOfferbookChannel channel = mock(BisqEasyOfferbookChannel.class);
        when(channel.getChatMessages()).thenReturn(new ObservableSet<>(Set.of(offerbookMessage)));
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels())
                .thenReturn(new ObservableSet<>(Set.of(channel)));
    }

    private void removeOffersFromOfferbook() {
        when(serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels())
                .thenReturn(new ObservableSet<>());
    }

    private void enableAutoAddToContactList(UserProfile takerProfile, UserProfile makerProfile) {
        when(serviceProvider.getSettingsService().getDoAutoAddToContactList()).thenReturn(true);
        when(serviceProvider.getUserService().getUserProfileService().findUserProfile(takerProfile.getId()))
                .thenReturn(Optional.of(takerProfile));
        when(serviceProvider.getUserService().getUserProfileService().findUserProfile(makerProfile.getId()))
                .thenReturn(Optional.of(makerProfile));
    }

    private void setMarketPrice(Market market, PriceQuote priceQuote) {
        MarketPrice marketPrice = new MarketPrice(priceQuote, System.currentTimeMillis(), mock(MarketPriceProviderInfo.class));
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPrice(market)).thenReturn(Optional.of(marketPrice));
        ObservableHashMap<Market, MarketPrice> marketPriceByMarket = new ObservableHashMap<>();
        marketPriceByMarket.put(market, marketPrice);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByMarket);
        when(serviceProvider.getBondedRolesService().getMarketPriceService()).thenReturn(marketPriceService);
    }

    private static NetworkId withOtherAddress(NetworkId networkId, int port) {
        Address otherAddress = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(otherAddress.getTransportType(), otherAddress)),
                networkId.getPubKey());
    }
}
