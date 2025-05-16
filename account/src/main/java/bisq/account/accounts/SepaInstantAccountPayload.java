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

import bisq.account.protobuf.AccountPayload;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SepaInstantAccountPayload extends CountryBasedAccountPayload {
    private final String holderName;
    private final String iban;
    private final String bic;
    private final List<String> acceptedCountryCodes;

    public SepaInstantAccountPayload(String id,
                                     String paymentMethodName,
                                     String holderName,
                                     String iban,
                                     String bic,
                                     String countryCode,
                                     List<String> acceptedCountryCodes) {
        super(id, paymentMethodName, countryCode);
        this.holderName = holderName;
        this.iban = iban;
        this.bic = bic;
        this.acceptedCountryCodes = acceptedCountryCodes != null ? new ArrayList<>(acceptedCountryCodes) : new ArrayList<>();

        verify();
    }

    public static SepaInstantAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var sepaInstantPayload = countryBasedAccountPayload.getSepaInstantAccountPayload();

        return new SepaInstantAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                sepaInstantPayload.getHolderName(),
                sepaInstantPayload.getIban(),
                sepaInstantPayload.getBic(),
                countryBasedAccountPayload.getCountryCode(),
                sepaInstantPayload.getAcceptedCountryCodesList()
        );
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateRequiredText(holderName, 100);
        NetworkDataValidation.validateIbanFormat(iban);
        NetworkDataValidation.validateBicFormat(bic);
        acceptedCountryCodes.forEach(NetworkDataValidation::validateRequiredCode);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setSepaInstantAccountPayload(toSepaInstantAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.SepaInstantAccountPayload toSepaInstantAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getSepaInstantAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SepaInstantAccountPayload.Builder getSepaInstantAccountPayloadBuilder(boolean serializeForHash) {
        bisq.account.protobuf.SepaInstantAccountPayload.Builder builder = bisq.account.protobuf.SepaInstantAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setIban(iban)
                .setBic(bic);
        builder.addAllAcceptedCountryCodes(acceptedCountryCodes);
        return builder;
    }
}