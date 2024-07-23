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

package bisq.desktop.webcam;

import bisq.application.ApplicationService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.timer.Scheduler;
import bisq.common.util.NetworkUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static bisq.desktop.webcam.WebcamAppService.State.*;
import static com.google.common.base.Preconditions.checkArgument;

// On MacOS one can reset the camera access permissions by `tccutil reset Camera`. This is helpful for tesing failure
// scenarios with failed permissions.
@Slf4j
public class WebcamAppService implements Service {
    private static final int SOCKET_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);
    private static final long STARTUP_TIME_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    private static final long CHECK_HEART_BEAT_INTERVAL = TimeUnit.SECONDS.toMillis(10);
    private static final long HEART_BEAT_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    @Getter
    private final Observable<State> state = new Observable<>();
    private Pin qrCodePin, isShutdownSignalReceivedPin, restartSignalReceivedPin;

    public enum State {
        NEW,
        STARTING,
        RUNNING,
        STOPPING,
        TERMINATED
    }

    @Getter
    private final WebcamAppModel model;
    private final QrCodeListeningServer qrCodeListeningServer;
    private final WebcamProcessLauncher webcamProcessLauncher;
    private Optional<Scheduler> checkHeartBeatUpdateScheduler = Optional.empty();
    private Optional<Scheduler> maxStartupTimeScheduler = Optional.empty();

    public WebcamAppService(ApplicationService.Config config) {
        model = new WebcamAppModel(config);
        InputHandler inputHandler = new InputHandler(model);
        qrCodeListeningServer = new QrCodeListeningServer(SOCKET_TIMEOUT, inputHandler, this::handleException);
        webcamProcessLauncher = new WebcamProcessLauncher(model.getBaseDir());

        state.set(NEW);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        state.set(STOPPING);
        stopSchedulers();
        unbind();
        qrCodeListeningServer.stopServer();
        return webcamProcessLauncher.shutdown()
                .thenApply(terminatedGraceFully -> {
                    state.set(TERMINATED);
                    model.reset();
                    return terminatedGraceFully;
                });
    }

    public void start() {
        checkArgument(isIdle(), "Start call when service is not in idle state");

        model.getLastHeartBeatTimestamp().set(0L);
        model.reset();

        setupTimeoutSchedulers();

        int port = NetworkUtils.selectRandomPort();
        model.setPort(port);

        // Start local tcp server listening for input from qr code scan
        qrCodeListeningServer.start(port);

        state.set(STARTING);
        log.error("STARTING");
        webcamProcessLauncher.start(port)
                .whenComplete((process, throwable) -> {
                    if (throwable != null) {
                        handleException(throwable);
                    } else {
                        log.error("RUNNING");
                        state.set(RUNNING);
                    }
                });
        log.info("We start the webcam application as new Java process and listen for a QR code result. TCP listening port={}", port);

        qrCodePin = model.getQrCode().addObserver(qrCode -> {
            if (qrCode != null) {
                shutdown();
            }
        });
        isShutdownSignalReceivedPin = model.getIsShutdownSignalReceived().addObserver(isShutdownSignalReceived -> {
            if (isShutdownSignalReceived != null) {
                shutdown();
            }
        });
        restartSignalReceivedPin = model.getRestartSignalReceived().addObserver(restartSignalReceived -> {
            if (restartSignalReceived != null && restartSignalReceived) {
                restart();
            }
        });
    }

    public CompletableFuture<Boolean> restart() {
        // We leave triggering the start to the client, so it can initialialize its recources and to avoid delays
        // by JavaFX thread mapping
        return shutdown();
    }

    private void unbind() {
        if (qrCodePin != null) {
            qrCodePin.unbind();
        }
        if (isShutdownSignalReceivedPin != null) {
            isShutdownSignalReceivedPin.unbind();
        }
        if (restartSignalReceivedPin != null) {
            restartSignalReceivedPin.unbind();
        }
    }

    public boolean isIdle() {
        return state.get() == NEW || state.get() == TERMINATED;
    }

    private void stopSchedulers() {
        maxStartupTimeScheduler.ifPresent(Scheduler::stop);
        maxStartupTimeScheduler = Optional.empty();
        checkHeartBeatUpdateScheduler.ifPresent(Scheduler::stop);
        checkHeartBeatUpdateScheduler = Optional.empty();
    }

    private void handleException(Throwable throwable) {
        shutdown();
    }

    private void setupTimeoutSchedulers() {
        stopSchedulers();

        maxStartupTimeScheduler = Optional.of(Scheduler.run(() -> {
            if (model.getLastHeartBeatTimestamp().get() == 0) {
                String errorMessage = "Have not received a heartbeat signal from the webcam app after " + STARTUP_TIME_TIMEOUT / 1000 + " seconds.";
                log.warn(errorMessage);
                model.getLocalException().set(new TimeoutException(errorMessage));
            } else {
                checkHeartBeatUpdateScheduler = Optional.of(Scheduler.run(() -> {
                            long now = System.currentTimeMillis();
                            if (now - model.getLastHeartBeatTimestamp().get() > HEART_BEAT_TIMEOUT) {
                                String errorMessage = "The last reeceived heartbeat signal from the webcam app is older than " + HEART_BEAT_TIMEOUT / 1000 + " seconds.";
                                log.warn(errorMessage);
                                model.getLocalException().set(new TimeoutException(errorMessage));
                            }
                        })
                        .periodically(CHECK_HEART_BEAT_INTERVAL));
            }
        }).after(STARTUP_TIME_TIMEOUT));
    }
}
