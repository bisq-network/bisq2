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

import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * AccountPayload is sent over the wire to the peer. It must not contain sensitive data.
 */
@Getter
@Slf4j
@ToString
@EqualsAndHashCode
public abstract class AccountPayload implements Proto {
    protected final String id;
    private final String settlementMethodName;

    public AccountPayload(String id, String settlementMethodName) {
        this.id = id;
        this.settlementMethodName = settlementMethodName;
    }

    public abstract bisq.account.protobuf.AccountPayload toProto();

    protected bisq.account.protobuf.AccountPayload.Builder getAccountPayloadBuilder() {
        return bisq.account.protobuf.AccountPayload.newBuilder()
                .setId(id)
                .setSettlementMethodName(settlementMethodName);
    }

    public static AccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        switch (proto.getMessageCase()) {
            case USERDEFINEDFIATACCOUNTPAYLOAD: {
                return UserDefinedFiatAccountPayload.fromProto(proto);
            }
            case REVOLUTACCOUNTPAYLOAD: {
                return RevolutAccountPayload.fromProto(proto);
            }
            case COUNTRYBASEDACCOUNTPAYLOAD: {
                return CountryBasedAccountPayload.fromProto(proto.getCountryBasedAccountPayload());
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}