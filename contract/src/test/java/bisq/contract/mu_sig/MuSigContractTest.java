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
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
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

class MuSigContractTest {
    @Test
    void usesBaseAsBtcSideWhenBaseCurrencyIsBitcoin() {
        PaymentMethodSpec<?> baseSpec = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        PaymentMethodSpec<?> quoteSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD");
        MuSigContract contract = createContract(createBtcFiatMarket(), 111L, 222L, baseSpec, quoteSpec);

        assertEquals(111L, contract.getBtcSideAmount());
        assertEquals(222L, contract.getNonBtcSideAmount());
        assertEquals(baseSpec, contract.getBtcSidePaymentMethodSpec());
        assertEquals(quoteSpec, contract.getNonBtcSidePaymentMethodSpec());
    }

    @Test
    void usesQuoteAsBtcSideWhenBaseCurrencyIsNotBitcoin() {
        PaymentMethodSpec<?> baseSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(new CryptoPaymentMethod("XMR"), "XMR");
        PaymentMethodSpec<?> quoteSpec = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        MuSigContract contract = createContract(createCryptoBtcMarket(), 111L, 222L, baseSpec, quoteSpec);

        assertEquals(222L, contract.getBtcSideAmount());
        assertEquals(111L, contract.getNonBtcSideAmount());
        assertEquals(quoteSpec, contract.getBtcSidePaymentMethodSpec());
        assertEquals(baseSpec, contract.getNonBtcSidePaymentMethodSpec());
    }

    @Test
    void usesTheSelectedSpecWhenTheNonBtcBaseSideOffersMultipleMethods() {
        // The take wizard lets the taker pick one of several offered methods on a crypto
        // market; the contract must carry the picked spec, not assume a single-spec offer.
        PaymentMethodSpec<?> selectedSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(new CryptoPaymentMethod("XMR"), "XMR");
        PaymentMethodSpec<?> quoteSpec = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        MuSigOffer offer = createOffer(createCryptoBtcMarket(),
                List.of(selectedSpec.getPaymentMethod(), new CryptoPaymentMethod("XMR2")),
                List.of());

        MuSigContract contract = new MuSigContract(System.currentTimeMillis(),
                offer,
                null,
                111L,
                222L,
                selectedSpec,
                new byte[20],
                Optional.empty(),
                Optional.empty(),
                null,
                0);

        assertEquals(selectedSpec, contract.getNonBtcSidePaymentMethodSpec());
        assertEquals(quoteSpec, contract.getBtcSidePaymentMethodSpec());
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
                Optional.empty(),
                null,
                0);

        assertFalse(contract.getMaker().getSaltedAccountPayloadHash().isPresent());
    }

    @Test
    void storesTheMarketPriceSnapshotHandedToIt() {
        // The take-offer handoff passes the market-price snapshot the amounts were validated
        // against; the contract must store exactly that value (MuSigContractVerifier compares
        // full contracts, so a divergent price is a signature mismatch).
        PaymentMethodSpec<?> baseSpec = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        PaymentMethodSpec<?> quoteSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD");
        long marketPriceSnapshot = 63_847_1234L;
        MuSigContract contract = createContract(createBtcFiatMarket(), 111L, 222L, baseSpec, quoteSpec, marketPriceSnapshot);

        assertEquals(marketPriceSnapshot, contract.getMarketPrice());
    }

    private MuSigContract createContract(Market market,
                                         long baseSideAmount,
                                         long quoteSideAmount,
                                         PaymentMethodSpec<?> baseSpec,
                                         PaymentMethodSpec<?> quoteSpec) {
        return createContract(market, baseSideAmount, quoteSideAmount, baseSpec, quoteSpec, 0);
    }

    private MuSigContract createContract(Market market,
                                         long baseSideAmount,
                                         long quoteSideAmount,
                                         PaymentMethodSpec<?> baseSpec,
                                         PaymentMethodSpec<?> quoteSpec,
                                         long marketPrice) {
        PaymentMethodSpec<?> nonBtcPaymentMethodSpec = getNonBtcPaymentMethodSpec(market, baseSpec, quoteSpec);
        MuSigOffer offer = createOffer(market,
                List.of(nonBtcPaymentMethodSpec.getPaymentMethod()),
                List.of());
        return new MuSigContract(System.currentTimeMillis(),
                offer,
                null,
                baseSideAmount,
                quoteSideAmount,
                nonBtcPaymentMethodSpec,
                new byte[20],
                Optional.empty(),
                Optional.empty(),
                null,
                marketPrice);
    }

    private PaymentMethodSpec<?> getNonBtcPaymentMethodSpec(Market market,
                                                            PaymentMethodSpec<?> baseSpec,
                                                            PaymentMethodSpec<?> quoteSpec) {
        return market.isBaseCurrencyBitcoin() ? quoteSpec : baseSpec;
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
                withCollateral(offerOptions),
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

    private static java.util.List<bisq.offer.options.OfferOption> withCollateral(java.util.List<? extends bisq.offer.options.OfferOption> options) {
        java.util.List<bisq.offer.options.OfferOption> result = new java.util.ArrayList<>(options.size() + 1);
        result.add(new bisq.offer.options.CollateralOption(0.25, 0.25));
        result.addAll(options);
        return result;
    }

}
