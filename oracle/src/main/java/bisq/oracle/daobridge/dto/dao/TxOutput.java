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

package bisq.oracle.daobridge.dto.dao;

import lombok.Data;

import javax.annotation.Nullable;

@Data
public final class TxOutput {
    private int index;
    private long value;
    private String txId;

    // Before v0.9.6 it was only set if dumpBlockchainData was set to true but we changed that with 0.9.6
    // so that it is always set. We still need to support it because of backward compatibility.
    @Nullable
    private PubKeyScript pubKeyScript; // Has about 50 bytes, total size of TxOutput is about 300 bytes.
    @Nullable
    private String address;
    @Nullable
    private byte[] opReturnData;
    private int blockHeight;
    private TxOutputType txOutputType;

    // The lockTime is stored in the first output of the LOCKUP tx.
    // If not set it is -1, 0 is a valid value.
    private int lockTime;
    // The unlockBlockHeight is stored in the first output of the UNLOCK tx.
    private int unlockBlockHeight;
    private TxOutputKey key;
}
