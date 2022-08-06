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

package bisq.account.settlement;

import bisq.account.protocol.SwapProtocolType;
import com.google.protobuf.ProtocolMessageEnum;

import java.util.List;

public enum BitcoinSettlementMethod implements SettlementMethod {
    MAINCHAIN,
    LN,
    LBTC,
    RBTC,
    WBTC,
    OTHER;


    @SuppressWarnings("DuplicateBranchesInSwitch")
    public static List<BitcoinSettlementMethod> getSettlementMethods(SwapProtocolType protocolType) {
        switch (protocolType) {
            case MONERO_SWAP:
                return List.of(BitcoinSettlementMethod.MAINCHAIN);
            case LIQUID_SWAP:
                return List.of(BitcoinSettlementMethod.LBTC);
            case BSQ_SWAP:
                return List.of(BitcoinSettlementMethod.MAINCHAIN);
            case LIGHTNING_X:
                return List.of(BitcoinSettlementMethod.LN);
            case BISQ_MULTISIG:
                return List.of(BitcoinSettlementMethod.MAINCHAIN);
            default:
                throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        }
    }

    @Override
    public ProtocolMessageEnum toProto() {
        return null;
    }
}
