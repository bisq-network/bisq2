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

package bisq.trade.mu_sig.messages.network.handler;

import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MuSigContractVerifierTest {
    private final ContractService contractService = new ContractService(null);

    @Test
    void verifyPeerAcceptsValidMatchingContractAndSignature() {
        MuSigContract contract = createContract();
        KeyPair myKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyPair peerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        ContractSignatureData mySignature = sign(contract, myKeyPair);
        ContractSignatureData peerSignature = sign(contract, peerKeyPair);

        assertDoesNotThrow(() ->
                MuSigContractVerifier.verifyPeer(contractService,
                        contract,
                        mySignature,
                        contract,
                        peerSignature));
    }

    @Test
    void verifyPeerRejectsInvalidPeerSignature() {
        MuSigContract contract = createContract();
        KeyPair myKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyPair peerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        ContractSignatureData mySignature = sign(contract, myKeyPair);
        ContractSignatureData tamperedPeerSignature = tamperSignature(sign(contract, peerKeyPair));

        assertThrows(IllegalArgumentException.class, () ->
                MuSigContractVerifier.verifyPeer(contractService,
                        contract,
                        mySignature,
                        contract,
                        tamperedPeerSignature));
    }

    @Test
    void verifyPeerRejectsMismatchedPeerHash() {
        MuSigContract contract = createContract();
        KeyPair myKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyPair peerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        ContractSignatureData mySignature = sign(contract, myKeyPair);
        ContractSignatureData mismatchedPeerSignatureData = tamperHash(sign(contract, peerKeyPair));

        assertThrows(IllegalArgumentException.class, () ->
                MuSigContractVerifier.verifyPeer(contractService,
                        contract,
                        mySignature,
                        contract,
                        mismatchedPeerSignatureData));
    }

    @Test
    void verifyPeerRejectsMismatchedPeerContract() {
        MuSigContract myContract = createContract();
        MuSigContract peersContract = createContract(333L, 444L);
        KeyPair myKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyPair peerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        ContractSignatureData mySignature = sign(myContract, myKeyPair);
        ContractSignatureData peerSignature = sign(peersContract, peerKeyPair);

        assertThrows(IllegalArgumentException.class, () ->
                MuSigContractVerifier.verifyPeer(contractService,
                        myContract,
                        mySignature,
                        peersContract,
                        peerSignature));
    }

    private ContractSignatureData sign(MuSigContract contract, KeyPair keyPair) {
        return assertDoesNotThrow(() -> contractService.signContract(contract, keyPair));
    }

    private ContractSignatureData tamperSignature(ContractSignatureData signatureData) {
        byte[] tamperedSignature = signatureData.getSignature().clone();
        tamperedSignature[tamperedSignature.length - 1] ^= 0x01;
        return new ContractSignatureData(signatureData.getContractHash().clone(),
                tamperedSignature,
                signatureData.getPublicKey());
    }

    private ContractSignatureData tamperHash(ContractSignatureData signatureData) {
        byte[] tamperedHash = signatureData.getContractHash().clone();
        tamperedHash[0] ^= 0x01;
        return new ContractSignatureData(tamperedHash,
                signatureData.getSignature().clone(),
                signatureData.getPublicKey());
    }

    private MuSigContract createContract() {
        return createContract(111L, 222L);
    }

    private MuSigContract createContract(long baseSideAmount, long quoteSideAmount) {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        MuSigOffer offer = new MuSigOffer("test-id",
                createNetworkId("offer-maker", 9997),
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(baseSideAmount),
                new FixPriceSpec(PriceQuote.fromFiatPrice(50_000, "USD")),
                List.of(),
                List.of(),
                "1.0.0");
        PaymentMethodSpec<?> baseSpec = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        PaymentMethodSpec<?> quoteSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER),
                "USD");
        return new MuSigContract(1_700_000_000_000L,
                offer,
                TradeProtocolType.MU_SIG,
                new Party(Role.MAKER, createNetworkId("maker", 9998)),
                new Party(Role.TAKER, createNetworkId("taker", 9999)),
                baseSideAmount,
                quoteSideAmount,
                baseSpec,
                quoteSpec,
                Optional.empty(),
                offer.getPriceSpec(),
                0);
    }

    private NetworkId createNetworkId(String keyIdSuffix, int port) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        PubKey pubKey = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "test-key-" + keyIdSuffix);
        return new NetworkId(addresses, pubKey);
    }
}
