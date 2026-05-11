package bisq.offer.bisq_easy;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethod;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethodSpec;
import bisq.account.payment_method.stable_coin.StableCoinPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BisqEasyOfferStableCoinTest {

    @Test
    @DisplayName("offer with StableCoinPaymentMethodSpec roundtrips through proto")
    void offer_with_stablecoin_spec_roundtrips_through_proto() {
        Market market = new Market("BTC", "USDC", "Bitcoin", "USD Coin");
        StableCoinPaymentMethodSpec quoteSideSpec = new StableCoinPaymentMethodSpec(
                StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON));
        BitcoinPaymentMethodSpec baseSideSpec = new BitcoinPaymentMethodSpec(
                BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN));

        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-offer-test");
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(5001)));
        NetworkId makerNetworkId = new NetworkId(addresses, pubKey);

        BisqEasyOffer original = new BisqEasyOffer(
                "offer-stablecoin-test",
                System.currentTimeMillis(),
                makerNetworkId,
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(baseSideSpec),
                List.of(quoteSideSpec),
                List.of(),
                List.of("en"),
                0,
                "1.0.0",
                "1.0.0"
        );

        bisq.offer.protobuf.Offer proto = original.toProto(false);
        BisqEasyOffer deserialized = BisqEasyOffer.fromProto(proto);

        assertEquals("BTC", deserialized.getMarket().getBaseCurrencyCode());
        assertEquals("USDC", deserialized.getMarket().getQuoteCurrencyCode());
        assertEquals(1, deserialized.getQuoteSidePaymentMethodSpecs().size());

        assertInstanceOf(StableCoinPaymentMethodSpec.class, deserialized.getQuoteSidePaymentMethodSpecs().get(0));
        StableCoinPaymentMethodSpec deserializedQuoteSpec =
                (StableCoinPaymentMethodSpec) deserialized.getQuoteSidePaymentMethodSpecs().get(0);
        assertEquals(quoteSideSpec.getPaymentMethod().getName(), deserializedQuoteSpec.getPaymentMethod().getName());
    }

    @Test
    @DisplayName("offer with multiple stablecoin specs preserves all")
    void offer_with_multiple_stablecoin_specs_preserves_all() {
        Market market = new Market("BTC", "USDC", "Bitcoin", "USD Coin");
        StableCoinPaymentMethodSpec polygonSpec = new StableCoinPaymentMethodSpec(
                StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON));
        StableCoinPaymentMethodSpec erc20Spec = new StableCoinPaymentMethodSpec(
                StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_ERC20));
        BitcoinPaymentMethodSpec baseSideSpec = new BitcoinPaymentMethodSpec(
                BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN));

        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-offer-multi");
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(5002)));
        NetworkId makerNetworkId = new NetworkId(addresses, pubKey);

        BisqEasyOffer original = new BisqEasyOffer(
                "offer-multi-stablecoin",
                System.currentTimeMillis(),
                makerNetworkId,
                Direction.SELL,
                market,
                new BaseSideFixedAmountSpec(200_000L),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(baseSideSpec),
                List.of(polygonSpec, erc20Spec),
                List.of(),
                List.of("en"),
                0,
                "1.0.0",
                "1.0.0"
        );

        bisq.offer.protobuf.Offer proto = original.toProto(false);
        BisqEasyOffer deserialized = BisqEasyOffer.fromProto(proto);

        assertEquals(2, deserialized.getQuoteSidePaymentMethodSpecs().size());
        assertTrue(deserialized.getQuoteSidePaymentMethodSpecs().stream()
                .allMatch(spec -> spec instanceof StableCoinPaymentMethodSpec));
    }
}
