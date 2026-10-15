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

package bisq.network.p2p.services.data.storage.append;

import bisq.common.encoding.canonical.Canonical;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.StorageData;

/**
 * Interface for AppendOnlyData implementations like AccountAgeWitnessData
 * <p>
 * Implementations must support canonical encoding. The storage key V2 and the proof of work payload of
 * AddAppendOnlyDataRequest use it. The canonical encoding of an implementation is its bare message, without the
 * google.protobuf.Any in which AddAppendOnlyDataRequest carries it:
 * <ul>
 *     <li>DEFAULT and SIGNATURE: all non-transient fields; the bytes equal serialize().</li>
 *     <li>STORAGE_KEY, PROOF_OF_WORK and TRADE_CONTRACT: without the fields annotated with @ExcludeForHash; the bytes
 *     equal serializeForHash(). Nodes of earlier versions compute the proof of work payload with serializeForHash().</li>
 * </ul>
 */
public interface AppendOnlyData extends DistributedData, StorageData, Canonical {
}
