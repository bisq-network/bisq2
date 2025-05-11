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
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.Random;

import static com.google.protobuf.ByteString.copyFrom;

public class MusigServer {

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder
                .forPort(50051)
                .addService(new MusigServiceImpl())
                .build();

        System.out.println("Starting gRPC server on port 50051...");
        server.start();
        System.out.println("Server started.");
        server.awaitTermination();
    }

    static class MusigServiceImpl extends MusigGrpc.MusigImplBase {
        public void initTrade(bisq.trade.protobuf.PubKeySharesRequest request,
                              StreamObserver<bisq.trade.protobuf.PubKeySharesResponse> responseObserver) {
            System.out.printf("Received InitTrade request: tradeId=%s, myRole=%s%n",
                    request.getTradeId(), request.getMyRole());

            bisq.trade.protobuf.PubKeySharesResponse response = bisq.trade.protobuf.PubKeySharesResponse.newBuilder()
                    .setBuyerOutputPubKeyShare(copyFrom(randomBytes(100)))
                    .setSellerOutputPubKeyShare(copyFrom(randomBytes(100)))
                    .setCurrentBlockHeight(123456)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        public void getNonceShares(bisq.trade.protobuf.NonceSharesRequest request,
                                   StreamObserver<bisq.trade.protobuf.NonceSharesMessage> responseObserver) {
            var response = bisq.trade.protobuf.NonceSharesMessage.newBuilder()
                    .setWarningTxFeeBumpAddress("tb1qexamplewarning")
                    .setRedirectTxFeeBumpAddress("tb1qexampleredirect")
                    .setHalfDepositPsbt(copyFrom(randomBytes(100)))
                    .setSwapTxInputNonceShare(copyFrom(randomBytes(32)))
                    .setBuyersWarningTxBuyerInputNonceShare(copyFrom(randomBytes(32)))
                    .setBuyersWarningTxSellerInputNonceShare(copyFrom(randomBytes(32)))
                    .setSellersWarningTxBuyerInputNonceShare(copyFrom(randomBytes(32)))
                    .setSellersWarningTxSellerInputNonceShare(copyFrom(randomBytes(32)))
                    .setBuyersRedirectTxInputNonceShare(copyFrom(randomBytes(32)))
                    .setSellersRedirectTxInputNonceShare(copyFrom(randomBytes(32)))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }


        public void getPartialSignatures(bisq.trade.protobuf.PartialSignaturesRequest request,
                                         StreamObserver<bisq.trade.protobuf.PartialSignaturesMessage> responseObserver) {
            var response = bisq.trade.protobuf.PartialSignaturesMessage.newBuilder()
                    .setPeersWarningTxBuyerInputPartialSignature(copyFrom(randomBytes(64)))
                    .setPeersWarningTxSellerInputPartialSignature(copyFrom(randomBytes(64)))
                    .setPeersRedirectTxInputPartialSignature(copyFrom(randomBytes(64)))
                    .setSwapTxInputPartialSignature(copyFrom(randomBytes(64)))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }


        public void signDepositTx(bisq.trade.protobuf.DepositTxSignatureRequest request,
                                  StreamObserver<bisq.trade.protobuf.DepositPsbt> responseObserver) {
            var response = bisq.trade.protobuf.DepositPsbt.newBuilder()
                    .setDepositPsbt(copyFrom(randomBytes(200)))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }


        public void publishDepositTx(bisq.trade.protobuf.PublishDepositTxRequest request,
                                     StreamObserver<bisq.trade.protobuf.TxConfirmationStatus> responseObserver) {
            for (int i = 0; i <= 2; i++) {
                var response = bisq.trade.protobuf.TxConfirmationStatus.newBuilder()
                        .setTx(copyFrom(randomBytes(100)))
                        .setCurrentBlockHeight(800000 + i)
                        .setNumConfirmations(i)
                        .build();
                responseObserver.onNext(response);
                try {
                    Thread.sleep(500); // simulate delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            responseObserver.onCompleted();
        }


        public void signSwapTx(bisq.trade.protobuf.SwapTxSignatureRequest request,
                               StreamObserver<bisq.trade.protobuf.SwapTxSignatureResponse> responseObserver) {
            var response = bisq.trade.protobuf.SwapTxSignatureResponse.newBuilder()
                    .setSwapTx(copyFrom(randomBytes(100)))
                    .setPeerOutputPrvKeyShare(copyFrom(randomBytes(32)))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }


        public void closeTrade(bisq.trade.protobuf.CloseTradeRequest request,
                               StreamObserver<bisq.trade.protobuf.CloseTradeResponse> responseObserver) {
            var response = bisq.trade.protobuf.CloseTradeResponse.newBuilder()
                    .setPeerOutputPrvKeyShare(copyFrom(randomBytes(32)))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        private byte[] randomBytes(int len) {
            byte[] data = new byte[len];
            new Random().nextBytes(data);
            return data;
        }
    }
}
