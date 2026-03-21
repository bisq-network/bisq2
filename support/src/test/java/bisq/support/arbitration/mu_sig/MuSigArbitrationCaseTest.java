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

import bisq.security.keys.KeyGeneration;
import bisq.support.arbitration.ArbitrationPayoutDistributionType;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.Optional;

import static bisq.support.arbitration.ArbitrationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT;
import static bisq.support.arbitration.ArbitrationPayoutDistributionType.SELLER_GETS_TRADE_AMOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MuSigArbitrationCaseTest {
    @Test
    void setSignedMuSigArbitrationResultRejectsChangedSignature() throws GeneralSecurityException {
        MuSigArbitrationCase arbitrationCase = new MuSigArbitrationCase(null);
        MuSigArbitrationResult arbitrationResult = createArbitrationResult(
                new byte[20], BUYER_GETS_TRADE_AMOUNT, 10L, 20L);
        byte[] signature = MuSigArbitrationResultService.signArbitrationResult(arbitrationResult, KeyGeneration.generateDefaultEcKeyPair());
        byte[] otherSignature = MuSigArbitrationResultService.signArbitrationResult(arbitrationResult, KeyGeneration.generateDefaultEcKeyPair());

        assertThat(arbitrationCase.setSignedMuSigArbitrationResult(arbitrationResult, signature)).isTrue();
        assertThatThrownBy(() -> arbitrationCase.setSignedMuSigArbitrationResult(arbitrationResult, otherSignature))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("arbitrationResultSignature");
    }

    @Test
    void setSignedMuSigArbitrationResultRejectsChangedResult() throws GeneralSecurityException {
        MuSigArbitrationCase arbitrationCase = new MuSigArbitrationCase(null);
        MuSigArbitrationResult arbitrationResult = createArbitrationResult(
                new byte[20], BUYER_GETS_TRADE_AMOUNT, 10L, 20L);
        byte[] signature = MuSigArbitrationResultService.signArbitrationResult(arbitrationResult, KeyGeneration.generateDefaultEcKeyPair());
        byte[] otherHash = new byte[20];
        otherHash[0] = 1;

        assertThat(arbitrationCase.setSignedMuSigArbitrationResult(arbitrationResult, signature)).isTrue();
        assertThatThrownBy(() -> arbitrationCase.setSignedMuSigArbitrationResult(
                createArbitrationResult(otherHash, SELLER_GETS_TRADE_AMOUNT, 15L, 25L),
                signature))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MuSigArbitrationResult cannot be changed");
    }

    private MuSigArbitrationResult createArbitrationResult(byte[] contractHash,
                                                           ArbitrationPayoutDistributionType payoutDistributionType,
                                                           long buyerPayoutAmount,
                                                           long sellerPayoutAmount) {
        return new MuSigArbitrationResult(
                contractHash,
                payoutDistributionType,
                buyerPayoutAmount,
                sellerPayoutAmount,
                Optional.of("summary"));
    }
}
