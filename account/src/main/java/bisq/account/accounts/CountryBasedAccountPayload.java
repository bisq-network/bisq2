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

import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public abstract class CountryBasedAccountPayload extends AccountPayload {
    protected final String countryCode;

    public CountryBasedAccountPayload(String id, String settlementMethodName, String countryCode) {
        super(id, settlementMethodName);
        this.countryCode = countryCode;
    }

    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder() {
        return bisq.account.protobuf.CountryBasedAccountPayload.newBuilder()
                .setCountryCode(countryCode);
    }

    public static CountryBasedAccountPayload fromProto(bisq.account.protobuf.CountryBasedAccountPayload proto) {
        switch (proto.getMessageCase()) {
            case SEPAACCOUNTPAYLOAD: {
                return SepaAccountPayload.fromProto(proto);
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}