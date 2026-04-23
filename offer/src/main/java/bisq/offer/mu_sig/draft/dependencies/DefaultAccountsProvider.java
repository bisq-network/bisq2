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

package bisq.offer.mu_sig.draft.dependencies;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.common.market.Market;

import java.util.List;

public class DefaultAccountsProvider implements AccountsProvider {
    private final AccountService accountService;

    public DefaultAccountsProvider(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public List<Account<?, ?>> findAccountsForMarket(Market market) {
        return accountService.findAccountsForMarket(market);
    }
}

