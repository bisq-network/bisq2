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

package bisq.desktop.common.qr.webcam.mvc;

import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.qr.webcam.VideoSize;
import bisq.desktop.common.qr.webcam.WebcamService;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.overlay.OverlayController;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.function.Consumer;

@Slf4j
public class QrCodeWebcamController implements InitWithDataController<QrCodeWebcamController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final Consumer<String> qrCodeDataHandler;

        public InitData(Consumer<String> qrCodeDataHandler) {
            this.qrCodeDataHandler = qrCodeDataHandler;
        }
    }

    private final QrCodeWebcamModel model;
    @Getter
    private final QrCodeWebcamView view;
    private final WebcamService webcamService;
    private Consumer<String> qrCodeDataHandler;
    private Subscription errorMessagePin;
    private Pin exceptionPin, capturedImagePin, qrCodePin;

    public QrCodeWebcamController(ServiceProvider serviceProvider) {
        VideoSize videoSize = VideoSize.SMALL;
        model = new QrCodeWebcamModel(videoSize);
        view = new QrCodeWebcamView(model, this);

        webcamService = serviceProvider.getWebcamService();
        webcamService.setVideoSize(videoSize);
    }

    @Override
    public void initWithData(QrCodeWebcamController.InitData initData) {
        qrCodeDataHandler = initData.getQrCodeDataHandler();
    }

    @Override
    public boolean useCaching() {
        return false;
    }

    @Override
    public void onActivate() {
        exceptionPin = FxBindings.bind(model.getException()).to(webcamService.getException());
        capturedImagePin = FxBindings.bind(model.getWebcamImage()).to(webcamService.getCapturedImage());
        qrCodePin = webcamService.getQrCode().addObserver(qrCode -> {
            if (qrCode != null) {
                UIThread.run(() -> {
                    qrCodeDataHandler.accept(qrCode);
                    UIThread.runOnNextRenderFrame(this::onClose);
                });
            }
        });
        errorMessagePin = EasyBind.subscribe(model.getException(), exception -> {
            if (exception != null) {
                UIScheduler.run(() -> new Popup().error(exception).show()).after(300);
                onClose();
            }
        });
        webcamService.initialize();
    }

    @Override
    public void onDeactivate() {
        exceptionPin.unbind();
        capturedImagePin.unbind();
        qrCodePin.unbind();
        errorMessagePin.unsubscribe();
        model.getException().set(null);
        model.getWebcamImage().set(null);
        webcamService.shutdown();
    }

    void onClose() {
        OverlayController.hide();
    }
}
