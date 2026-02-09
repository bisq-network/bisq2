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

import bisq.account.accounts.AccountUtils;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.validation.PhoneNumberValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class HalCashAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {

    private final String mobileNr;

    public HalCashAccountPayload(String id, String countryCode, String mobileNr) {
        this(id, AccountUtils.generateSalt(), countryCode, mobileNr);
    }

    private HalCashAccountPayload(String id, byte[] salt, String countryCode, String mobileNr) {
        super(id, salt, countryCode);
        this.mobileNr = mobileNr;
    }

    @Override
    public void verify() {
        super.verify();

        checkArgument(PhoneNumberValidation.isValid(mobileNr, "ES"));
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setHalCashAccountPayload(
                toHalCashAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.HalCashAccountPayload toHalCashAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getHalCashAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.HalCashAccountPayload.Builder getHalCashAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.HalCashAccountPayload.newBuilder()
                .setMobileNr(mobileNr);
    }

    public static HalCashAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.HalCashAccountPayload payload = countryBasedAccountPayload.getHalCashAccountPayload();
        return new HalCashAccountPayload(proto.getId(),
                proto.getSalt().toByteArray(),
                countryBasedAccountPayload.getCountryCode(),
                payload.getMobileNr()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.HAL_CASH);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.mobileNr"), mobileNr
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        byte[] data = mobileNr.getBytes(StandardCharsets.UTF_8);
        return super.getFingerprint(data);
    }
}
