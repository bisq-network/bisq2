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

// Distributed data which names its owner in its own fields, for example the author of a chat message or the maker of
// an offer. Only the owner can publish the data in the network storage, and in the authenticated data store only the
// owner can refresh and remove it. The authenticated data store accepts distributed data which does not name its owner
// only as authorized data, which its authorized key owns (see docs/specifications/network/storage-ownership.md).
public interface OwnershipVerifiableData extends DistributedData {
    // The hash of the public key of the owner, as PubKey.getHash() computes it
    byte[] getOwnerPubKeyHash();
}
