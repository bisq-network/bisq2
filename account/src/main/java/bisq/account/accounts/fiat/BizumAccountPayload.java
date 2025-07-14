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
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
import bisq.common.validation.PhoneNumberValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class BizumAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    private final String mobileNr;

    public BizumAccountPayload(String id, String countryCode, String mobileNr) {
        super(id, countryCode);
        this.mobileNr = mobileNr;
    }

    @Override
    public void verify() {
        super.verify();

        checkArgument(PhoneNumberValidation.isValid(mobileNr, "ES"));
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setBizumAccountPayload(
                toBizumAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.BizumAccountPayload toBizumAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getBizumAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.BizumAccountPayload.Builder getBizumAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.BizumAccountPayload.newBuilder().setMobileNr(mobileNr);
    }

    public static BizumAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        return new BizumAccountPayload(
                proto.getId(),
                countryBasedAccountPayload.getCountryCode(),
                countryBasedAccountPayload.getBizumAccountPayload().getMobileNr());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.BIZUM);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.mobileNr"), mobileNr
        ).toString();
    }
}
