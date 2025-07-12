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

package bisq.account.accounts.fiat;

import bisq.account.protobuf.Account;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class MoneyBeamAccount extends CountryBasedAccount<MoneyBeamAccountPayload> {
    public MoneyBeamAccount(String id, long creationDate, String accountName, MoneyBeamAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccount.Builder getCountryBasedAccountBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountBuilder(serializeForHash).setMoneyBeamAccount(
                toMoneyBeamAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.MoneyBeamAccount toMoneyBeamAccountProto(boolean serializeForHash) {
        return resolveBuilder(getMoneyBeamAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.MoneyBeamAccount.Builder getMoneyBeamAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.MoneyBeamAccount.newBuilder();
    }

    public static MoneyBeamAccount fromProto(Account proto) {
        return new MoneyBeamAccount(proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                MoneyBeamAccountPayload.fromProto(proto.getAccountPayload()));
    }
}
