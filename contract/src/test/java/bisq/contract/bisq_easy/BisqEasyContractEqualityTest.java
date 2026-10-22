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

package bisq.contract.bisq_easy;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.market.Market;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.contract.ContractService;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.TradeId;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two contracts are equal when the terms which both traders sign are equal. The taker is one of these terms.
 */
class BisqEasyContractEqualityTest {
    private static final long TAKE_OFFER_DATE = 1_700_000_600_000L;
    private static final long BASE_SIDE_AMOUNT = 100_000;
    private static final long QUOTE_SIDE_AMOUNT = 600_000;

    private final NetworkId maker = createNetworkId(1001, KeyGeneration.generateDefaultEcKeyPair().getPublic(), "maker");
    private final PublicKey takersPublicKey = KeyGeneration.generateDefaultEcKeyPair().getPublic();
    private final NetworkId taker = createNetworkId(1002, takersPublicKey, "taker");
    private final BisqEasyOffer offer = offer();

    @Test
    void copyOfAContractIsEqual() throws Exception {
        BisqEasyContract contract = contract(new Party(Role.TAKER, taker));

        BisqEasyContract copy = BisqEasyContract.fromProto(contract.toProto(false));

        assertThat(copy).isEqualTo(contract);
        assertThat(copy.hashCode()).isEqualTo(contract.hashCode());
    }

    // Each of these takers has the key of the taker, so the contract is the contract of the same trade. The traders
    // sign another hash, so it is another contract.
    @Test
    void contractWithAnotherVersionOfTheTakerIsNotEqual() {
        BisqEasyContract contract = contract(new Party(Role.TAKER, taker));
        List<Party> otherTakers = List.of(
                new Party(Role.TAKER, createNetworkId(2002, takersPublicKey, "taker")),
                new Party(Role.TAKER, createNetworkId(1002, takersPublicKey, "other-key-id")),
                new Party(Role.ESCROW_AGENT, taker),
                new Party(Role.TAKER, taker, Optional.of(new byte[20])));

        for (Party otherTaker : otherTakers) {
            BisqEasyContract otherContract = contract(otherTaker);

            assertThat(TradeId.fromContract(otherContract)).isEqualTo(TradeId.fromContract(contract));
            assertThat(ContractService.getContractHash(otherContract))
                    .isNotEqualTo(ContractService.getContractHash(contract));
            assertThat(otherContract).isNotEqualTo(contract);
        }
    }

    @Test
    void contractWithAnotherTakerIsNotEqual() {
        BisqEasyContract contract = contract(new Party(Role.TAKER, taker));
        NetworkId otherTaker = createNetworkId(1003, KeyGeneration.generateDefaultEcKeyPair().getPublic(), "taker");

        assertThat(contract(new Party(Role.TAKER, otherTaker))).isNotEqualTo(contract);
    }

    private BisqEasyContract contract(Party taker) {
        return new BisqEasyContract(TAKE_OFFER_DATE,
                offer,
                TradeProtocolType.BISQ_EASY,
                taker,
                BASE_SIDE_AMOUNT,
                QUOTE_SIDE_AMOUNT,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                Optional.empty(),
                new MarketPriceSpec(),
                600_000_000L);
    }

    private BisqEasyOffer offer() {
        return new BisqEasyOffer(maker,
                Direction.BUY,
                new Market("BTC", "USD", "Bitcoin", "US Dollar"),
                new BaseSideFixedAmountSpec(BASE_SIDE_AMOUNT),
                new MarketPriceSpec(),
                List.of(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                "",
                List.of("en"),
                "1.0.0");
    }

    private static NetworkId createNetworkId(int port, PublicKey publicKey, String keyId) {
        Address address = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(publicKey, keyId));
    }
}
