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

package bisq.trade.mu_sig;

import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.mu_sig.MuSigContract;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MuSigTradeUtilsTest {
    @Test
    void returnsBaseAsBtcSideForBtcFiatMarket() {
        MuSigContract contract = createContract(createBtcFiatMarket(), 111L, 222L);

        Monetary btcSideMonetary = MuSigTradeUtils.getBtcSideMonetary(contract);
        Monetary nonBtcSideMonetary = MuSigTradeUtils.getNonBtcSideMonetary(contract);

        assertEquals(111L, btcSideMonetary.getValue());
        assertEquals("BTC", btcSideMonetary.getCode());
        assertEquals(222L, nonBtcSideMonetary.getValue());
        assertEquals("USD", nonBtcSideMonetary.getCode());
    }

    @Test
    void returnsQuoteAsBtcSideForCryptoBtcMarket() {
        MuSigContract contract = createContract(createCryptoBtcMarket(), 111L, 222L);

        Monetary btcSideMonetary = MuSigTradeUtils.getBtcSideMonetary(contract);
        Monetary nonBtcSideMonetary = MuSigTradeUtils.getNonBtcSideMonetary(contract);

        assertEquals(222L, btcSideMonetary.getValue());
        assertEquals("BTC", btcSideMonetary.getCode());
        assertEquals(111L, nonBtcSideMonetary.getValue());
        assertEquals("XMR", nonBtcSideMonetary.getCode());
    }

    private MuSigContract createContract(Market market, long baseSideAmount, long quoteSideAmount) {
        MuSigOffer offer = new MuSigOffer("test-id",
                null,
                Direction.BUY,
                market,
                null,
                null,
                List.of(),
                List.of(),
                "1.0.0");
        PaymentMethodSpec<?> baseSpec = market.isBaseCurrencyBitcoin()
                ? PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0)
                : PaymentMethodSpecUtil.createPaymentMethodSpec(new CryptoPaymentMethod("XMR"), "XMR");
        PaymentMethodSpec<?> quoteSpec = market.isBaseCurrencyBitcoin()
                ? PaymentMethodSpecUtil.createPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD")
                : PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        return new MuSigContract(System.currentTimeMillis(),
                offer,
                TradeProtocolType.MU_SIG,
                new Party(Role.TAKER, null),
                baseSideAmount,
                quoteSideAmount,
                baseSpec,
                quoteSpec,
                Optional.empty(),
                null,
                0);
    }

    private Market createBtcFiatMarket() {
        return new Market("BTC", "USD", "Bitcoin", "US Dollar");
    }

    private Market createCryptoBtcMarket() {
        return new Market("XMR", "BTC", "Monero", "Bitcoin");
    }
}
