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
import bisq.common.market.Market;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.contract.Contract;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A contract has a positive amount on each side, and its price can be computed from the two amounts. So
 * getPriceQuote() does not throw for a contract which was created or parsed.
 */
class BisqEasyContractAmountsTest {
    // 0.001 BTC for 60 USD
    private static final long BASE_SIDE_AMOUNT = 100_000;
    private static final long QUOTE_SIDE_AMOUNT = 600_000;

    private final NetworkId maker = createNetworkId(1001);
    private final NetworkId taker = createNetworkId(1002);

    @Test
    void contractWithPositiveAmountsIsCreatedAndParsed() throws Exception {
        BisqEasyContract contract = contract(BASE_SIDE_AMOUNT, QUOTE_SIDE_AMOUNT);

        BisqEasyContract parsed = BisqEasyContract.fromProto(serializeAndParse(contract.toProto(false)));

        assertThat(parsed).isEqualTo(contract);
        // 60 USD for 0.001 BTC is a price of 60 000 USD. The price has 4 decimal places.
        assertThat(parsed.getPriceQuote().getValue()).isEqualTo(600_000_000L);
    }

    @ParameterizedTest(name = "baseSideAmount={0}, quoteSideAmount={1}")
    @CsvSource({
            // An amount which is not positive
            "0, 600000",
            "100000, 0",
            "0, 0",
            "-100000, 600000",
            "100000, -600000",
            // Positive amounts whose price does not fit into a long
            "1, 9223372036854775807",
            "1, 100000000000"
    })
    void contractWithoutAPriceCannotBeCreated(long baseSideAmount, long quoteSideAmount) {
        assertThatThrownBy(() -> contract(baseSideAmount, quoteSideAmount))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "baseSideAmount={0}, quoteSideAmount={1}")
    @CsvSource({
            "0, 600000",
            "100000, 0",
            "0, 0",
            "-100000, 600000",
            "100000, -600000",
            "1, 9223372036854775807",
            "1, 100000000000"
    })
    void receivedContractWithoutAPriceFailsAtParsing(long baseSideAmount, long quoteSideAmount) throws Exception {
        bisq.contract.protobuf.Contract.Builder builder = contract(BASE_SIDE_AMOUNT, QUOTE_SIDE_AMOUNT)
                .toProto(false).toBuilder();
        builder.getTwoPartyContractBuilder().getBisqEasyContractBuilder()
                .setBaseSideAmount(baseSideAmount)
                .setQuoteSideAmount(quoteSideAmount);
        bisq.contract.protobuf.Contract received = serializeAndParse(builder.build());

        assertThatThrownBy(() -> BisqEasyContract.fromProto(received))
                .isInstanceOf(UnresolvableProtobufException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Contract.fromProto(received))
                .isInstanceOf(UnresolvableProtobufException.class);
    }

    @Test
    void largestPriceWhichFitsIntoALongIsAccepted() {
        // The price value is quoteSideAmount * 10^8 / baseSideAmount for a base currency with 8 decimal places
        long largestQuoteSideAmount = Long.MAX_VALUE / 100_000_000L;

        BisqEasyContract contract = contract(1, largestQuoteSideAmount);

        assertThat(contract.getPriceQuote().getValue()).isEqualTo(largestQuoteSideAmount * 100_000_000L);
        assertThatThrownBy(() -> contract(1, largestQuoteSideAmount + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private BisqEasyContract contract(long baseSideAmount, long quoteSideAmount) {
        return new BisqEasyContract(System.currentTimeMillis(),
                offer(),
                taker,
                baseSideAmount,
                quoteSideAmount,
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

    private static bisq.contract.protobuf.Contract serializeAndParse(bisq.contract.protobuf.Contract proto)
            throws InvalidProtocolBufferException {
        return bisq.contract.protobuf.Contract.parseFrom(proto.toByteArray());
    }

    private static NetworkId createNetworkId(int port) {
        Address address = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "key-" + port));
    }
}
