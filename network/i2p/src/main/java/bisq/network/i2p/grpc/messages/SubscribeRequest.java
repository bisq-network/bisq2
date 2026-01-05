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

package bisq.network.i2p.grpc.messages;


import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EqualsAndHashCode
@ToString
public class SubscribeRequest implements Proto {
    @Getter
    private final Topic topic;

    public SubscribeRequest(Topic topic) {
        this.topic = topic;
    }

    @Override
    public bisq.bi2p.protobuf.SubscribeRequest completeProto() {
        return toProto(false);
    }

    @Override
    public bisq.bi2p.protobuf.SubscribeRequest toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    @Override
    public bisq.bi2p.protobuf.SubscribeRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.bi2p.protobuf.SubscribeRequest.newBuilder()
                .setTopic(topic.toProtoEnum());
    }

    // Not used. We still keep it to follow convention.
    public static SubscribeRequest fromProto(bisq.bi2p.protobuf.SubscribeRequest proto) {
        return new SubscribeRequest(Topic.fromProto(proto.getTopic()));
    }
}
