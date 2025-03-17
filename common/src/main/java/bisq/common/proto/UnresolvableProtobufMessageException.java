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

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnresolvableProtobufMessageException extends RuntimeException {
    public UnresolvableProtobufMessageException(String message, Message proto) {
        super(message + ". Message case not found for proto message: \n" + proto.toString());
    }

    public UnresolvableProtobufMessageException(Message proto) {
        super("Message case not found for proto message: \n" + proto.toString());
    }

    public UnresolvableProtobufMessageException(Any any) {
        super("No class found for resolving proto Any message.\n" + any.toString());
    }

    public UnresolvableProtobufMessageException(InvalidProtocolBufferException e) {
        super("Could not resolve proto Any message", e);
    }
}