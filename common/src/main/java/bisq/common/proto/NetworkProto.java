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

/**
 * Interface for any object which gets serialized using protobuf
 * <p>
 * We require deterministic serialisation (e.g. used for hashes) for most data.
 * We need to ensure that Collections are deterministically sorted.
 * Maps are not allowed as they do not guarantee that (even if Java have deterministic implementation for it as
 * in HashMap - there is no guarantee that all JVms will support that and non-Java implementations need to be able
 * to deal with it as well. Rust for instance randomize the key set in maps by default for security reasons).
 */
public interface NetworkProto extends Proto {
    // TODO Remove default implementation
    default void verify() {
    }
}
