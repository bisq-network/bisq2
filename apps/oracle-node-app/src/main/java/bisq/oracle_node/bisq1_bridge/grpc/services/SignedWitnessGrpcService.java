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

package bisq.oracle_node.bisq1_bridge.grpc.services;

import bisq.common.application.Service;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.messages.SignedWitnessDateRequest;
import bisq.oracle_node.bisq1_bridge.grpc.messages.SignedWitnessDateResponse;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Base64;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class SignedWitnessGrpcService implements Service {
    private final GrpcClient grpcClient;

    public SignedWitnessGrpcService(GrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    public long verifyAndRequestDate(AuthorizeSignedWitnessRequest request) {
        log.info("verifyAndRequestDate {}", request);
        try {
            verifySignature(request);

            String hashAsHex = request.getHashAsHex();
            long date = requestDate(hashAsHex);

            long witnessSignDate = request.getWitnessSignDate();
            checkArgument(date == witnessSignDate,
                    "Date of signed witness for %s is not matching the date from the users request. " +
                            "Date from bridge service call: %s; Date from users request: %s",
                    hashAsHex, date, witnessSignDate);

            return date;
        } catch (Exception e) {
            log.warn("Error at verifyAndRequestDate", e);
            throw new RuntimeException(e);
        }
    }

    private void verifySignature(AuthorizeSignedWitnessRequest request) throws GeneralSecurityException {
        long witnessSignDate = request.getWitnessSignDate();
        String profileId = request.getProfileId();
        String hashAsHex = request.getHashAsHex();
        byte[] signature = Base64.getDecoder().decode(request.getSignatureBase64());
        String pubKeyBase64 = request.getPubKeyBase64();

        String messageString = profileId + hashAsHex + witnessSignDate;
        byte[] message = messageString.getBytes(StandardCharsets.UTF_8);
        PublicKey publicKey = KeyGeneration.generatePublic(Base64.getDecoder().decode(pubKeyBase64), KeyGeneration.DSA);
        boolean isValid = SignatureUtil.verify(message,
                signature,
                publicKey,
                SignatureUtil.SHA256withDSA);
        checkArgument(isValid, "Signature verification for %s failed", request);
    }

    private long requestDate(String hashAsHex) {
        var protoRequest = new SignedWitnessDateRequest(hashAsHex).toProto(true);
        var protoResponse = grpcClient.getSignedWitnessBlockingStub().requestSignedWitnessDate(protoRequest);
        SignedWitnessDateResponse response = SignedWitnessDateResponse.fromProto(protoResponse);
        return response.getDate();
    }
}