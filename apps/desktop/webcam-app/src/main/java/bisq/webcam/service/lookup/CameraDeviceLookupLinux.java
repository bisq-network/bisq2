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

package bisq.webcam.service.lookup;

import bisq.common.observable.Observable;
import bisq.common.threading.ExecutorFactory;
import bisq.webcam.service.ErrorCode;
import bisq.webcam.service.WebcamException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Adjusted version to make webcam usage work on Linux.
 * Tested with Ubuntu 22 on a Dell XPS laptop
 * org.bytedeco.opencv.opencv_videoio.VideoCapture failes on that config, thus the check how many devices are available
 * is removed and the number of devices to try is set to 3.
 */
@Slf4j
public class CameraDeviceLookupLinux implements CameraDeviceLookup {
    @Getter
    private final Observable<Integer> deviceNumber = new Observable<>(0);
    @Getter
    private final Observable<Integer> numDevices = new Observable<>(0);

    public CameraDeviceLookupLinux() {
        numDevices.set(3);
    }

    public CompletableFuture<FrameGrabber> find() {
        return CompletableFuture.supplyAsync(() -> {
            int maxDeviceNumber = 3;
            Throwable ignoredException;
            do {
                try {
                    return find(deviceNumber.get()).get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof WebcamException || cause instanceof TimeoutException) {
                        ignoredException = cause;
                        log.warn("Camera with deviceNumber {} not found. We try the next one. Error message={}", deviceNumber, e.getMessage());
                        deviceNumber.set(deviceNumber.get() + 1);
                    } else {
                        throw new WebcamException(ErrorCode.EXECUTION_EXCEPTION, e);
                    }
                } catch (InterruptedException e) {
                    throw new WebcamException(ErrorCode.INTERRUPTED_EXCEPTION, e);
                }
            } while (deviceNumber.get() < maxDeviceNumber);
            ErrorCode errorCode = ignoredException instanceof WebcamException
                    ? ((WebcamException) ignoredException).getErrorCode()
                    : ErrorCode.NO_DEVICE_FOUND;
            throw new WebcamException(errorCode, ignoredException);
        }, ExecutorFactory.newSingleThreadScheduledExecutor("look-up-camera"));
    }

    public CompletableFuture<FrameGrabber> find(int deviceNumber) {
        log.info("Try to find camera with device number {}", deviceNumber);
        return CompletableFuture.supplyAsync(() -> {
                    try (FrameGrabber frameGrabber = new OpenCVFrameGrabber(deviceNumber)) {
                        // has a 10 sec. timeout built in
                        frameGrabber.start();
                        return frameGrabber;
                    } catch (Exception e) {
                        log.error("frameGrabber.start() failed. {}", e.getMessage());
                        throw new WebcamException(ErrorCode.DEVICE_LOOKUP_FAILED, e);
                    }
                }, ExecutorFactory.newSingleThreadScheduledExecutor("look-up-device"))
                .orTimeout(20, TimeUnit.SECONDS);
    }
}
