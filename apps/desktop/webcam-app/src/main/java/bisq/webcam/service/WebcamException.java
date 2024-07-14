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

import bisq.i18n.Res;
import lombok.Getter;

import java.util.Optional;

@Getter
public class WebcamException extends RuntimeException {
    private final ErrorCode errorCode;

    public WebcamException(ErrorCode errorCode) {
        super();
        this.errorCode = errorCode;
    }

    public WebcamException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WebcamException(ErrorCode errorCode, Throwable exception) {
        super(exception);
        this.errorCode = errorCode;
    }

    @Override
    public String getMessage() {
        return Optional.ofNullable(super.getMessage()).orElse("");
    }

    public String getLocalizedErrorMessage() {
        Throwable cause = getCause();
        while (cause instanceof WebcamException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String details = Optional.ofNullable(cause).map(Throwable::getMessage).orElse(getMessage());
        return Res.get(errorCode.name(), details);
    }
}
