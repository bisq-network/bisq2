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

package bisq.desktop.main.content.authorized_role.mediator.bisq_easy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyMediationCaseListItemTest {

    @Test
    void mostRecentLastMessageFirstFloatsActiveCasesToTop() {
        BisqEasyMediationCaseListItem oldest = itemWith(1_000L);
        BisqEasyMediationCaseListItem newest = itemWith(3_000L);
        BisqEasyMediationCaseListItem middle = itemWith(2_000L);

        List<BisqEasyMediationCaseListItem> items =
                new ArrayList<>(List.of(oldest, newest, middle));
        items.sort(BisqEasyMediationCaseListItem.BY_LAST_MESSAGE_DATE.reversed());

        assertEquals(List.of(newest, middle, oldest), items);
    }

    private static BisqEasyMediationCaseListItem itemWith(long lastMessageDate) {
        BisqEasyMediationCaseListItem item = mock(BisqEasyMediationCaseListItem.class);
        when(item.getLastMessageDate()).thenReturn(lastMessageDate);
        return item;
    }
}
