package bisq.offer.mu_sig.draft;

import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateOfferDraftWorkflowTest {

    @Test
    public void testClampTradeAmount() {
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(100.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(1000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        // Within limits
        TradeAmount amount1 = new TradeAmount(Coin.asBtcFromFaceValue(5.0), Fiat.fromFaceValue(500.0, "USD"));
        assertEquals(amount1, CreateOfferDraftWorkflow.clampTradeAmount(limits, amount1));

        // Below min
        TradeAmount amount2 = new TradeAmount(Coin.asBtcFromFaceValue(0.5), Fiat.fromFaceValue(50.0, "USD"));
        assertEquals(min, CreateOfferDraftWorkflow.clampTradeAmount(limits, amount2));

        // Above max
        TradeAmount amount3 = new TradeAmount(Coin.asBtcFromFaceValue(15.0), Fiat.fromFaceValue(1500.0, "USD"));
        assertEquals(max, CreateOfferDraftWorkflow.clampTradeAmount(limits, amount3));

        // Partially below/above (mixed)
        TradeAmount amount4 = new TradeAmount(Coin.asBtcFromFaceValue(0.5), Fiat.fromFaceValue(1500.0, "USD"));
        TradeAmount expected4 = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(1000.0, "USD"));
        assertEquals(expected4, CreateOfferDraftWorkflow.clampTradeAmount(limits, amount4));
    }

    @Test
    public void testClampBaseSideAmount() {
        TradeAmount minAmount = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(100.0, "USD"));
        TradeAmount maxAmount = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(1000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(minAmount, maxAmount);

        Monetary min = minAmount.getBaseSideAmount();
        Monetary max = maxAmount.getBaseSideAmount();

        // Within limits
        Monetary amount1 = Coin.asBtcFromFaceValue(5.0);
        assertEquals(amount1, CreateOfferDraftWorkflow.clampBaseSideAmount(limits, amount1));

        // Below min
        Monetary amount2 = Coin.asBtcFromFaceValue(0.5);
        assertEquals(min, CreateOfferDraftWorkflow.clampBaseSideAmount(limits, amount2));

        // Above max
        Monetary amount3 = Coin.asBtcFromFaceValue(15.0);
        assertEquals(max, CreateOfferDraftWorkflow.clampBaseSideAmount(limits, amount3));
    }

    @Test
    public void testClampQuoteSideAmount() {
        TradeAmount minAmount = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(100.0, "USD"));
        TradeAmount maxAmount = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(1000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(minAmount, maxAmount);

        Monetary min = minAmount.getQuoteSideAmount();
        Monetary max = maxAmount.getQuoteSideAmount();

        // Within limits
        Monetary amount1 = Fiat.fromFaceValue(500.0, "USD");
        assertEquals(amount1, CreateOfferDraftWorkflow.clampQuoteSideAmount(limits, amount1));

        // Below min
        Monetary amount2 = Fiat.fromFaceValue(50.0, "USD");
        assertEquals(min, CreateOfferDraftWorkflow.clampQuoteSideAmount(limits, amount2));

        // Above max
        Monetary amount3 = Fiat.fromFaceValue(1500.0, "USD");
        assertEquals(max, CreateOfferDraftWorkflow.clampQuoteSideAmount(limits, amount3));
    }
}
