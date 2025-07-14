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

package bisq.account.accounts.crypto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class MoneroAccount extends CryptoCurrencyAccount<MoneroAccountPayload> {
    public MoneroAccount(String id, long creationDate, String accountName, MoneroAccountPayload accountPayload) {
        super(id, creationDate, accountName, accountPayload);
    }

    @Override
    public bisq.account.protobuf.Account.Builder getBuilder(boolean serializeForHash) {
        bisq.account.protobuf.CryptoCurrencyAccount.Builder builder = getCryptoCurrencyAccountBuilder(serializeForHash)
                .setMoneroAccount(getMoneroAccountBuilder(serializeForHash));
        bisq.account.protobuf.CryptoCurrencyAccount cryptoCurrencyAccount = resolveBuilder(builder, serializeForHash).build();
        return getAccountBuilder(serializeForHash)
                .setCryptoCurrencyAccount(cryptoCurrencyAccount);
    }

    private bisq.account.protobuf.MoneroAccount.Builder getMoneroAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.MoneroAccount.newBuilder();
    }

    public static MoneroAccount fromProto(bisq.account.protobuf.Account proto) {
        var cryptoCurrency = proto.getCryptoCurrencyAccount();
        var monero = cryptoCurrency.getMoneroAccount();
        return new MoneroAccount(
                proto.getId(),
                proto.getCreationDate(),
                proto.getAccountName(),
                MoneroAccountPayload.fromProto(proto.getAccountPayload())
        );
    }
}