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

package network.misq.grpc;

import io.grpc.stub.StreamObserver;
import network.misq.api.DefaultApi;
import network.misq.grpc.proto.GetVersionGrpc;
import network.misq.grpc.proto.GetVersionReply;
import network.misq.grpc.proto.GetVersionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GrpcVersionService extends GetVersionGrpc.GetVersionImplBase {
    private static final Logger log = LoggerFactory.getLogger(GrpcVersionService.class);

    private final DefaultApi api;

    public GrpcVersionService(DefaultApi api) {
        this.api = api;
    }

    @Override
    public void getVersion(GetVersionRequest req,
                           StreamObserver<GetVersionReply> responseObserver) {
        try {
            var reply = GetVersionReply.newBuilder()
                    .setVersion(api.getVersion())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            new GrpcExceptionHandler().handleException(log, cause, responseObserver);
        }
    }
}
