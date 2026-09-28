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

package bisq.network.p2p.services.data.storage;

import bisq.common.proto.NetworkProto;
import com.google.protobuf.Any;

import java.util.Optional;

// Interface for any data which gets distributed to the P2P network. Usually data from outside the network module 
// like Offer, ChatMessage,...
public interface DistributedData extends NetworkProto {
    static DistributedData fromAny(Any any) {
        return DistributedDataResolver.fromAny(any);
    }

    default Any toAny(boolean serializeForHash) {
        return Any.pack(toProto(serializeForHash));
    }

    MetaData getMetaData();

    default String getClassName() {
        return getMetaData().getClassName();
    }

    // Validates the content of the data. The storage checks separately whether the publisher is the owner (see
    // findOwnerPubKeyHash()).
    boolean isDataInvalid();

    // The hash of the public key of the owner, if the data names its owner in its own fields, for example the author
    // of a chat message or the maker of an offer. Only the owner can publish the data in the network storage, and in
    // the authenticated data store only the owner can refresh and remove it. The authenticated data store accepts data
    // without an owner only as authorized data, which its authorized key owns
    // (see docs/specifications/network/storage-ownership.md).
    Optional<byte[]> findOwnerPubKeyHash();
}