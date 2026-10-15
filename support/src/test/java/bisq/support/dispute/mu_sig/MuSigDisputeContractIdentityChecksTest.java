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

package bisq.support.dispute.mu_sig;

import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.contract.Role;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MuSigDisputeContractIdentityChecksTest {
    @Test
    void hasMatchingContractParties_returnsTrue_whenRequesterIsMakerAndPeerIsTaker() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        MuSigContract contract = createContract(maker, taker);

        boolean result = MuSigDisputeContractIdentityChecks.hasMatchingContractParties(contract, maker, taker);

        assertThat(result).isTrue();
    }

    @Test
    void hasMatchingContractParties_returnsTrue_whenRequesterIsTakerAndPeerIsMaker() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        MuSigContract contract = createContract(maker, taker);

        boolean result = MuSigDisputeContractIdentityChecks.hasMatchingContractParties(contract, taker, maker);

        assertThat(result).isTrue();
    }

    @Test
    void hasMatchingContractParties_returnsFalse_whenPeerIsNotContractParty() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        UserProfile stranger = createStranger();
        MuSigContract contract = createContract(maker, taker);

        boolean result = MuSigDisputeContractIdentityChecks.hasMatchingContractParties(contract, maker, stranger);

        assertThat(result).isFalse();
    }

    @Test
    void hasMatchingContractDisputeAgent_returnsTrue_whenReceiverMatchesAgentNetworkId() {
        UserProfile disputeAgent = createDisputeAgent();

        boolean result = MuSigDisputeContractIdentityChecks.hasMatchingContractDisputeAgent(Optional.of(disputeAgent), disputeAgent.getNetworkId());

        assertThat(result).isTrue();
    }

    @Test
    void hasMatchingContractDisputeAgent_returnsFalse_whenReceiverDoesNotMatchAgentNetworkId() {
        UserProfile disputeAgent = createDisputeAgent();
        UserProfile receiver = createStranger();

        boolean result = MuSigDisputeContractIdentityChecks.hasMatchingContractDisputeAgent(Optional.of(disputeAgent), receiver.getNetworkId());

        assertThat(result).isFalse();
    }

    @Test
    void hasMatchingContractDisputeAgent_returnsFalse_whenAgentIsMissing() {
        UserProfile receiver = createDisputeAgent();

        boolean result = MuSigDisputeContractIdentityChecks.hasMatchingContractDisputeAgent(Optional.empty(), receiver.getNetworkId());

        assertThat(result).isFalse();
    }

    @Test
    void resolveSenderRole_returnsMaker_whenSenderIsMaker() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        MuSigContract contract = createContract(maker, taker);

        Role result = MuSigDisputeContractIdentityChecks.resolveSenderRole(contract, maker.getId());

        assertThat(result).isEqualTo(Role.MAKER);
    }

    @Test
    void resolveSenderRole_returnsTaker_whenSenderIsTaker() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        MuSigContract contract = createContract(maker, taker);

        Role result = MuSigDisputeContractIdentityChecks.resolveSenderRole(contract, taker.getId());

        assertThat(result).isEqualTo(Role.TAKER);
    }

    @Test
    void resolveSenderRole_throws_whenSenderIsNotContractParty() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        UserProfile stranger = createStranger();
        MuSigContract contract = createContract(maker, taker);

        assertThatThrownBy(() -> MuSigDisputeContractIdentityChecks.resolveSenderRole(contract, stranger.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private MuSigContract createContract(UserProfile maker, UserProfile taker) {
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        MuSigOffer offer = new MuSigOffer(
                "offer-" + maker.getId(),
                maker.getNetworkId(),
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(paymentMethod),
                List.of(new bisq.offer.options.CollateralOption(0.25, 0.25)),
                "1.0.0"
        );
        PaymentMethodSpec<?> quoteSidePaymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, "EUR");
        return new MuSigContract(
                System.currentTimeMillis(),
                offer,
                taker.getNetworkId(),
                100_000L,
                3_500_000L,
                quoteSidePaymentMethodSpec,
                new byte[20],
                Optional.empty(),
                Optional.empty(),
                new MarketPriceSpec(),
                0
        );
    }

    private UserProfile createUserProfile(int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port))
        );
        NetworkId networkId = new NetworkId(addresses, pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, "nick-" + port, proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private UserProfile createMaker() {
        return createUserProfile(20_001);
    }

    private UserProfile createTaker() {
        return createUserProfile(20_002);
    }

    private UserProfile createStranger() {
        return createUserProfile(20_003);
    }

    private UserProfile createDisputeAgent() {
        return createUserProfile(20_004);
    }
}
