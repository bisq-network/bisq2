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

package bisq.contract.mu_sig;

import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class MuSigContractTest {
    @Test
    void usesBaseAsBtcSideWhenBaseCurrencyIsBitcoin() {
        PaymentMethodSpec<?> baseSpec = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        PaymentMethodSpec<?> quoteSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD");
        MuSigContract contract = createContract(createBtcFiatMarket(), 111L, 222L, baseSpec, quoteSpec);

        assertEquals(111L, contract.getBtcSideAmount());
        assertEquals(222L, contract.getNonBtcSideAmount());
        assertSame(baseSpec, contract.getBtcSidePaymentMethodSpec());
        assertSame(quoteSpec, contract.getNonBtcSidePaymentMethodSpec());
    }

    @Test
    void usesQuoteAsBtcSideWhenBaseCurrencyIsNotBitcoin() {
        PaymentMethodSpec<?> baseSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(new CryptoPaymentMethod("XMR"), "XMR");
        PaymentMethodSpec<?> quoteSpec = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        MuSigContract contract = createContract(createCryptoBtcMarket(), 111L, 222L, baseSpec, quoteSpec);

        assertEquals(222L, contract.getBtcSideAmount());
        assertEquals(111L, contract.getNonBtcSideAmount());
        assertSame(quoteSpec, contract.getBtcSidePaymentMethodSpec());
        assertSame(baseSpec, contract.getNonBtcSidePaymentMethodSpec());
    }

    @Test
    void derivesMakerSaltedAccountPayloadHashFromNonBtcQuoteSideAccountOption() {
        FiatPaymentMethod paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        byte[] expectedHash = hash((byte) 1);
        MuSigOffer offer = createOffer(createBtcFiatMarket(),
                List.of(paymentMethod),
                List.of(accountOption(paymentMethod, expectedHash)));

        MuSigContract contract = new MuSigContract(System.currentTimeMillis(),
                offer,
                null,
                111L,
                222L,
                PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, "USD"),
                hash((byte) 9),
                Optional.empty(),
                null,
                0);

        assertArrayEquals(expectedHash, contract.getMaker().getSaltedAccountPayloadHash().orElseThrow());
    }

    @Test
    void derivesMakerSaltedAccountPayloadHashFromNonBtcBaseSideAccountOption() {
        CryptoPaymentMethod paymentMethod = new CryptoPaymentMethod("XMR");
        byte[] expectedHash = hash((byte) 2);
        MuSigOffer offer = createOffer(createCryptoBtcMarket(),
                List.of(paymentMethod),
                List.of(accountOption(paymentMethod, expectedHash)));

        MuSigContract contract = new MuSigContract(System.currentTimeMillis(),
                offer,
                null,
                111L,
                222L,
                PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, "XMR"),
                hash((byte) 9),
                Optional.empty(),
                null,
                0);

        assertArrayEquals(expectedHash, contract.getMaker().getSaltedAccountPayloadHash().orElseThrow());
    }

    @Test
    void makerSaltedAccountPayloadHashIsEmptyWhenNoMatchingAccountOptionExists() {
        FiatPaymentMethod offerPaymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER);
        FiatPaymentMethod differentPaymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        MuSigOffer offer = createOffer(createBtcFiatMarket(),
                List.of(offerPaymentMethod),
                List.of(accountOption(differentPaymentMethod, hash((byte) 3))));

        MuSigContract contract = new MuSigContract(System.currentTimeMillis(),
                offer,
                null,
                111L,
                222L,
                PaymentMethodSpecUtil.createPaymentMethodSpec(offerPaymentMethod, "USD"),
                hash((byte) 9),
                Optional.empty(),
                null,
                0);

        assertFalse(contract.getMaker().getSaltedAccountPayloadHash().isPresent());
    }

    private MuSigContract createContract(Market market,
                                         long baseSideAmount,
                                         long quoteSideAmount,
                                         PaymentMethodSpec<?> baseSpec,
                                         PaymentMethodSpec<?> quoteSpec) {
        MuSigOffer offer = new MuSigOffer("test-id",
                null,
                Direction.BUY,
                market,
                null,
                null,
                List.of(),
                List.of(),
                "1.0.0");
        return new MuSigContract(System.currentTimeMillis(),
                offer,
                TradeProtocolType.MU_SIG,
                new Party(Role.MAKER, offer.getMakerNetworkId()),
                new Party(Role.TAKER, null),
                baseSideAmount,
                quoteSideAmount,
                baseSpec,
                quoteSpec,
                Optional.empty(),
                null,
                0);
    }

    private MuSigOffer createOffer(Market market,
                                   List<bisq.account.payment_method.PaymentMethod<?>> paymentMethods,
                                   List<? extends OfferOption> offerOptions) {
        return new MuSigOffer("test-id",
                null,
                Direction.BUY,
                market,
                null,
                null,
                paymentMethods,
                offerOptions,
                "1.0.0");
    }

    private AccountOption accountOption(bisq.account.payment_method.PaymentMethod<?> paymentMethod, byte[] saltedAccountPayloadHash) {
        return new AccountOption(paymentMethod,
                "0123456789abcdef0123456789abcdef01234567",
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(),
                saltedAccountPayloadHash);
    }

    private byte[] hash(byte value) {
        byte[] bytes = new byte[20];
        bytes[0] = value;
        return bytes;
    }

    private Market createBtcFiatMarket() {
        return new Market("BTC", "USD", "Bitcoin", "US Dollar");
    }

    private Market createCryptoBtcMarket() {
        return new Market("XMR", "BTC", "Monero", "Bitcoin");
    }
}
