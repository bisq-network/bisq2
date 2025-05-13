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

import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public abstract class IfscBasedAccountPayload extends CountryBasedAccountPayload {
    private final String holderName;
    private final String accountNr;
    private final String ifsc; // Indian Financial System Code

    protected IfscBasedAccountPayload(String id,
                                      String paymentMethodName,
                                      String countryCode,
                                      String holderName,
                                      String accountNr,
                                      String ifsc) {
        super(id, paymentMethodName, countryCode);
        this.holderName = holderName;
        this.accountNr = accountNr;
        this.ifsc = ifsc;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(holderName, 100);
        NetworkDataValidation.validateText(accountNr, 50);
        // https://ifsc.bankifsccode.com/
        // IFSC codes are 11 characters
        NetworkDataValidation.validateText(ifsc, 11);
    }

    protected bisq.account.protobuf.IfscBasedAccountPayload.Builder getIfscBasedAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.IfscBasedAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setAccountNr(accountNr)
                .setIfsc(ifsc);
    }

    protected bisq.account.protobuf.IfscBasedAccountPayload toIfscBasedAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getIfscBasedAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setIfscBasedAccountPayload(toIfscBasedAccountPayloadProto(serializeForHash));
    }
}