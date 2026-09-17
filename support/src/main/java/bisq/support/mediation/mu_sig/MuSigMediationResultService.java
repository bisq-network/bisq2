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

package bisq.support.mediation.mu_sig;

import bisq.common.validation.NetworkDataValidation;
import bisq.contract.ContractService;
import bisq.contract.mu_sig.MuSigContract;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;

@Slf4j
public final class MuSigMediationResultService {
    private MuSigMediationResultService() {
    }

    public static byte[] signMediationResult(MuSigMediationResult mediationResult,
                                             KeyPair keyPair)
            throws GeneralSecurityException {
        byte[] mediationResultHash = getMediationResultHash(mediationResult);
        return SignatureUtil.sign(mediationResultHash, keyPair.getPrivate());
    }

    public static boolean verifyMediationResult(MuSigMediationResult mediationResult,
                                                byte[] mediationResultSignature,
                                                MuSigContract contract,
                                                PublicKey publicKey) throws GeneralSecurityException {
        if (!Arrays.equals(mediationResult.getContractHash(), ContractService.getContractHash(contract))) {
            log.warn("Contract hash from MuSigMediationResult does not match the given contract");
            return false;
        }
        return verifyMediationResult(mediationResult, mediationResultSignature, publicKey);
    }

    public static byte[] getMediationResultHash(MuSigMediationResult mediationResult) {
        return DigestUtil.hash(mediationResult.serializeForHash());
    }

    private static boolean verifyMediationResult(MuSigMediationResult mediationResult,
                                                byte[] mediationResultSignature,
                                                PublicKey publicKey) throws GeneralSecurityException {
        NetworkDataValidation.validateECSignature(mediationResultSignature);
        return SignatureUtil.verify(getMediationResultHash(mediationResult), mediationResultSignature, publicKey);
    }
}
