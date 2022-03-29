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

    public static List<CryptoSettlementMethod> getSettlementMethods(SwapProtocolType protocolType, String code) {
        return switch (protocolType) {
            case BTC_XMR_SWAP -> List.of(CryptoSettlementMethod.NATIVE_CHAIN);
            case LIQUID_SWAP -> List.of(CryptoSettlementMethod.NATIVE_CHAIN);
            case BSQ_SWAP -> List.of(CryptoSettlementMethod.NATIVE_CHAIN);
            case LN_SWAP -> List.of(values());
            case MULTISIG -> List.of(values());
            case BSQ_BOND -> List.of(values());
            case REPUTATION -> List.of(values());
        };
    }

    @Override
    public ProtocolMessageEnum toProto() {
        return null;
    }
}
