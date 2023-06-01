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
import bisq.common.currency.TradeCurrency;
import bisq.common.proto.ProtoEnum;
import bisq.i18n.Res;

import java.util.List;

public interface SettlementMethod extends ProtoEnum {
    String name();

    default String getDisplayName(String code) {
        return Res.get(name());
    }

    static List<? extends SettlementMethod> from(SwapProtocolType protocolType, String code) {
        if (TradeCurrency.isFiat(code)) {
            return FiatSettlementMethod.getSettlementMethods(protocolType);
        } else {
            if (code.equals("BTC")) {
                return BitcoinSettlementMethod.getSettlementMethods(protocolType);
            } else {
                return CryptoSettlementMethod.getSettlementMethods(protocolType, code);
            }
        }
    }

    static SettlementMethod from(String settlementMethodName, String code) {
        if (TradeCurrency.isFiat(code)) {
            return FiatSettlementMethod.valueOf(settlementMethodName);
        } else {
            if (code.equals("BTC")) {
                return BitcoinSettlementMethod.valueOf(settlementMethodName);
            } else {
                return CryptoSettlementMethod.valueOf(settlementMethodName);
            }
        }
    }
}
