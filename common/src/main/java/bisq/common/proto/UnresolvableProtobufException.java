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

package bisq.common.proto;

import lombok.extern.slf4j.Slf4j;

/**
 * Thrown by fromProto and fromAny if a protobuf message does not describe a valid object: an unknown message type or
 * enum value, a field which fails validation, or an invalid key. The data is received from the network or read from
 * disk, so the caller has to decide how to handle it, for example to skip the entry or to drop the message.
 * A fromProto method converts every RuntimeException from its own code, for example from the verify method of a
 * constructor, into this exception. So a caller does not need to catch RuntimeException.
 */
@Slf4j
public class UnresolvableProtobufException extends Exception {

    public UnresolvableProtobufException(String message) {
        super(message);
    }

    public UnresolvableProtobufException(Throwable cause) {
        super(cause);
    }

    public UnresolvableProtobufException(String message, Throwable cause) {
        super(message, cause);
    }
}