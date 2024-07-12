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

package bisq.webcam.service;

import lombok.Getter;
import org.bytedeco.javacv.FrameGrabber;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Getter
public class WebcamException extends RuntimeException {
    private final ErrorCode errorCode;
    private Optional<FrameGrabber.Exception> frameGrabberException = Optional.empty();
    private Optional<ExecutionException> executionException = Optional.empty();
    private Optional<InterruptedException> interruptedException = Optional.empty();
    private Optional<IOException> ioException = Optional.empty();

    public WebcamException(ErrorCode errorCode) {
        super();
        this.errorCode = errorCode;
    }

    public WebcamException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WebcamException(ErrorCode errorCode, FrameGrabber.Exception frameGrabberException) {
        this.errorCode = errorCode;
        this.frameGrabberException = Optional.of(frameGrabberException);
    }

    public WebcamException(ErrorCode errorCode, ExecutionException executionException) {
        this.errorCode = errorCode;
        this.executionException = Optional.of(executionException);
    }

    public WebcamException(ErrorCode errorCode, InterruptedException interruptedException) {
        this.errorCode = errorCode;
        this.interruptedException = Optional.of(interruptedException);
    }

    public WebcamException(ErrorCode errorCode, IOException ioException) {
        this.errorCode = errorCode;
        this.ioException = Optional.of(ioException);
    }
}
