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
import bisq.account.accounts.SelectableCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.ByteArrayUtils;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.PaymentAccountValidation;
import bisq.i18n.Res;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

@Getter
public class F2FAccountPayload extends CountryBasedAccountPayload implements SelectableCurrencyAccountPayload {
    public static final int CITY_MIN_LENGTH = 2;
    public static final int CITY_MAX_LENGTH = 50;
    public static final int CONTACT_MIN_LENGTH = 2;
    public static final int CONTACT_MAX_LENGTH = 100;
    public static final int EXTRA_INFO_MAX_LENGTH = 300;

    private final String selectedCurrencyCode;
    private final String city;
    private final String contact;
    private final String extraInfo;

    public F2FAccountPayload(String id,
                             String countryCode,
                             String selectedCurrencyCode,
                             String city,
                             String contact,
                             String extraInfo) {
        this(id,
                AccountUtils.generateSalt(),
                countryCode,
                selectedCurrencyCode,
                city,
                contact,
                extraInfo);
    }

    public F2FAccountPayload(String id,
                              byte[] salt,
                              String countryCode,
                              String selectedCurrencyCode,
                              String city,
                              String contact,
                              String extraInfo) {
        super(id, salt, countryCode);
        this.selectedCurrencyCode = selectedCurrencyCode;
        this.city = city;
        this.contact = contact;
        this.extraInfo = extraInfo;
        verify();
    }

    @Override
    public void verify() {
        super.verify();
        PaymentAccountValidation.validateCurrencyCode(selectedCurrencyCode);
        NetworkDataValidation.validateRequiredText(city, CITY_MIN_LENGTH, CITY_MAX_LENGTH);
        NetworkDataValidation.validateRequiredText(contact, CONTACT_MIN_LENGTH, CONTACT_MAX_LENGTH);
        NetworkDataValidation.validateText(extraInfo, EXTRA_INFO_MAX_LENGTH);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setF2FAccountPayload(
                toF2FAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.F2FAccountPayload toF2FAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getF2FAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.F2FAccountPayload.Builder getF2FAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.F2FAccountPayload.newBuilder()
                .setSelectedCurrencyCode(selectedCurrencyCode)
                .setCity(city)
                .setContact(contact)
                .setExtraInfo(extraInfo);
    }

    public static F2FAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.F2FAccountPayload payload = countryBasedAccountPayload.getF2FAccountPayload();
        return new F2FAccountPayload(proto.getId(),
                proto.getSalt().toByteArray(),
                countryBasedAccountPayload.getCountryCode(),
                payload.getSelectedCurrencyCode(),
                payload.getCity(),
                payload.getContact(),
                payload.getExtraInfo()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.F2F);
    }

    @Override
    public String getDefaultAccountName() {
        return getPaymentMethodName() + "-" + countryCode + "/" + StringUtils.truncate(city, 5);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.f2f.city"), city,
                Res.get("paymentAccounts.f2f.contact"), contact,
                Res.get("paymentAccounts.f2f.extraInfo"), extraInfo
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        byte[] data = ByteArrayUtils.concat(contact.getBytes(StandardCharsets.UTF_8),
                city.getBytes(StandardCharsets.UTF_8));
        return super.getFingerprint(data);
    }
}
