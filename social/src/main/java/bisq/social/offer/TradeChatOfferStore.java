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

package bisq.social.offer;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeChatOfferStore implements PersistableStore<TradeChatOfferStore> {
    public TradeChatOfferStore() {
    }

    @Override
    public bisq.social.protobuf.TradeChatOfferStore toProto() {
        return bisq.social.protobuf.TradeChatOfferStore.newBuilder()
                .build();
    }

    public static TradeChatOfferStore fromProto(bisq.social.protobuf.TradeChatOfferStore proto) {
        return new TradeChatOfferStore();
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.social.protobuf.TradeChatOfferStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(TradeChatOfferStore chatStore) {
    }

    @Override
    public TradeChatOfferStore getClone() {
        return new TradeChatOfferStore();
    }
}