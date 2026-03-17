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

import bisq.common.encoding.Hex;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.mu_sig.MuSigContract;
import bisq.trade.mu_sig.protocol.MuSigProtocolException;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public final class MuSigContractVerifier {

    public static void verifyPeer(ContractService contractService,
                                  MuSigContract myContract,
                                  ContractSignatureData myContractSignatureData,
                                  MuSigContract peersContract,
                                  ContractSignatureData peersContractSignatureData) {
        checkContractsEqual(peersContract, myContract);
        checkHashesEqual(peersContractSignatureData, myContractSignatureData);
        verifyPeerSignature(contractService, myContract, peersContractSignatureData);
    }

    private static void verifyPeerSignature(ContractService contractService,
                                            MuSigContract myContract,
                                            ContractSignatureData peersContractSignatureData) {
        try {
            checkArgument(contractService.verifyContractSignature(myContract, peersContractSignatureData),
                    "Verifying peer contract signature failed");
        } catch (GeneralSecurityException e) {
            log.error("Verifying peer contract signature failed", e);
            throw new MuSigProtocolException(e);
        }
    }

    private static void checkContractsEqual(MuSigContract peersContract, MuSigContract myContract) {
        checkArgument(peersContract.equals(myContract),
                "Peer's contract is not the same as my contract.\n" +
                        "peersContract=" + peersContract + "\n" +
                        "myContract=" + myContract);
    }

    private static void checkHashesEqual(ContractSignatureData peersContractSignatureData,
                                         ContractSignatureData myContractSignatureData) {
        checkArgument(Arrays.equals(peersContractSignatureData.getContractHash(), myContractSignatureData.getContractHash()),
                "Peer's contractHash at contract signature data is not the same as the contractHash at my contract signature data.\n" +
                        "peersContractSignatureData.contractHash=" + Hex.encode(peersContractSignatureData.getContractHash()) + "\n" +
                        "myContractSignatureData.contractHash=" + Hex.encode(myContractSignatureData.getContractHash()));
    }
}
