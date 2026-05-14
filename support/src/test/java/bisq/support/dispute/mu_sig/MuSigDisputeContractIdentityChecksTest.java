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
import org.junit.jupiter.api.DisplayName;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MuSigDisputeContractIdentityChecksTest {
    @Test
    @DisplayName("has matching contract parties returns true when requester is maker and peer is taker")
    void has_matching_contract_parties_returns_true_when_requester_is_maker_and_peer_is_taker() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        MuSigContract contract = createContract(maker, taker);

        boolean result = MuSigDisputeContractIdentityChecks.hasMatchingContractParties(contract, maker, taker);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("has matching contract parties returns true when requester is taker and peer is maker")
    void has_matching_contract_parties_returns_true_when_requester_is_taker_and_peer_is_maker() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        MuSigContract contract = createContract(maker, taker);

        boolean result = MuSigDisputeContractIdentityChecks.hasMatchingContractParties(contract, taker, maker);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("has matching contract parties returns false when peer is not contract party")
    void has_matching_contract_parties_returns_false_when_peer_is_not_contract_party() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        UserProfile stranger = createStranger();
        MuSigContract contract = createContract(maker, taker);

        boolean result = MuSigDisputeContractIdentityChecks.hasMatchingContractParties(contract, maker, stranger);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("has matching contract dispute agent returns true when receiver matches agent network id")
    void has_matching_contract_dispute_agent_returns_true_when_receiver_matches_agent_network_id() {
        UserProfile disputeAgent = createDisputeAgent();

        boolean result = MuSigDisputeContractIdentityChecks.hasMatchingContractDisputeAgent(Optional.of(disputeAgent), disputeAgent.getNetworkId());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("has matching contract dispute agent returns false when receiver does not match agent network id")
    void has_matching_contract_dispute_agent_returns_false_when_receiver_does_not_match_agent_network_id() {
        UserProfile disputeAgent = createDisputeAgent();
        UserProfile receiver = createStranger();

        boolean result = MuSigDisputeContractIdentityChecks.hasMatchingContractDisputeAgent(Optional.of(disputeAgent), receiver.getNetworkId());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("has matching contract dispute agent returns false when agent is missing")
    void has_matching_contract_dispute_agent_returns_false_when_agent_is_missing() {
        UserProfile receiver = createDisputeAgent();

        boolean result = MuSigDisputeContractIdentityChecks.hasMatchingContractDisputeAgent(Optional.empty(), receiver.getNetworkId());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("resolve sender role returns maker when sender is maker")
    void resolve_sender_role_returns_maker_when_sender_is_maker() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        MuSigContract contract = createContract(maker, taker);

        Role result = MuSigDisputeContractIdentityChecks.resolveSenderRole(contract, maker.getId());

        assertThat(result).isEqualTo(Role.MAKER);
    }

    @Test
    @DisplayName("resolve sender role returns taker when sender is taker")
    void resolve_sender_role_returns_taker_when_sender_is_taker() {
        UserProfile maker = createMaker();
        UserProfile taker = createTaker();
        MuSigContract contract = createContract(maker, taker);

        Role result = MuSigDisputeContractIdentityChecks.resolveSenderRole(contract, taker.getId());

        assertThat(result).isEqualTo(Role.TAKER);
    }

    @Test
    @DisplayName("resolve sender role throws when sender is not contract party")
    void resolve_sender_role_throws_when_sender_is_not_contract_party() {
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
                List.of(),
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
