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

package bisq.oracle.daobridge;

import bisq.common.encoding.Hex;
import bisq.common.util.CompletableFutureUtils;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.oracle.daobridge.dto.ProofOfBurnDto;
import bisq.oracle.daobridge.model.AuthorizedProofOfBurnData;
import bisq.security.KeyGeneration;
import com.typesafe.config.Config;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Prepares a service for distributing authorized DAO data. Could be useful for providing existing proof or burns or
 * bonded role data as long we do not have the DAO integrated.
 * An authorized developer could add those data. Verification is done by verifying the signature and pubKey against
 * the provided hard-coded pubKeys.
 * Similar concept is used in Bisq 1 for Filter, Alert or mediator/arbitrator registration.
 * The user who is authorized for publishing that data need to pass the private key as VM argument as defined below.
 * -Dbisq.oracle.daoBridge.privateKey=30818d020100301006072a8648ce3d020106052b8104000a04763074020101042010c2ea3b2b1f1787f8a57d074e550b120cc04b326b43c545214434e474e5cde2a00706052b8104000aa14403420004170a828efbaa0316b7a59ec5a1e8033ca4c215b5e58b17b16f3e3cbfa5ec085f4bdb660c7b766ec5ba92b432265ba3ed3689c5d87118fbebe19e92b9228aca63
  -Dbisq.oracle.daoBridge.publicKey=3056301006072a8648ce3d020106052b8104000a03420004170a828efbaa0316b7a59ec5a1e8033ca4c215b5e58b17b16f3e3cbfa5ec085f4bdb660c7b766ec5ba92b432265ba3ed3689c5d87118fbebe19e92b9228aca63
 */
@Slf4j
public class DaoBridgeService {
    private final NetworkService networkService;
    private final IdentityService identityService;
    @Getter
    private final Config daoBridgeConfig;

    public DaoBridgeService(NetworkService networkService,
                            IdentityService identityService,
                            com.typesafe.config.Config daoBridgeConfig) {
        this.networkService = networkService;
        this.identityService = identityService;
        this.daoBridgeConfig = daoBridgeConfig;
    }

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> publishProofOfBurnDtoSet(List<ProofOfBurnDto> proofOfBurnDtoSet) {
        String privateKey = daoBridgeConfig.getString("privateKey");
        if (privateKey == null || privateKey.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        String publicKey = daoBridgeConfig.getString("publicKey");
        if (publicKey == null || publicKey.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFutureUtils.allOf(proofOfBurnDtoSet.stream()
                        .map(AuthorizedProofOfBurnData::from)
                        .map(authorizedProofOfBurnData -> publishProofOfBurnData(authorizedProofOfBurnData, privateKey, publicKey)))
                .thenApply(results -> !results.contains(false));
    }

    public CompletableFuture<Boolean> publishProofOfBurnData(AuthorizedProofOfBurnData authorizedProofOfBurnData,
                                                             String authorizedPrivateKeyAsHex,
                                                             String authorizedPublicKeyAsHex) {
        try {
            PrivateKey authorizedPrivateKey = KeyGeneration.generatePrivate(Hex.decode(authorizedPrivateKeyAsHex));
            PublicKey authorizedPublicKey = KeyGeneration.generatePublic(Hex.decode(authorizedPublicKeyAsHex));
            return identityService.getOrCreateIdentity(IdentityService.DEFAULT)
                    .thenCompose(identity -> networkService.publishAuthorizedData(authorizedProofOfBurnData,
                            identity.getNodeIdAndKeyPair(),
                            authorizedPrivateKey,
                            authorizedPublicKey))
                    .thenApply(broadCastDataResult -> true);
        } catch (Throwable e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }
}