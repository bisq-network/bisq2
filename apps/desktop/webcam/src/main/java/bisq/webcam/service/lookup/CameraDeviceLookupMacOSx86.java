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
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// TODO find libraries which work with Intel macs

/**
 * Version for webcam usage on MacOS with x86 architecture (Intel chips).
 * Tested with Mac OS 10.14 on a 2015 macbook pro, but it suffers a runtime crash caused from native libraries.
 */
@Slf4j
public class CameraDeviceLookupMacOSx86 implements CameraDeviceLookup {
    @Getter
    private final Observable<Integer> deviceNumber = new Observable<>(0);
    @Getter
    private final Observable<Integer> numDevices = new Observable<>();

    public CameraDeviceLookupMacOSx86() {
    }

    public CompletableFuture<FrameGrabber> find() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                numDevices.set(findNumDevices().get());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof WebcamException || cause instanceof TimeoutException) {
                    ErrorCode errorCode = cause instanceof WebcamException
                            ? ((WebcamException) cause).getErrorCode()
                            : ErrorCode.TIMEOUT_EXCEPTION;
                    throw new WebcamException(errorCode, cause);
                } else {
                    throw new WebcamException(ErrorCode.EXECUTION_EXCEPTION, e);
                }
            } catch (InterruptedException e) {
                throw new WebcamException(ErrorCode.INTERRUPTED_EXCEPTION, e);
            }

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
            } while (deviceNumber.get() < numDevices.get());
            ErrorCode errorCode = ignoredException instanceof WebcamException
                    ? ((WebcamException) ignoredException).getErrorCode()
                    : ErrorCode.NO_DEVICE_FOUND;
            throw new WebcamException(errorCode, ignoredException);
        }, ExecutorFactory.newSingleThreadScheduledExecutor("look-up-camera"));
    }

    public CompletableFuture<FrameGrabber> find(int deviceNumber) {
        log.info("Try to find camera with device number {}", deviceNumber);
        return CompletableFuture.supplyAsync(() -> {
                    try (VideoCapture capture = new VideoCapture(deviceNumber);
                         FrameGrabber frameGrabber = new OpenCVFrameGrabber(deviceNumber)) {
                        // We use capture.grab call to get an early error if permissions are denied
                        boolean grabResult = capture.grab();
                        if (!grabResult) {
                            throw new WebcamException(ErrorCode.DEVICE_PERMISSION_DENIED);
                        }

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

    public CompletableFuture<Integer> findNumDevices() {
        return CompletableFuture.supplyAsync(() -> {
                    try (VideoCapture capture = new VideoCapture()) {
                        int numDevices = 0;
                        while (numDevices < 10) {
                            boolean success = capture.open(numDevices++);
                            if (!success) {
                                break;
                            }
                        }
                        log.error("numDevices {}", numDevices);
                        return numDevices;
                    } catch (Exception e) {
                        log.error("capture.open failed. {}", e.getMessage());
                        throw new WebcamException(ErrorCode.NUM_DEVICE_COUNT_FAILED, e);
                    }
                }, ExecutorFactory.newSingleThreadScheduledExecutor("look-up-device"))
                .orTimeout(5, TimeUnit.SECONDS);
    }
}
