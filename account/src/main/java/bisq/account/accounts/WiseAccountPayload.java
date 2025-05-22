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

import java.util.Optional;

import static bisq.common.util.OptionalUtils.normalize;
import static bisq.common.util.StringUtils.toOptional;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class WiseAccountPayload extends CountryBasedAccountPayload {

    private final String email;
    private final Optional<String> holderName;
    private final Optional<String> beneficiaryAddress;

    public WiseAccountPayload(String id,
                              String paymentMethodName,
                              String countryCode,
                              String email,
                              Optional<String> holderName,
                              Optional<String> beneficiaryAddress) {
        super(id, paymentMethodName, countryCode);
        this.email = email;
        this.holderName = normalize(holderName);
        this.beneficiaryAddress = normalize(beneficiaryAddress);

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateEmail(email);
        NetworkDataValidation.validateText(holderName, 100);
        NetworkDataValidation.validateText(beneficiaryAddress, 200);

        // both fields must be either present or absent
        if (holderName.isPresent() != beneficiaryAddress.isPresent()) {
            throw new IllegalArgumentException("Both holder name and beneficiary address must be either present or absent");
        }
    }

    public static WiseAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var wisePayload = countryBasedAccountPayload.getWiseAccountPayload();

        return new WiseAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                wisePayload.getEmail(),
                toOptional(wisePayload.getHolderName()),
                toOptional(wisePayload.getBeneficiaryAddress())
        );
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setWiseAccountPayload(toWiseAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.WiseAccountPayload toWiseAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getWiseAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.WiseAccountPayload.Builder getWiseAccountPayloadBuilder(boolean serializeForHash) {
        var builder = bisq.account.protobuf.WiseAccountPayload.newBuilder()
                .setEmail(email);
        holderName.ifPresent(builder::setHolderName);
        beneficiaryAddress.ifPresent(builder::setBeneficiaryAddress);
        return builder;
    }
}