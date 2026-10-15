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

package bisq.offer.options;

import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nodes of v2.1.13 do not know AccountOption.saltedAccountPayloadHash. They compute the hash serialization of an offer
 * without it, and they sort the options of an offer by the hash code of their fields. A node of this version must
 * compute the same bytes, or nodes of v2.1.13 reject offers which it accepts (see "Adding a field to a
 * DistributedData type" in docs/specifications/network/distributed-data-payload-bytes.md).
 */
class AccountOptionHashCompatibilityTest {
    private static final PaymentMethod<?> WISE = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);
    private static final String SALTED_ACCOUNT_ID = "a1b2c3d4e5f6a7b8c9d0a1b2c3d4e5f6a7b8c9d0";

    @Test
    void hashSerializationLeavesOutTheSaltedAccountPayloadHash() {
        AccountOption option = accountOption(filledHash((byte) 1));

        assertThat(option.toProto(true).getAccountOption().getSaltedAccountPayloadHash().isEmpty()).isTrue();
        assertThat(option.toProto(false).getAccountOption().getSaltedAccountPayloadHash().toByteArray())
                .isEqualTo(filledHash((byte) 1));
    }

    @Test
    void hashCodeIsTheValueOfTheFieldsWhichNodesOfV2113Know() {
        AccountOption option = accountOption(filledHash((byte) 1));

        // Lombok's @EqualsAndHashCode over the fields of AccountOption in v2.1.13
        int expected = 1;
        expected = expected * 59 + WISE.hashCode();
        expected = expected * 59 + SALTED_ACCOUNT_ID.hashCode();
        expected = expected * 59 + Optional.of("DE").hashCode();
        expected = expected * 59 + List.of("DE", "AT").hashCode();
        expected = expected * 59 + Optional.empty().hashCode();
        expected = expected * 59 + List.of().hashCode();
        assertThat(option.hashCode()).isEqualTo(expected);
        assertThat(accountOption(filledHash((byte) 2)).hashCode()).isEqualTo(expected);
    }

    @Test
    void equalityStillDependsOnTheSaltedAccountPayloadHash() {
        assertThat(accountOption(filledHash((byte) 1))).isEqualTo(accountOption(filledHash((byte) 1)));
        assertThat(accountOption(filledHash((byte) 1))).isNotEqualTo(accountOption(filledHash((byte) 2)));
    }

    // The order of the options and the hash serialization of the offer do not depend on the field, so they are those
    // which a node of v2.1.13 computes. Many values are tried, because the order depends on the hash codes.
    @Test
    void hashSerializationOfAnOfferDoesNotDependOnTheSaltedAccountPayloadHash() throws UnresolvableProtobufException {
        NetworkId maker = createNetworkId();
        for (int i = 0; i < 50; i++) {
            MuSigOffer offer = muSigOffer(maker, "a1b2c3d4e5f6a7b8c9d0a1b2c3d4e5f6a7b8c9" + String.format("%02x", i));
            Offer withOtherHash = Offer.fromProto(withSaltedAccountPayloadHash(offer.toProto(false),
                    filledHash((byte) (i + 1))));

            assertThat(withOtherHash.serializeForHash()).isEqualTo(offer.serializeForHash());
            assertThat(withOtherHash).isNotEqualTo(offer);
        }
    }

    private static AccountOption accountOption(byte[] saltedAccountPayloadHash) {
        return new AccountOption(WISE,
                SALTED_ACCOUNT_ID,
                Optional.of("DE"),
                List.of("DE", "AT"),
                Optional.empty(),
                List.of(),
                saltedAccountPayloadHash);
    }

    private static MuSigOffer muSigOffer(NetworkId maker, String saltedAccountId) {
        return new MuSigOffer("offerId",
                maker,
                Direction.SELL,
                new Market("BTC", "USD", "Bitcoin", "US Dollar"),
                new QuoteSideFixedAmountSpec(1_000_000),
                new MarketPriceSpec(),
                List.of(WISE),
                List.of(new CollateralOption(0.25, 0.25),
                        new AccountOption(WISE,
                                saltedAccountId,
                                Optional.empty(),
                                List.of(),
                                Optional.empty(),
                                List.of(),
                                new byte[20])),
                "1.0.0");
    }

    private static bisq.offer.protobuf.Offer withSaltedAccountPayloadHash(bisq.offer.protobuf.Offer offer,
                                                                          byte[] saltedAccountPayloadHash) {
        bisq.offer.protobuf.Offer.Builder builder = offer.toBuilder();
        for (bisq.offer.protobuf.OfferOption.Builder option : builder.getOfferOptionsBuilderList()) {
            if (option.hasAccountOption()) {
                option.getAccountOptionBuilder()
                        .setSaltedAccountPayloadHash(ByteString.copyFrom(saltedAccountPayloadHash));
            }
        }
        return builder.build();
    }

    private static byte[] filledHash(byte value) {
        byte[] hash = new byte[20];
        Arrays.fill(hash, value);
        return hash;
    }

    private static NetworkId createNetworkId() {
        Address address = Address.from("127.0.0.1", 1001);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "key-1001"));
    }
}
