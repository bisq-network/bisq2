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


    public static List<BitcoinSettlementMethod> getSettlementMethods(SwapProtocolType protocolType) {
        return switch (protocolType) {
            case BTC_XMR_SWAP -> List.of(BitcoinSettlementMethod.MAINCHAIN);
            case LIQUID_SWAP -> List.of(BitcoinSettlementMethod.LBTC);
            case BSQ_SWAP -> List.of(BitcoinSettlementMethod.MAINCHAIN);
            case LN_SWAP -> List.of(BitcoinSettlementMethod.LN);
            case MULTISIG -> List.of(BitcoinSettlementMethod.MAINCHAIN);
            case BSQ_BOND -> List.of(BitcoinSettlementMethod.values());
            case REPUTATION -> List.of(BitcoinSettlementMethod.values());
        };
    }

    @Override
    public ProtocolMessageEnum toProto() {
        return null;
    }
}
