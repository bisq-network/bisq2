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

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class WesternUnionAccountPayload extends CountryBasedAccountPayload {

    private final String holderName;
    private final String city;
    private final String state;
    private final String email;

    public WesternUnionAccountPayload(String id,
                                      String paymentMethodName,
                                      String countryCode,
                                      String holderName,
                                      String city,
                                      String state,
                                      String email) {
        super(id, paymentMethodName, countryCode);
        this.holderName = holderName;
        this.city = city;
        this.state = state;
        this.email = email;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(holderName, 100);
        NetworkDataValidation.validateText(city, 100);
        NetworkDataValidation.validateText(state, 50);
        NetworkDataValidation.validateText(email, 100);
    }

    public static WesternUnionAccountPayload fromProto(AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        var westernUnionPayload = countryBasedAccountPayload.getWesternUnionAccountPayload();

        return new WesternUnionAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                countryBasedAccountPayload.getCountryCode(),
                westernUnionPayload.getHolderName(),
                westernUnionPayload.getCity(),
                westernUnionPayload.getState(),
                westernUnionPayload.getEmail()
        );
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash)
                .setWesternUnionAccountPayload(toWesternUnionAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.WesternUnionAccountPayload toWesternUnionAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getWesternUnionAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.WesternUnionAccountPayload.Builder getWesternUnionAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.WesternUnionAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setCity(city)
                .setState(state)
                .setEmail(email);
    }
}