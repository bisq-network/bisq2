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
import bisq.account.accounts.fiat.CashByMailAccountPayload;
import bisq.account.accounts.fiat.CountryBasedAccountPayload;
import bisq.account.accounts.fiat.FasterPaymentsAccountPayload;
import bisq.account.accounts.fiat.InteracETransferAccountPayload;
import bisq.account.accounts.fiat.PayIdAccountPayload;
import bisq.account.accounts.fiat.RevolutAccountPayload;
import bisq.account.accounts.fiat.USPostalMoneyOrderAccountPayload;
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload;
import bisq.account.accounts.fiat.ZelleAccountPayload;
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

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * AccountPayload is sent over the wire to the peer during the trade process. It is not used in the offer.
 */
@Getter
@Slf4j
@ToString
@EqualsAndHashCode
public abstract class AccountPayload<M extends PaymentMethod<?>> implements NetworkProto {
    protected final String id;
    protected final String paymentMethodId;
    protected final byte[] salt;

    public AccountPayload(String id) {
        this(id, null, ByteArrayUtils.getRandomBytes(32));
    }

    public AccountPayload(String id, @Nullable String paymentMethodId, byte[] salt) {
        this.id = id;
        this.salt = salt;
        this.paymentMethodId = paymentMethodId != null ? paymentMethodId : getPaymentMethodName();
    }

    // public abstract byte[] getAgeWitnessInputData();
    //todo
    public byte[] getAgeWitnessInputData() {
        return id.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateId(id);
        NetworkDataValidation.validateText(getPaymentMethodName(), 100);
    }

    @Override
    public bisq.account.protobuf.AccountPayload toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.account.protobuf.AccountPayload completeProto() {
        return toProto(false);
    }

    protected bisq.account.protobuf.AccountPayload.Builder getAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AccountPayload.newBuilder()
                .setId(id)
                .setPaymentMethodId(paymentMethodId)
                .setSalt(ByteString.copyFrom(salt));
    }

    public static AccountPayload<?> fromProto(bisq.account.protobuf.AccountPayload proto) {
        return switch (proto.getMessageCase()) {
            case ZELLEACCOUNTPAYLOAD -> ZelleAccountPayload.fromProto(proto);
            case COUNTRYBASEDACCOUNTPAYLOAD -> CountryBasedAccountPayload.fromProto(proto);
            case REVOLUTACCOUNTPAYLOAD -> RevolutAccountPayload.fromProto(proto);
            case USERDEFINEDFIATACCOUNTPAYLOAD -> UserDefinedFiatAccountPayload.fromProto(proto);
            case FASTERPAYMENTSACCOUNTPAYLOAD -> FasterPaymentsAccountPayload.fromProto(proto);
            case PAYIDACCOUNTPAYLOAD -> PayIdAccountPayload.fromProto(proto);
            case USPOSTALMONEYORDERACCOUNTPAYLOAD -> USPostalMoneyOrderAccountPayload.fromProto(proto);
            case CASHBYMAILACCOUNTPAYLOAD -> CashByMailAccountPayload.fromProto(proto);
            case INTERACETRANSFERACCOUNTPAYLOAD -> InteracETransferAccountPayload.fromProto(proto);
            case CRYPTOASSETACCOUNTPAYLOAD -> CryptoAssetAccountPayload.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
            default -> throw new UnresolvableProtobufMessageException(proto);
        };
    }

    protected byte[] getAgeWitnessInputData(byte[] data) {
        return ByteArrayUtils.concat(paymentMethodId.getBytes(StandardCharsets.UTF_8), data);
    }

    public abstract M getPaymentMethod();

    public String getPaymentMethodName() {
        return getPaymentMethod().getPaymentRailName();
    }

    public String getDefaultAccountName() {
        return getPaymentMethodName() + "-" + StringUtils.truncate(id, 8);
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