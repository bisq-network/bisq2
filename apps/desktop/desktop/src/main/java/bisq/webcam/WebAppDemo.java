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

package bisq.webcam;

import bisq.common.util.FileUtils;
import bisq.common.util.NetworkUtils;
import bisq.common.util.OsUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Demonstration of usage launching the webapp as a new java process and listening on a detected qr code.
 * As the java dependencies for webcam are huge, and they contain a lot of native drivers it represents a
 * certain security risk to integrate it directly in the Bisq application.
 * By launching it as independent Java process and only listening on a string on a tcp socket, thus limiting potential harm.
 */
@Slf4j
public class WebAppDemo {
    public static void main(String[] args) {
        new WebAppDemo();
        keepRunning();
    }

    private final QrCodeListener qrCodeListener;
    private final WebcamProcessLauncher webcamProcessLauncher;

    public WebAppDemo() {
        int port = NetworkUtils.selectRandomPort();
        qrCodeListener = new QrCodeListener(port, this::onQrCodeDetected, this::onWebcamAppShutdown);

        String baseDir = Path.of(OsUtils.getUserDataDir().toAbsolutePath().toString(), "Bisq2_webAppDemo").toAbsolutePath().toString();
        try {
            FileUtils.makeDirIfNotExists(baseDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        webcamProcessLauncher = new WebcamProcessLauncher(baseDir, port);

        // Start local tcp server listening for input from qr code scan
        qrCodeListener.start();

        // Start webcam app in new Java process. Once a qr code is detected we get called ourhandler.
        // If the webcam app got shut down we get called out shutDown handler
        webcamProcessLauncher.start();
    }

    private void onQrCodeDetected(String qrCode) {
        log.info("onQrCodeDetected={}", qrCode);
        if (qrCode != null) {
            // Once received the qr code we close both the webcam app and the server and exit
            webcamProcessLauncher.shutdown();
            qrCodeListener.stopServer();
            System.exit(0);
        }
    }

    private void onWebcamAppShutdown() {
        log.info("onWebcamAppShutdown");
        qrCodeListener.stopServer();
        System.exit(0);
    }

    private static void keepRunning() {
        try {
            // Avoid that the main thread is exiting
            Thread.currentThread().join();
        } catch (InterruptedException ignore) {
        }
    }
}
