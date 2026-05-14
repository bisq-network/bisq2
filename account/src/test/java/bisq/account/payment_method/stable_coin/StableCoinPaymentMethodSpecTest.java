package bisq.account.payment_method.stable_coin;

import bisq.account.payment_method.PaymentMethodSpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StableCoinPaymentMethodSpecTest {

    @Test
    @DisplayName("toProto and fromProto roundtrip preserves all fields")
    void proto_roundtrip_preserves_fields() {
        StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON);
        StableCoinPaymentMethodSpec original = new StableCoinPaymentMethodSpec(method);

        bisq.account.protobuf.PaymentMethodSpec proto = original.toProto(false);
        StableCoinPaymentMethodSpec deserialized = StableCoinPaymentMethodSpec.fromProto(proto);

        assertEquals(original.getPaymentMethod().getName(), deserialized.getPaymentMethod().getName());
        assertEquals(original, deserialized);
    }

    @Test
    @DisplayName("toProto and fromProto roundtrip with salted maker account id")
    void proto_roundtrip_with_salted_maker_account_id() {
        StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON);
        StableCoinPaymentMethodSpec original = new StableCoinPaymentMethodSpec(method, Optional.of("salt-123"));

        bisq.account.protobuf.PaymentMethodSpec proto = original.toProto(false);
        StableCoinPaymentMethodSpec deserialized = StableCoinPaymentMethodSpec.fromProto(proto);

        assertEquals(original.getPaymentMethod().getName(), deserialized.getPaymentMethod().getName());
        assertTrue(deserialized.getSaltedMakerAccountId().isPresent());
        assertEquals("salt-123", deserialized.getSaltedMakerAccountId().get());
    }

    @Test
    @DisplayName("fromProto dispatched via PaymentMethodSpec.protoToQuoteSidePaymentMethodSpec returns correct type")
    void dispatch_via_quote_side_returns_correct_type() {
        StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON);
        StableCoinPaymentMethodSpec original = new StableCoinPaymentMethodSpec(method);

        bisq.account.protobuf.PaymentMethodSpec proto = original.toProto(false);
        PaymentMethodSpec<?> dispatched = PaymentMethodSpec.protoToQuoteSidePaymentMethodSpec(proto);

        assertInstanceOf(StableCoinPaymentMethodSpec.class, dispatched);
        StableCoinPaymentMethodSpec dispatchedSpec = (StableCoinPaymentMethodSpec) dispatched;
        assertEquals(original.getPaymentMethod().getName(), dispatchedSpec.getPaymentMethod().getName());
    }

    @Test
    @DisplayName("different rails produce different specs")
    void different_rails_produce_different_specs() {
        StableCoinPaymentMethodSpec polygonSpec = new StableCoinPaymentMethodSpec(
                StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON));
        StableCoinPaymentMethodSpec erc20Spec = new StableCoinPaymentMethodSpec(
                StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_ERC20));

        assertNotEquals(polygonSpec, erc20Spec);
        assertNotEquals(polygonSpec.getPaymentMethod().getPaymentRail(),
                erc20Spec.getPaymentMethod().getPaymentRail());
    }
}
