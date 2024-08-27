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

package bisq.contract;

import bisq.common.application.Service;
import bisq.offer.Offer;
import bisq.security.DigestUtil;
import bisq.security.SecurityService;
import bisq.security.SignatureUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class ContractService implements Service {

    public ContractService(SecurityService securityService) {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);

    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public <T extends Offer<?, ?>> ContractSignatureData signContract(Contract<T> contract, KeyPair keyPair)
            throws GeneralSecurityException {
        byte[] contractHash = getContractHash(contract);
        byte[] signature = SignatureUtil.sign(contractHash, keyPair.getPrivate());
        return new ContractSignatureData(contractHash, signature, keyPair.getPublic());
    }

    public <T extends Offer<?, ?>> boolean verifyContractSignature(Contract<T> contract, ContractSignatureData signatureData)
            throws GeneralSecurityException {
        byte[] contractHash = signatureData.getContractHash();
        checkArgument(Arrays.equals(contractHash, getContractHash(contract)),
                "Contract hash from the signatureData does not match the given contract");
        return SignatureUtil.verify(contractHash, signatureData.getSignature(), signatureData.getPublicKey());
    }

    private <T extends Offer<?, ?>> byte[] getContractHash(Contract<T> contract) {
        return DigestUtil.hash(contract.serializeForHash());
    }

}