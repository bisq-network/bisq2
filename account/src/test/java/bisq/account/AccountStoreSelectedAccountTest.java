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

package bisq.account;

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.timestamp.KeyType;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AccountStoreSelectedAccountTest {
    private final UserDefinedFiatAccount account = new UserDefinedFiatAccount("id",
            System.currentTimeMillis(),
            "My account",
            new UserDefinedFiatAccountPayload("payloadId", "account data"),
            KeyGeneration.generateDefaultEcKeyPair(),
            KeyType.EC,
            AccountOrigin.BISQ2_NEW);

    // The selected account is a second copy of a stored account. If that copy cannot be read, for example after a
    // validation rule became stricter, the store must keep all accounts and the selection
    @Test
    void unreadableCopyOfSelectedAccountDoesNotMakeTheStoreUnreadable() throws Exception {
        bisq.account.protobuf.AccountStore proto = new AccountStore(Map.of(account.getAccountName(), account),
                Optional.of(account)).toProto(false).toBuilder()
                .setSelectedAccount(account.toProto(false).toBuilder().clearMessage())
                .build();

        AccountStore store = AccountStore.fromProto(proto);

        Account<? extends PaymentMethod<?>, ?> storedAccount = store.getAccountByName().get(account.getAccountName());
        assertEquals(account.getId(), storedAccount.getId());
        assertSame(storedAccount, store.getSelectedAccount().get());
    }

    @Test
    void selectionOfAnAccountWhichIsNotStoredIsDropped() throws Exception {
        bisq.account.protobuf.AccountStore proto = new AccountStore(Map.<String, Account<? extends PaymentMethod<?>, ?>>of(),
                Optional.of(account)).toProto(false);

        AccountStore store = AccountStore.fromProto(proto);

        assertNull(store.getSelectedAccount().get());
    }
}
