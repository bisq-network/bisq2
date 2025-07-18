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

package bisq.account.accounts.fiat;

import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
import bisq.common.validation.NetworkDataValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class PixAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    public static final int HOLDER_NAME_MIN_LENGTH = 2;
    public static final int HOLDER_NAME_MAX_LENGTH = 70;
    public static final int PIX_KEY_MIN_LENGTH = 2;
    public static final int PIX_KEY_MAX_LENGTH = 100;

    private final String holderName;
    private final String pixKey;

    public PixAccountPayload(String id, String countryCode, String holderName, String pixKey) {
        super(id, countryCode);
        this.holderName = holderName;
        this.pixKey = pixKey;
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateRequiredText(holderName, HOLDER_NAME_MIN_LENGTH, HOLDER_NAME_MAX_LENGTH);
        NetworkDataValidation.validateRequiredText(pixKey, PIX_KEY_MIN_LENGTH, PIX_KEY_MAX_LENGTH);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setPixAccountPayload(
                toPixAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.PixAccountPayload toPixAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getPixAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PixAccountPayload.Builder getPixAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PixAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setPixKey(pixKey);
    }

    public static PixAccountPayload fromProto(AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.PixAccountPayload payload = countryBasedAccountPayload.getPixAccountPayload();
        return new PixAccountPayload(proto.getId(),
                countryBasedAccountPayload.getCountryCode(),
                payload.getHolderName(),
                payload.getPixKey()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PIX);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.holderName"), holderName,
                Res.get("paymentAccounts.pix.pixKey"), pixKey
        ).toString();
    }
}
