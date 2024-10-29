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

package bisq.daemon;

import bisq.daemon.protobuf.BootstrapEvent;
import bisq.daemon.protobuf.Command;
import bisq.daemon.protobuf.DaemonGrpc;
import bisq.network.tor.TorService;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class DaemonService extends DaemonGrpc.DaemonImplBase {
    private final TorService torService;
    private final CountDownLatch isBootstrapped = new CountDownLatch(1);

    public DaemonService(TorService torService) {
        this.torService = torService;
    }

    @Override
    public void bootstrapTor(Command request, StreamObserver<BootstrapEvent> responseObserver) {
        Consumer<bisq.network.tor.controller.events.events.BootstrapEvent> observer = createBootstrapObserver(responseObserver);
        torService.getBootstrapEvent().addObserver(observer);
        torService.initialize();

        try {
            boolean bootstrappedSuccessfully = isBootstrapped.await(2, TimeUnit.MINUTES);
            if (!bootstrappedSuccessfully) {
                log.error("Tor is still bootstrapping after 2 minutes.");
            }
        } catch (InterruptedException e) {
            log.error("Thread was interrupted. This shouldn't happen!");
        }
    }

    private Consumer<bisq.network.tor.controller.events.events.BootstrapEvent> createBootstrapObserver(
            StreamObserver<BootstrapEvent> responseObserver) {
        return bootstrapEvent -> {
            if (bootstrapEvent != null) {
                int progress = bootstrapEvent.getProgress();

                BootstrapEvent event = BootstrapEvent.newBuilder()
                        .setProgress(progress)
                        .setTag(bootstrapEvent.getTag())
                        .setSummary(bootstrapEvent.getSummary())
                        .build();
                responseObserver.onNext(event);

                if (progress == 100) {
                    responseObserver.onCompleted();
                    isBootstrapped.countDown();
                }
            }
        };
    }
}
