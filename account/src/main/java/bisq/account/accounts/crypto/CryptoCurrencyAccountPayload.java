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

package bisq.account.accounts.crypto;

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.CryptoPaymentMethod;
import bisq.account.payment_method.CryptoPaymentRail;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public abstract class CryptoCurrencyAccountPayload extends AccountPayload<CryptoPaymentMethod> implements SingleCurrencyAccountPayload {
    protected final String currencyCode;
    protected final String address;
    protected final boolean isInstant;
    protected final boolean isAutoConf;

    public CryptoCurrencyAccountPayload(String id,
                                        String currencyCode,
                                        String address,
                                        boolean isInstant,
                                        boolean isAutoConf) {
        super(id);
        this.currencyCode = currencyCode;
        this.address = address;
        this.isInstant = isInstant;
        this.isAutoConf = isAutoConf;
    }

    protected bisq.account.protobuf.CryptoCurrencyAccountPayload toCryptoCurrencyAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getCryptoCurrencyAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    protected bisq.account.protobuf.CryptoCurrencyAccountPayload.Builder getCryptoCurrencyAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.CryptoCurrencyAccountPayload.newBuilder()
                .setCurrencyCode(currencyCode)
                .setAddress(address)
                .setIsInstant(isInstant)
                .setIsAutoConf(isAutoConf);
    }

    public static CryptoCurrencyAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        return switch (proto.getCryptoCurrencyAccountPayload().getMessageCase()) {
            case MONEROACCOUNTPAYLOAD -> MoneroAccountPayload.fromProto(proto);
            case OTHERCRYPTOCURRENCYACCOUNTPAYLOAD -> OtherCryptoCurrencyAccountPayload.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }

    @Override
    public CryptoPaymentMethod getPaymentMethod() {
        return CryptoPaymentMethod.fromPaymentRail(CryptoPaymentRail.NATIVE_CHAIN, currencyCode);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("user.paymentAccounts.altcoin.currencyCode"), currencyCode,
                Res.get("user.paymentAccounts.altcoin.address"), address
        ).toString();
    }
}
