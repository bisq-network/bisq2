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

import bisq.account.settlement.Settlement;
import bisq.common.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Date;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode
public abstract class Account<T extends Settlement.Method> implements Serializable {
    private final String id;
    private final long creationDate;
    private final String accountName;
    private final AccountPayload payload;
    private final T settlementMethod;

    public Account(String accountName, T settlementMethod, AccountPayload payload) {
        this(StringUtils.createUid(), new Date().getTime(), accountName, settlementMethod, payload);
    }

    public Account(String id, long creationDate, String accountName, T settlementMethod, AccountPayload payload) {
        this.id = id;
        this.creationDate = creationDate;
        this.accountName = accountName;
        this.payload = payload;
        this.settlementMethod = settlementMethod;
    }
}