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

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class UserDefinedFiatAccountPayload extends AccountPayload {
    public static final int MAX_DATA_LENGTH = 1000;
    private final String accountData;

    public UserDefinedFiatAccountPayload(String id, String paymentMethodName, String accountData) {
        super(id, paymentMethodName);
        checkArgument(accountData.length() <= MAX_DATA_LENGTH);
        this.accountData = accountData;
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setUserDefinedFiatAccountPayload(toUserDefinedFiatAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.UserDefinedFiatAccountPayload toUserDefinedFiatAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getUserDefinedFiatAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.UserDefinedFiatAccountPayload.Builder getUserDefinedFiatAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.UserDefinedFiatAccountPayload.newBuilder()
                .setAccountData(accountData);
    }

    public static UserDefinedFiatAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        return new UserDefinedFiatAccountPayload(proto.getId(), proto.getPaymentMethodName(), proto.getUserDefinedFiatAccountPayload().getAccountData());
    }
}