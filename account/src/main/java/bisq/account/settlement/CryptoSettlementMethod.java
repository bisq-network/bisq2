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

import bisq.account.protocol_type.SwapProtocolType;
import bisq.i18n.Res;
import com.google.protobuf.ProtocolMessageEnum;

import java.util.List;

public enum CryptoSettlementMethod implements SettlementMethod {
    NATIVE_CHAIN,
    OTHER;

    @Override
    public String getDisplayName(String code) {
        return Res.get(name(), code);
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    public static List<CryptoSettlementMethod> getSettlementMethods(SwapProtocolType protocolType, String code) {
        switch (protocolType) {
            case MONERO_SWAP:
                return List.of(CryptoSettlementMethod.NATIVE_CHAIN);
            case LIQUID_SWAP:
                return List.of(CryptoSettlementMethod.NATIVE_CHAIN);
            case BSQ_SWAP:
                return List.of(CryptoSettlementMethod.NATIVE_CHAIN);
            case LIGHTNING_X:
                return List.of(values());
            case BISQ_MULTISIG:
                return List.of(values());
            default:
                throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        }
    }

    @Override
    public ProtocolMessageEnum toProto() {
        return null;
    }
}
