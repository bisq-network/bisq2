/*
 * This file is part of misq.
 *
 * misq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * misq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with misq. If not, see <http://www.gnu.org/licenses/>.
 */

package network.misq.grpc;


import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;

/**
 * An implementation of misq.common.handlers.ErrorMessageHandler that avoids
 * an exception loop with the UI's misq.common.taskrunner framework.
 * <p>
 * The legacy ErrorMessageHandler is for reporting error messages only to the UI, but
 * some core api tasks (takeoffer) require one.  This implementation works around
 * the problem of Task ErrorMessageHandlers not throwing exceptions to the gRPC client.
 * <p>
 * Extra care is needed because exceptions thrown by an ErrorMessageHandler inside
 * a Task may be thrown back to the GrpcService object, and if a gRPC ErrorMessageHandler
 * responded by throwing another exception, the loop may only stop after the gRPC
 * stream is closed.
 * <p>
 * A unique instance should be used for a single gRPC call.
 */
public class GrpcErrorMessageHandler implements ErrorMessageHandler {

    private boolean isErrorHandled = false;

    private final StreamObserver<?> responseObserver;
    private final GrpcExceptionHandler exceptionHandler;
    private final Logger log;

    public GrpcErrorMessageHandler(StreamObserver<?> responseObserver,
                                   GrpcExceptionHandler exceptionHandler,
                                   Logger log) {
        this.exceptionHandler = exceptionHandler;
        this.responseObserver = responseObserver;
        this.log = log;
    }

    @Override
    public void handleErrorMessage(String errorMessage) {
        // A task runner may call handleErrorMessage(String) more than once.
        // Throw only one exception if that happens, to avoid looping until the
        // grpc stream is closed
        if (!isErrorHandled) {
            this.isErrorHandled = true;
            log.error(errorMessage);
            exceptionHandler.handleErrorMessage(log,
                    errorMessage,
                    responseObserver);
        }
    }

    public boolean isErrorHandled() {
        return isErrorHandled;
    }
}
