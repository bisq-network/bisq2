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

import bisq.common.threading.ExecutorFactory;
import bisq.webcam.service.ErrorCode;
import bisq.webcam.service.WebcamException;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class CameraDevicceLookup {
    public static CompletableFuture<FrameGrabber> find() {
        int maxDeviceNumber = 4;
        return CompletableFuture.supplyAsync(() -> {
            int deviceNumber = 0;
            do {
                try {
                    return find(deviceNumber).get();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof WebcamException) {
                        log.warn("Camera wth deviceNumber {} not found. We try the next one. Error message={}", deviceNumber, e.getMessage());
                    } else {
                        throw new WebcamException(ErrorCode.EXECUTION_EXCEPTION, e);
                    }
                } catch (InterruptedException e) {
                    throw new WebcamException(ErrorCode.INTERRUPTED_EXCEPTION, e);
                }
            } while (++deviceNumber < maxDeviceNumber);
            throw new WebcamException(ErrorCode.NO_DEVICE_FOUND);
        }, ExecutorFactory.newSingleThreadScheduledExecutor("look-up-camera"));
    }

    public static CompletableFuture<FrameGrabber> find(int deviceNumber) {
        log.info("Try to find camera with device number {}", deviceNumber);
        return CompletableFuture.supplyAsync(() -> {
            // return new OpenCVFrameGrabber(deviceNumber);

            try (FrameGrabber frameGrabber = new OpenCVFrameGrabber(deviceNumber)) {
                frameGrabber.start();
                return frameGrabber;
            } catch (FrameGrabber.Exception e) {
                throw new WebcamException(ErrorCode.DEVICE_LOOKUP_FAILED, e);
            }
        }, ExecutorFactory.newSingleThreadScheduledExecutor("look-up-device"));
    }
}
