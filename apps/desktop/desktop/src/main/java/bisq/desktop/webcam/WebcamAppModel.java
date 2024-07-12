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
import bisq.common.observable.Observable;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@ToString
@Getter
@Slf4j
public class WebcamAppModel {
    private final String baseDir;
    private final Observable<Integer> port = new Observable<>();
    private final Observable<Boolean> imageRecognized = new Observable<>();
    private final Observable<String> qrCode = new Observable<>();
    private final Observable<String> webcamAppErrorMessage = new Observable<>();
    private final Observable<String> userFriendlyErrorMessage = new Observable<>();
    private final Observable<Throwable> exception = new Observable<>();
    private final Observable<Boolean> isShutdownSignalReceived = new Observable<>();
    private final Observable<Long> lastHeartBeatTimestamp = new Observable<>();

    public WebcamAppModel(ApplicationService.Config config) {
        baseDir = config.getBaseDir().toAbsolutePath().toString();
    }

    public void reset() {
        port.set(null);
        imageRecognized.set(null);
        qrCode.set(null);
        webcamAppErrorMessage.set(null);
        userFriendlyErrorMessage.set(null);
        exception.set(null);
        isShutdownSignalReceived.set(null);
        lastHeartBeatTimestamp.set(null);
    }
}
