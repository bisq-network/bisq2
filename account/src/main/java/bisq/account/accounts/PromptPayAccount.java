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
public final class PromptPayAccount extends CountryBasedAccount<PromptPayAccountPayload> {
    public PromptPayAccount(String id, long creationDate, String accountName, PromptPayAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setPromptPayAccount(
                toPromptPayAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.PromptPayAccount toPromptPayAccountProto(boolean serializeForHash) {
        return resolveBuilder(getPromptPayAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PromptPayAccount.Builder getPromptPayAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PromptPayAccount.newBuilder();
    }

    public static PromptPayAccount fromProto(bisq.account.protobuf.Account proto) {
        return new PromptPayAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                PromptPayAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
