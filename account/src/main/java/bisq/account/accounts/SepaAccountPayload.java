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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SepaAccountPayload extends CountryBasedAccountPayload {
    private final String holderName;
    private final String iban;
    private final String bic;

    public SepaAccountPayload(String id, String paymentMethodName, String holderName, String iban, String bic, String countryCode) {
        super(id, paymentMethodName, countryCode);
        this.holderName = holderName;
        this.iban = iban;
        this.bic = bic;
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
                .setBic(bic);
    }

    public static SepaAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        bisq.account.protobuf.CountryBasedAccountPayload countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        bisq.account.protobuf.SepaAccountPayload sepaAccountPayload = countryBasedAccountPayload.getSepaAccountPayload();
        return new SepaAccountPayload(proto.getId(),
                proto.getPaymentMethodName(),
                sepaAccountPayload.getHolderName(),
                sepaAccountPayload.getIban(),
                sepaAccountPayload.getBic(),
                countryBasedAccountPayload.getCountryCode());
    }
}