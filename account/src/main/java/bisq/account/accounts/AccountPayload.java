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

package bisq.account.accounts;

import bisq.account.accounts.crypto.CryptoAssetAccountPayload;
import bisq.account.accounts.fiat.AdvancedCashAccountPayload;
import bisq.account.accounts.fiat.CashByMailAccountPayload;
import bisq.account.accounts.fiat.CountryBasedAccountPayload;
import bisq.account.accounts.fiat.MercadoPagoAccountPayload;
import bisq.account.accounts.fiat.MoneseAccountPayload;
import bisq.account.accounts.fiat.PayseraAccountPayload;
import bisq.account.accounts.fiat.PerfectMoneyAccountPayload;
import bisq.account.accounts.fiat.RevolutAccountPayload;
import bisq.account.accounts.fiat.SatispayAccountPayload;
import bisq.account.accounts.fiat.UpholdAccountPayload;
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload;
import bisq.account.accounts.fiat.WiseAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.ByteArrayUtils;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * AccountPayload is sent over the wire to the peer during the trade process. It is not used in the offer.
 */
@Getter
@Slf4j
@ToString
@EqualsAndHashCode
public abstract class AccountPayload<M extends PaymentMethod<?>> implements NetworkProto {
    protected final String id;
    protected final byte[] salt; // 32 bytes

    public AccountPayload(String id, byte[] salt) {
        this.id = id;
        this.salt = salt;
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateId(id);
        NetworkDataValidation.validateText(getPaymentMethodName(), 100);
    }

    @Override
    public bisq.account.protobuf.AccountPayload toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    protected bisq.account.protobuf.AccountPayload.Builder getAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AccountPayload.newBuilder()
                .setId(id)
                .setSalt(ByteString.copyFrom(salt));
    }

    public static AccountPayload<?> fromProto(bisq.account.protobuf.AccountPayload proto) {
        return switch (proto.getMessageCase()) {
            case COUNTRYBASEDACCOUNTPAYLOAD -> CountryBasedAccountPayload.fromProto(proto);
            case REVOLUTACCOUNTPAYLOAD -> RevolutAccountPayload.fromProto(proto);
            case USERDEFINEDFIATACCOUNTPAYLOAD -> UserDefinedFiatAccountPayload.fromProto(proto);
            case CASHBYMAILACCOUNTPAYLOAD -> CashByMailAccountPayload.fromProto(proto);
            case UPHOLDACCOUNTPAYLOAD -> UpholdAccountPayload.fromProto(proto);
            case ADVANCEDCASHACCOUNTPAYLOAD -> AdvancedCashAccountPayload.fromProto(proto);
            case PERFECTMONEYACCOUNTPAYLOAD -> PerfectMoneyAccountPayload.fromProto(proto);
            case MONESEACCOUNTPAYLOAD -> MoneseAccountPayload.fromProto(proto);
            case PAYSERAACCOUNTPAYLOAD -> PayseraAccountPayload.fromProto(proto);
            case SATISPAYACCOUNTPAYLOAD -> SatispayAccountPayload.fromProto(proto);
            case MERCADOPAGOACCOUNTPAYLOAD -> MercadoPagoAccountPayload.fromProto(proto);
            case WISEACCOUNTPAYLOAD -> WiseAccountPayload.fromProto(proto);
            case CRYPTOASSETACCOUNTPAYLOAD -> CryptoAssetAccountPayload.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
            default -> throw new UnresolvableProtobufMessageException(proto);
        };
    }

    public abstract byte[] getFingerprint();

    protected byte[] getFingerprint(byte[] data) {
        // paymentMethodId must match Bisq 1 paymentMethodId to support imported Bisq 1 accounts and account age
        String paymentMethodId = getBisq1CompatiblePaymentMethodId();
        return ByteArrayUtils.concat(paymentMethodId.getBytes(StandardCharsets.UTF_8), data);
    }

    protected String getBisq1CompatiblePaymentMethodId() {
        // In case Bisq 2 PaymentMethodName is different to Bisq 1, we can override it here to match the Bisq 1 ID to
        // not break account age verification.
        return getPaymentMethodName();
    }

    public abstract M getPaymentMethod();

    public String getPaymentMethodName() {
        return getPaymentMethod().getPaymentRailName();
    }

    public String getDefaultAccountName() {
        return getPaymentMethod().getShortDisplayString() + "-" + StringUtils.truncate(id, 4);
    }

    public List<String> getSelectedCurrencyCodes() {
        return switch (this) {
            case MultiCurrencyAccountPayload multiCurrencyAccountPayload ->
                    multiCurrencyAccountPayload.getSelectedCurrencyCodes();
            case SelectableCurrencyAccountPayload selectableCurrencyAccountPayload ->
                    Collections.singletonList(selectableCurrencyAccountPayload.getSelectedCurrencyCode());
            case SingleCurrencyAccountPayload singleCurrencyAccountPayload ->
                    Collections.singletonList(singleCurrencyAccountPayload.getCurrencyCode());
            default -> {
                log.error("accountPayload of unexpected type: {}", getClass().getSimpleName());
                yield List.of();
            }
        };
    }

    public abstract String getAccountDataDisplayString();
}
