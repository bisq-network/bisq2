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

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.PaymentAccountValidation;
import bisq.common.validation.SepaPaymentAccountValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SepaAccountPayload extends CountryBasedAccountPayload {
    public static final int HOLDER_NAME_MIN_LENGTH = 2;
    public static final int HOLDER_NAME_MAX_LENGTH = 70;

    private final String holderName;
    private final String iban;
    private final String bic;
    private final List<String> acceptedCountryCodes;

    public SepaAccountPayload(String id,
                              String holderName,
                              String iban,
                              String bic,
                              String countryCode,
                              List<String> acceptedCountryCodes) {
        super(id, countryCode);
        this.holderName = holderName;
        this.iban = iban;
        this.bic = bic;
        this.acceptedCountryCodes = acceptedCountryCodes != null ? List.copyOf(acceptedCountryCodes) : List.of();
        verify();
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateRequiredText(holderName, HOLDER_NAME_MIN_LENGTH, HOLDER_NAME_MAX_LENGTH);
        SepaPaymentAccountValidation.validateSepaIbanFormat(iban, FiatPaymentRailUtil.getAllSepaCountryCodes());
        SepaPaymentAccountValidation.validateBicFormat(bic);
        PaymentAccountValidation.validateCountryCodes(acceptedCountryCodes,
                FiatPaymentRailUtil.getAllSepaCountryCodes(),
                "SEPA payments");
        SepaPaymentAccountValidation.validateIbanCountryConsistency(iban, getCountryCode());
        acceptedCountryCodes.forEach(NetworkDataValidation::validateRequiredCode);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setSepaAccountPayload(
                toSepaAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.SepaAccountPayload toSepaAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getSepaAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SepaAccountPayload.Builder getSepaAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SepaAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setIban(iban)
                .setBic(bic)
                .addAllAcceptedCountryCodes(acceptedCountryCodes);
    }

    public static SepaAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.SepaAccountPayload sepaAccountPayload = countryBasedAccountPayload.getSepaAccountPayload();
        return new SepaAccountPayload(proto.getId(),
                sepaAccountPayload.getHolderName(),
                sepaAccountPayload.getIban(),
                sepaAccountPayload.getBic(),
                countryBasedAccountPayload.getCountryCode(),
                sepaAccountPayload.getAcceptedCountryCodesList());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
    }

    @Override
    public String getDefaultAccountName() {
        return getPaymentMethodName() + "-" + StringUtils.truncate(iban, 8);
    }
}