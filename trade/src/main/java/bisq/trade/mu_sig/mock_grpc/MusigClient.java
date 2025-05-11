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

package bisq.trade.mu_sig.mock_grpc;

import bisq.trade.protobuf.MusigGrpc;
import bisq.trade.protobuf.PubKeySharesRequest;
import bisq.trade.protobuf.Role;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;

public class MusigClient {
    public static void main(String[] args) {
        ManagedChannel grpcChannel = Grpc.newChannelBuilderForAddress(
                "127.0.0.1",
                50051,
                InsecureChannelCredentials.create()
        ).build();

        MusigGrpc.MusigBlockingStub musigStub = MusigGrpc.newBlockingStub(grpcChannel);

        PubKeySharesRequest request = PubKeySharesRequest.newBuilder()
                .setTradeId("mock-trade-001")
                .setMyRole(Role.BUYER_AS_TAKER)
                .build();

        var response = musigStub.initTrade(request);

        System.out.println("Received response:");
        System.out.println("  buyerOutputPubKeyShare: " + response.getBuyerOutputPubKeyShare().size() + " bytes");
        System.out.println("  sellerOutputPubKeyShare: " + response.getSellerOutputPubKeyShare().size() + " bytes");
        System.out.println("  currentBlockHeight: " + response.getCurrentBlockHeight());

        // Keep running (simulate long-lived client)
        try {
            Thread.sleep(10_000); // or use Scanner input or similar logic
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            grpcChannel.shutdownNow();
        }
    }
}

