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

package bisq.desktop.common.qr.webcam;

import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.threading.ExecutorFactory;
import bisq.desktop.common.qr.webcam.converter.FrameToBitmapConverter;
import bisq.desktop.common.qr.webcam.converter.FrameToImageConverter;
import bisq.desktop.common.qr.webcam.lookup.CameraDevicceLookup;
import bisq.desktop.common.qr.webcam.processor.QrCodeProcessor;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class WebcamService implements Service {
    public final FrameToImageConverter frameToImageConverter;
    @Getter
    private final Observable<Image> capturedImage = new Observable<>();
    @Getter
    private final Observable<String> qrCode = new Observable<>();
    @Getter
    private final Observable<Throwable> exception = new Observable<>();
    @Setter
    private VideoSize videoSize;
    private final QrCodeProcessor qrCodeProcessor;
    private volatile boolean isRunning;
    private volatile boolean isStopped;
    private Optional<ExecutorService> executor = Optional.empty();

    public WebcamService() {
        this.videoSize = VideoSize.SMALL;
        frameToImageConverter = new FrameToImageConverter();
        qrCodeProcessor = new QrCodeProcessor(new FrameToBitmapConverter());
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        isStopped = false;
        exception.set(null);
        qrCode.set(null);
        capturedImage.set(null);
        return findFrameGrabber()
                .thenApply(frameGrabber -> {
                    startFrameCapture(frameGrabber);
                    return true;
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (isStopped) {
            return CompletableFuture.completedFuture(true);
        }
        if (!isRunning) {
            log.warn("Shutdown already in progress");
            return CompletableFuture.completedFuture(true);
        }
        isRunning = false;
        return CompletableFuture.supplyAsync(() -> {
            while (!isStopped) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                }
            }
            qrCode.set(null);
            capturedImage.set(null);
            return true;
        });
    }

    public CompletableFuture<FrameGrabber> findFrameGrabber() {
        return CameraDevicceLookup.find()
                .whenComplete((frameGrabber, throwable) -> {
                    if (throwable != null) {
                        log.error("Error at device lookup", throwable);
                        exception.set(throwable);
                    } else {
                        frameGrabber.setImageWidth(videoSize.getWidth());
                        frameGrabber.setImageHeight(videoSize.getHeight());
                    }
                });
    }

    public void startFrameCapture(FrameGrabber frameGrabber) {
        isRunning = true;
        executor = Optional.of(ExecutorFactory.newSingleThreadExecutor("startFrameCapture"));
        executor.get().submit(() -> {
            try {
                frameGrabber.start();
            } catch (FrameGrabber.Exception e) {
                log.error("Error at starting frameGrabber", e);
                exception.set(e);
                throw new RuntimeException(e);
            }

            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try (Frame capturedFrame = frameGrabber.grabAtFrameRate()) {
                    if (capturedFrame == null) {
                        throw new FrameGrabber.Exception("capturedFrame is null");
                    }
                    qrCodeProcessor.process(capturedFrame).ifPresent(qrCode::set);
                    capturedImage.set(frameToImageConverter.convert(capturedFrame));
                } catch (Exception e) {
                    exception.set(e);
                    throw new RuntimeException(e);
                }
            }
            try {
                frameGrabber.close();
            } catch (FrameGrabber.Exception e) {
                log.error("Exception at closing frameGrabber", e);
            }
            isStopped = true;
        });
    }
}
