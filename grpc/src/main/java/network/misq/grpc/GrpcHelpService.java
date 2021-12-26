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
import network.misq.grpc.proto.GetMethodHelpReply;
import network.misq.grpc.proto.GetMethodHelpRequest;
import network.misq.grpc.proto.HelpGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GrpcHelpService extends HelpGrpc.HelpImplBase {
    private static final Logger log = LoggerFactory.getLogger(GrpcHelpService.class);

    private final DefaultApi api;

    public GrpcHelpService(DefaultApi api) {
        this.api = api;
    }

    @Override
    public void getMethodHelp(GetMethodHelpRequest req,
                              StreamObserver<GetMethodHelpReply> responseObserver) {
        try {
            String helpText = api.getHelp();
            var reply = GetMethodHelpReply.newBuilder().setMethodHelp(helpText).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            new GrpcExceptionHandler().handleException(log, cause, responseObserver);
        }
    }
}
