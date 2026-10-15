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

package bisq.trade.mu_sig.mediation;

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.fiat.NationalBankAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatMessageType;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigDisputeAgentType;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.common.util.StringUtils;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.CollateralOption;
import bisq.offer.options.OfferOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.support.mediation.mu_sig.MuSigMediationRequest;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.KeyPair;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MuSigTraderMediationServiceTest {
    private static final String TRADE_ID = "trade-id";
    private static final String OFFER_ID = "offer-id";

    private NetworkService networkService;
    private MuSigTraderMediationService service;
    private UserProfile maker;
    private UserProfile taker;
    private UserProfile mediator;
    private Identity makerIdentity;

    @BeforeEach
    void setUp() {
        maker = createUserProfile(30_001);
        taker = createUserProfile(30_002);
        mediator = createUserProfile(30_003);
        makerIdentity = createIdentity(maker.getNetworkId());

        networkService = mock(NetworkService.class);
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userProfileService.findUserProfile(maker.getId())).thenReturn(Optional.of(maker));
        when(userProfileService.findUserProfile(taker.getId())).thenReturn(Optional.of(taker));
        UserService userService = mock(UserService.class);
        when(userService.getUserProfileService()).thenReturn(userProfileService);
        ChatService chatService = mock(ChatService.class);
        when(chatService.getMuSigOpenTradeChannelService()).thenReturn(mock(MuSigOpenTradeChannelService.class));

        service = new MuSigTraderMediationService(networkService, chatService, userService, mock(BondedRolesService.class));
    }

    @Test
    void requestCarriesOnlyMessagesOfTheTrade() {
        UserIdentity makerUserIdentity = mock(UserIdentity.class);
        when(makerUserIdentity.getId()).thenReturn(maker.getId());
        when(makerUserIdentity.getUserName()).thenReturn("maker");
        MuSigOpenTradeChannel channel = MuSigOpenTradeChannel.create(TRADE_ID,
                makerUserIdentity,
                Set.of(maker, taker),
                Optional.of(mediator),
                Optional.empty(),
                MuSigDisputeAgentType.NONE);
        String channelIdOfTrade = MuSigOpenTradeChannel.createId(TRADE_ID);
        MuSigOpenTradeMessage messageOfTrade = message(taker, TRADE_ID, channelIdOfTrade);
        channel.addChatMessage(messageOfTrade);
        // The taker chooses the IDs of its messages, and the channel holds messages of the mediator as well. The
        // mediator ignores a request with a message of another trade, channel or sender.
        channel.addChatMessage(message(taker, "other-trade-id", channelIdOfTrade));
        channel.addChatMessage(message(taker, TRADE_ID, MuSigOpenTradeChannel.createId("other-trade-id")));
        channel.addChatMessage(message(mediator, TRADE_ID, channelIdOfTrade));

        service.requestMediation(TRADE_ID,
                makerIdentity,
                new MuSigTradeParty(taker.getNetworkId()),
                mediator,
                createContract(),
                channel);

        ArgumentCaptor<MuSigMediationRequest> request = ArgumentCaptor.forClass(MuSigMediationRequest.class);
        verify(networkService).confidentialSend(request.capture(), eq(mediator.getNetworkId()), any());
        assertEquals(List.of(messageOfTrade), request.getValue().getChatMessages());
    }

    private MuSigOpenTradeMessage message(UserProfile sender, String messageTradeId, String channelId) {
        return new MuSigOpenTradeMessage(messageTradeId,
                StringUtils.createUid(),
                channelId,
                sender,
                maker.getId(),
                maker.getNetworkId(),
                "text",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                Optional.empty(),
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                new HashSet<>());
    }

    private MuSigContract createContract() {
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        List<OfferOption> offerOptions = List.of(
                new CollateralOption(0.25, 0.25),
                new AccountOption(
                        paymentMethod,
                        "0123456789abcdef0123456789abcdef01234567",
                        Optional.empty(),
                        List.of(),
                        Optional.empty(),
                        List.of(),
                        OfferOptionUtil.createSaltedAccountPayloadHash(createAccountPayload("maker", "DE01"), OFFER_ID)
                ));
        MuSigOffer offer = new MuSigOffer(OFFER_ID,
                maker.getNetworkId(),
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(paymentMethod),
                offerOptions,
                "1.0.0");
        PaymentMethodSpec<?> quoteSidePaymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, "EUR");
        return new MuSigContract(System.currentTimeMillis(),
                offer,
                taker.getNetworkId(),
                100_000L,
                3_500_000L,
                quoteSidePaymentMethodSpec,
                OfferOptionUtil.createSaltedAccountPayloadHash(createAccountPayload("taker", "DE02"), OFFER_ID),
                Optional.of(mediator),
                Optional.empty(),
                new MarketPriceSpec(),
                0);
    }

    private static AccountPayload<?> createAccountPayload(String id, String accountNr) {
        return new NationalBankAccountPayload(id,
                "DE",
                "EUR",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                accountNr,
                Optional.empty(),
                Optional.empty());
    }

    private static UserProfile createUserProfile(int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port)));
        NetworkId networkId = new NetworkId(addresses, pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, "nick-" + port, proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private static Identity createIdentity(NetworkId networkId) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyBundle keyBundle = new KeyBundle("maker",
                keyPair,
                TorKeyGeneration.generateKeyPair(),
                I2PKeyGeneration.generateKeyPair());
        return new Identity("maker", networkId, keyBundle);
    }
}
