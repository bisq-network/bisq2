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

package bisq.desktop.common.qr.webcam.lookup;

import bisq.common.threading.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class CameraDevicceLookup {
    public static CompletableFuture<FrameGrabber> find() {
        int maxDeviceNumber = 4;
        return CompletableFuture.supplyAsync(() -> {
            int deviceNumber = 0;
            do {
                try {
                    return find(deviceNumber).get();
                } catch (Exception e) {
                    log.warn("Camera wth deviceNumber {} not found. We try the next one. Error message={}", deviceNumber, e.getMessage());
                }
            } while (++deviceNumber < maxDeviceNumber);
            throw new CameraDevicceLookupException("No camera found");
        }, ExecutorFactory.newSingleThreadScheduledExecutor("look-up-camera"));
    }

    public static CompletableFuture<FrameGrabber> find(int deviceNumber) {
        log.info("Try to find camera with device number {}", deviceNumber);
        return CompletableFuture.supplyAsync(() -> {
            try (FrameGrabber frameGrabber = new OpenCVFrameGrabber(deviceNumber)) {
                frameGrabber.start();
                return frameGrabber;
            } catch (Exception e) {
                throw new CameraDevicceLookupException(e);
            }
        }, ExecutorFactory.newSingleThreadScheduledExecutor("look-up-device"));
    }
}
