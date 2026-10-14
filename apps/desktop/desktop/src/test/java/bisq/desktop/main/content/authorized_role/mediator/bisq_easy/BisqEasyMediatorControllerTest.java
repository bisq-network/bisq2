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

package bisq.desktop.main.content.authorized_role.mediator.bisq_easy;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.market.MarketRepository;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.util.StringUtils;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The mediator's case list shows a mediation case only if the mediator's trade channel matches the contract of the
 * request. The mediator's node creates the channel with the mediator's current profile, while the contract holds the
 * profile which the taker saw when it took the offer. The test covers the comparison which the filter of
 * BisqEasyMediatorController uses, because the controller itself needs a running UI.
 */
class BisqEasyMediatorControllerTest {
    private static final String TRADE_ID = "5a6c1a0c-30e0-3c33-a8a5-d1d0e2a3a7a1";

    private final Identity mediatorIdentity = identity("mediator", 1000);
    private final UserProfile maker = userProfile(identity("maker", 1001).getNetworkId(), "");
    private final UserProfile taker = userProfile(identity("taker", 1002).getNetworkId(), "");
    private final BisqEasyOffer offer = offer(maker.getNetworkId());

    @Test
    void caseIsShownWhenTheMediatorChangedItsStatementAfterTheTradeWasTaken() {
        UserProfile mediatorWhenTaken = userProfile(mediatorIdentity.getNetworkId(), "statement when the trade was taken");
        UserProfile mediatorNow = UserProfile.forEdit(mediatorWhenTaken, "", "new statement");

        assertThat(BisqEasyMediatorController.isChannelMatchingContract(mediatorsChannel(mediatorNow, offer),
                contract(offer, Optional.of(mediatorWhenTaken)))).isTrue();
    }

    @Test
    void caseIsShownWhenTheAddressesOfTheMediatorChangedAfterTheTradeWasTaken() {
        UserProfile mediatorWhenTaken = userProfile(withOtherAddress(mediatorIdentity.getNetworkId(), 2000), "");
        UserProfile mediatorNow = userProfile(mediatorIdentity.getNetworkId(), "");

        assertThat(BisqEasyMediatorController.isChannelMatchingContract(mediatorsChannel(mediatorNow, offer),
                contract(offer, Optional.of(mediatorWhenTaken)))).isTrue();
    }

    @Test
    void caseOfAnotherMediatorIsNotShown() {
        UserProfile mediatorNow = userProfile(mediatorIdentity.getNetworkId(), "");
        UserProfile otherMediator = userProfile(identity("other-mediator", 1003).getNetworkId(), "");

        assertThat(BisqEasyMediatorController.isChannelMatchingContract(mediatorsChannel(mediatorNow, offer),
                contract(offer, Optional.of(otherMediator)))).isFalse();
    }

    // All cases of a trade share its channel, whose offer comes from the first accepted request. A request with a
    // differing offer opens its own case, which shows its own contract.
    @Test
    void caseWhoseContractHasAnotherOfferThanTheChannelIsShown() {
        UserProfile mediatorNow = userProfile(mediatorIdentity.getNetworkId(), "");

        assertThat(BisqEasyMediatorController.isChannelMatchingContract(mediatorsChannel(mediatorNow, offer(maker.getNetworkId())),
                contract(offer, Optional.of(mediatorNow)))).isTrue();
    }

    private BisqEasyOpenTradeChannel mediatorsChannel(UserProfile mediator, BisqEasyOffer channelOffer) {
        return BisqEasyOpenTradeChannel.create(TRADE_ID,
                channelOffer,
                new UserIdentity(mediatorIdentity, mediator),
                Set.of(maker, taker),
                Optional.of(mediator),
                true);
    }

    private BisqEasyContract contract(BisqEasyOffer contractOffer, Optional<UserProfile> mediator) {
        return new BisqEasyContract(System.currentTimeMillis(),
                contractOffer,
                taker.getNetworkId(),
                100_000,
                100_0000,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                mediator,
                new MarketPriceSpec(),
                60_000_0000);
    }

    private static BisqEasyOffer offer(NetworkId makerNetworkId) {
        return new BisqEasyOffer(StringUtils.createUid(),
                System.currentTimeMillis(),
                makerNetworkId,
                Direction.SELL,
                MarketRepository.getUSDBitcoinMarket(),
                new QuoteSideFixedAmountSpec(100_0000),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                1,
                "1.0.0",
                "2.1.13");
    }

    private static UserProfile userProfile(NetworkId networkId, String statement) {
        return new UserProfile(0,
                networkId.getPubKey().getKeyId(),
                new ProofOfWork(networkId.getPubKeyHash(), 0, null, 1, new byte[72], 0),
                0,
                networkId,
                "",
                statement,
                "");
    }

    private static Identity identity(String keyId, int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        Address address = Address.from("127.0.0.1", port);
        NetworkId networkId = new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), keyId));
        return new Identity(keyId, networkId, new KeyBundle(keyId, keyPair, TorKeyGeneration.generateKeyPair(),
                I2PKeyGeneration.generateKeyPair()));
    }

    private static NetworkId withOtherAddress(NetworkId networkId, int port) {
        Address otherAddress = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(otherAddress.getTransportType(), otherAddress)),
                networkId.getPubKey());
    }
}
