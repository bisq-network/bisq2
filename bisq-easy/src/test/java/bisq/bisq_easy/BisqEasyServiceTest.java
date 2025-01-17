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

package bisq.bisq_easy;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


public class BisqEasyServiceTest {
    @Test
    public void testIsAccountDataBanned() {
        Set<String> bannedAccountDataSet = new HashSet<>();
        String sellersAccountData;

        sellersAccountData="";
        assertFalse(BisqEasyService.isAccountDataBanned(bannedAccountDataSet, sellersAccountData));

        bannedAccountDataSet = Set.of(" | , ");
        sellersAccountData="Name: Peter Tosh\nIBAN:123456789";
        assertFalse(BisqEasyService.isAccountDataBanned(bannedAccountDataSet, sellersAccountData));

        sellersAccountData="abc";
        assertFalse(BisqEasyService.isAccountDataBanned(bannedAccountDataSet, sellersAccountData));

        sellersAccountData="Name: Peter Tosh\nIBAN:123456789, BIC:3333";
        assertFalse(BisqEasyService.isAccountDataBanned(bannedAccountDataSet, sellersAccountData));

        sellersAccountData="Name: Peter Tosh, Paula Jam\nIBAN:123456789 | BIC:3333";
        assertFalse(BisqEasyService.isAccountDataBanned(bannedAccountDataSet, sellersAccountData));

        bannedAccountDataSet = Set.of("Peter Tosh,123456789");
        sellersAccountData="Name: Peter Tosh\nIBAN:123456789, BIC:3333";
        assertTrue(BisqEasyService.isAccountDataBanned(bannedAccountDataSet, sellersAccountData));

        bannedAccountDataSet = Set.of("Peter Tosh,123456789");
        sellersAccountData="Name: Peter Tosh, Paula Jam\nIBAN:123456789 | BIC:3333";
        assertTrue(BisqEasyService.isAccountDataBanned(bannedAccountDataSet, sellersAccountData));

        bannedAccountDataSet = Set.of("Paula Jam, 3333");
        sellersAccountData="Name: Peter Tosh, Paula Jam\nIBAN:123456789 | BIC:3333";
        assertTrue(BisqEasyService.isAccountDataBanned(bannedAccountDataSet, sellersAccountData));

        bannedAccountDataSet = Set.of("Paula Jam  ,  3333");
        sellersAccountData="Name: Peter Tosh, Paula Jam\nIBAN:123456789 | BIC:3333";
        assertTrue(BisqEasyService.isAccountDataBanned(bannedAccountDataSet, sellersAccountData));

        bannedAccountDataSet = Set.of("Tim Burns,98989|123456789|");
        sellersAccountData="Name: Peter Tosh, Paula Jam\nIBAN:123456789 | BIC:3333";
        assertTrue(BisqEasyService.isAccountDataBanned(bannedAccountDataSet, sellersAccountData));

        bannedAccountDataSet = Set.of("Tim Burns,98989| Tosh |");
        sellersAccountData="Name: Peter Tosh, Paula Jam\nIBAN:123456789 | BIC:3333";
        assertTrue(BisqEasyService.isAccountDataBanned(bannedAccountDataSet, sellersAccountData));
    }
}
