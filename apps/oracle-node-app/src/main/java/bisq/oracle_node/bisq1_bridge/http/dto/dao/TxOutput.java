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

package bisq.oracle_node.bisq1_bridge.http.dto.dao;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public final class TxOutput {
    private int index;
    private long value;
    private String txId;

    // Before v0.9.6 it was only set if dumpBlockchainData was set to true, but we changed that with 0.9.6
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TxOutput txOutput)) return false;

        return index == txOutput.index &&
                value == txOutput.value &&
                blockHeight == txOutput.blockHeight &&
                lockTime == txOutput.lockTime &&
                unlockBlockHeight == txOutput.unlockBlockHeight &&
                Objects.equals(txId, txOutput.txId) &&
                Objects.equals(pubKeyScript, txOutput.pubKeyScript) &&
                Objects.equals(address, txOutput.address) &&
                Arrays.equals(opReturnData, txOutput.opReturnData) &&
                txOutputType == txOutput.txOutputType &&
                Objects.equals(key, txOutput.key);
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + Long.hashCode(value);
        result = 31 * result + Objects.hashCode(txId);
        result = 31 * result + Objects.hashCode(pubKeyScript);
        result = 31 * result + Objects.hashCode(address);
        result = 31 * result + Arrays.hashCode(opReturnData);
        result = 31 * result + blockHeight;
        result = 31 * result + Objects.hashCode(txOutputType);
        result = 31 * result + lockTime;
        result = 31 * result + unlockBlockHeight;
        result = 31 * result + Objects.hashCode(key);
        return result;
    }
}
