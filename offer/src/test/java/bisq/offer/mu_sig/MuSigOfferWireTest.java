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

package bisq.offer.mu_sig;

import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.util.StringUtils;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.options.AccountOption;
import bisq.offer.options.CollateralOption;
import bisq.offer.options.OfferOption;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Wire-boundary coverage: every case serializes a complete offer to bytes, parses it back and
 * dispatches through Offer.fromProto, so protobuf field fidelity and the top-level dispatch are
 * part of what is asserted.
 */
class MuSigOfferWireTest {
    private static final Market MARKET = new Market("BTC", "USD", "Bitcoin", "US Dollar");
    private static final PaymentMethod<?> WISE = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);

    @Test
    void validOfferSurvivesTheWireByteExact() throws Exception {
        MuSigOffer offer = createOffer(new CollateralOption(0.25, 0.25));

        Offer<?, ?> roundTripped = Offer.fromProto(serializeAndParse(offer.toProto(true)));

        assertThat(roundTripped).isEqualTo(offer);
    }

    @Test
    void nanPayloadBitsInCollateralAreRejectedAtTheWire() throws Exception {
        assertRejectedAfterMutation(collateral -> collateral.toBuilder()
                .setBuyerSecurityDeposit(Double.longBitsToDouble(0x7ff8000000000001L))
                .build());
    }

    @Test
    void serializedNegativeZeroCollateralIsRejectedAtTheWire() throws Exception {
        assertRejectedAfterMutation(collateral -> collateral.toBuilder()
                .setSellerSecurityDeposit(-0.0)
                .build());
    }

    @Test
    void asymmetricCollateralOfferIsRejectedAtTheWire() throws Exception {
        assertRejectedAfterMutation(collateral -> collateral.toBuilder()
                .setBuyerSecurityDeposit(0.1)
                .setSellerSecurityDeposit(0.2)
                .build());
    }

    @Test
    void offerWithoutCollateralOptionIsRejectedAtTheWire() throws Exception {
        MuSigOffer offer = createOffer(new CollateralOption(0.25, 0.25));
        bisq.offer.protobuf.Offer.Builder builder = offer.toProto(true).toBuilder();
        int index = collateralOptionIndex(builder);
        builder.removeOfferOptions(index);

        bisq.offer.protobuf.Offer parsed = serializeAndParse(builder.build());
        assertThatThrownBy(() -> Offer.fromProto(parsed))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void offerWithDuplicateCollateralOptionsIsRejectedAtTheWire() throws Exception {
        MuSigOffer offer = createOffer(new CollateralOption(0.25, 0.25));
        bisq.offer.protobuf.Offer.Builder builder = offer.toProto(true).toBuilder();
        int index = collateralOptionIndex(builder);
        builder.addOfferOptions(builder.getOfferOptions(index));

        bisq.offer.protobuf.Offer parsed = serializeAndParse(builder.build());
        assertThatThrownBy(() -> Offer.fromProto(parsed))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void assertRejectedAfterMutation(UnaryOperator<bisq.offer.protobuf.CollateralOption> mutation) throws Exception {
        MuSigOffer offer = createOffer(new CollateralOption(0.25, 0.25));
        bisq.offer.protobuf.Offer.Builder builder = offer.toProto(true).toBuilder();
        int index = collateralOptionIndex(builder);
        bisq.offer.protobuf.OfferOption option = builder.getOfferOptions(index);
        builder.setOfferOptions(index, option.toBuilder()
                .setCollateralOption(mutation.apply(option.getCollateralOption()))
                .build());

        bisq.offer.protobuf.Offer parsed = serializeAndParse(builder.build());
        assertThatThrownBy(() -> Offer.fromProto(parsed))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static int collateralOptionIndex(bisq.offer.protobuf.Offer.Builder builder) {
        for (int i = 0; i < builder.getOfferOptionsCount(); i++) {
            if (builder.getOfferOptions(i).hasCollateralOption()) {
                return i;
            }
        }
        throw new IllegalStateException("No CollateralOption in offer proto");
    }

    private static bisq.offer.protobuf.Offer serializeAndParse(bisq.offer.protobuf.Offer proto) throws InvalidProtocolBufferException {
        return bisq.offer.protobuf.Offer.parseFrom(proto.toByteArray());
    }

    private MuSigOffer createOffer(CollateralOption collateralOption) {
        List<OfferOption> offerOptions = List.of(collateralOption,
                new AccountOption(WISE,
                        "a1b2c3d4e5f6a7b8c9d0a1b2c3d4e5f6a7b8c9d0",
                        Optional.empty(),
                        List.of(),
                        Optional.empty(),
                        List.of(),
                        new byte[20]));
        return new MuSigOffer(StringUtils.createUid(),
                createNetworkId(),
                Direction.SELL,
                MARKET,
                new QuoteSideFixedAmountSpec(40_000_000L),
                new MarketPriceSpec(),
                List.of(WISE),
                offerOptions,
                "1.0.0");
    }

    private static NetworkId createNetworkId() {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", 8888)));
        PubKey pubKey = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "test-key");
        return new NetworkId(addresses, pubKey);
    }
}
