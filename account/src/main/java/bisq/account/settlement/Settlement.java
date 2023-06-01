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
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.i18n.Res;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class Settlement<M extends Settlement.Method> implements Proto {
    public static List<? extends Method> getSettlementMethods(SwapProtocolType protocolType, String currencyCode) {
        if (TradeCurrency.isFiat(currencyCode)) {
            return FiatSettlement.getSettlementMethods(protocolType);
        } else {
            if (currencyCode.equals("BTC")) {
                return BitcoinSettlement.getSettlementMethods(protocolType);
            } else {
                return CryptoSettlement.getSettlementMethods(protocolType);
            }
        }
    }

    public static Settlement<? extends Method> from(String settlementMethodName, String currencyCode) {
        if (TradeCurrency.isFiat(currencyCode)) {
            return FiatSettlement.from(settlementMethodName);
        } else {
            if (currencyCode.equals("BTC")) {
                return BitcoinSettlement.from(settlementMethodName);
            } else {
                return CryptoSettlement.from(settlementMethodName, currencyCode);
            }
        }
    }

    public static Method getSettlementMethod(String name, String currencyCode) {
        return from(name, currencyCode).getMethod();
    }

    public interface Method {
        String name();
    }

    protected final String settlementMethodName;
    protected final M method;

    public Settlement(M method) {
        this.settlementMethodName = method.name();
        this.method = method;
    }

    public Settlement(String settlementMethodName) {
        this.settlementMethodName = settlementMethodName;
        this.method = getFallbackMethod();
    }

    public abstract bisq.account.protobuf.Settlement toProto();

    protected bisq.account.protobuf.Settlement.Builder getSettlementBuilder() {
        return bisq.account.protobuf.Settlement.newBuilder()
                .setSettlementMethodName(settlementMethodName);
    }

    public static Settlement<? extends Method> fromProto(bisq.account.protobuf.Settlement proto) {
        switch (proto.getMessageCase()) {
            case FIATSETTLEMENT: {
                return FiatSettlement.fromProto(proto);
            }
            case BITCOINSETTLEMENT: {
                return BitcoinSettlement.fromProto(proto);
            }
            case CRYPTOSETTLEMENT: {
                return CryptoSettlement.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }


    protected abstract M getFallbackMethod();

    public abstract List<TradeCurrency> getTradeCurrencies();

    protected String getDisplayName(String code) {
        return Res.get(getSettlementMethodName());
    }
}
