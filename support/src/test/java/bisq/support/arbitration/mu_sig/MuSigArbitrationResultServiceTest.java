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

package bisq.support.arbitration.mu_sig;

import bisq.contract.ContractService;
import bisq.contract.mu_sig.MuSigContract;
import bisq.security.keys.KeyGeneration;
import bisq.support.arbitration.ArbitrationPayoutDistributionType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MuSigArbitrationResultServiceTest {
    @Test
    void verifyReturnsTrueForMatchingSignature() throws GeneralSecurityException {
        KeyPair arbitratorKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        MuSigArbitrationResult arbitrationResult = createArbitrationResult();
        byte[] arbitrationResultSignature = MuSigArbitrationResultService.signArbitrationResult(arbitrationResult, arbitratorKeyPair);

        assertThat(MuSigArbitrationResultService.verifyArbitrationResult(arbitrationResult,
                arbitrationResultSignature,
                createContract("contract-a"),
                arbitratorKeyPair.getPublic())).isTrue();
    }

    @Test
    void verifyReturnsFalseForTamperedResult() throws GeneralSecurityException {
        KeyPair arbitratorKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        byte[] arbitrationResultSignature = MuSigArbitrationResultService.signArbitrationResult(createArbitrationResult(), arbitratorKeyPair);
        MuSigArbitrationResult tamperedResult = new MuSigArbitrationResult(
                ContractService.getContractHash(createContract("contract-a")),
                ArbitrationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT,
                11L,
                22L,
                Optional.of("tampered"));

        assertThat(MuSigArbitrationResultService.verifyArbitrationResult(tamperedResult,
                arbitrationResultSignature,
                arbitratorKeyPair.getPublic())).isFalse();
    }

    @Test
    void verifyThrowsForMismatchedContractHash() throws GeneralSecurityException {
        KeyPair arbitratorKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        MuSigArbitrationResult arbitrationResult = createArbitrationResult();
        byte[] arbitrationResultSignature = MuSigArbitrationResultService.signArbitrationResult(arbitrationResult, arbitratorKeyPair);

        assertThatThrownBy(() -> MuSigArbitrationResultService.verifyArbitrationResult(arbitrationResult,
                arbitrationResultSignature,
                createContract("contract-b"),
                arbitratorKeyPair.getPublic()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contract hash");
    }

    private MuSigArbitrationResult createArbitrationResult() {
        return new MuSigArbitrationResult(
                ContractService.getContractHash(createContract("contract-a")),
                ArbitrationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT,
                10L,
                20L,
                Optional.of("summary"));
    }

    private MuSigContract createContract(String id) {
        MuSigContract contract = mock(MuSigContract.class);
        when(contract.serializeForHash()).thenReturn(createContractPayload(id));
        return contract;
    }

    private byte[] createContractPayload(String id) {
        return id.getBytes(StandardCharsets.UTF_8);
    }
}
