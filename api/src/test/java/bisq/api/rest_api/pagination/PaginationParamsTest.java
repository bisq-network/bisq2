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

package bisq.api.rest_api.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationParamsTest {

    private static List<Integer> range(int n) {
        return IntStream.range(0, n).boxed().toList();
    }

    @Test
    void of_appliesDefaultsWhenAbsent() {
        PaginationParams p = PaginationParams.of(Optional.empty(), Optional.empty());
        assertEquals(PaginationParams.DEFAULT_PAGE, p.page());
        assertEquals(PaginationParams.DEFAULT_PAGE_SIZE, p.pageSize());
    }

    @Test
    void of_clampsPageSizeAboveMax() {
        PaginationParams p = PaginationParams.of(Optional.of(1), Optional.of(PaginationParams.MAX_PAGE_SIZE * 10));
        assertEquals(PaginationParams.MAX_PAGE_SIZE, p.pageSize());
    }

    @Test
    void of_replacesNonPositivePageWithDefault() {
        PaginationParams p = PaginationParams.of(Optional.of(0), Optional.of(10));
        assertEquals(PaginationParams.DEFAULT_PAGE, p.page());
    }

    @Test
    void of_replacesNonPositivePageSizeWithDefault() {
        PaginationParams p = PaginationParams.of(Optional.of(1), Optional.of(0));
        assertEquals(PaginationParams.DEFAULT_PAGE_SIZE, p.pageSize());
    }

    @Test
    void paginate_emptyListReturnsEmptyPageWithoutThrowing() {
        PaginationParams p = PaginationParams.of(Optional.of(1), Optional.of(20));
        PaginatedResponse<Integer> resp = p.paginate(List.of());
        assertTrue(resp.items().isEmpty());
        assertEquals(1, resp.page());
        assertEquals(20, resp.pageSize());
        assertEquals(0L, resp.totalItems());
        assertEquals(0, resp.totalPages());
    }

    @Test
    void paginate_firstPageOfPartialLastPage() {
        PaginationParams p = PaginationParams.of(Optional.of(1), Optional.of(3));
        PaginatedResponse<Integer> resp = p.paginate(range(7));
        assertEquals(List.of(0, 1, 2), resp.items());
        assertEquals(7L, resp.totalItems());
        assertEquals(3, resp.totalPages());
    }

    @Test
    void paginate_lastPagePartial() {
        PaginationParams p = PaginationParams.of(Optional.of(3), Optional.of(3));
        PaginatedResponse<Integer> resp = p.paginate(range(7));
        assertEquals(List.of(6), resp.items());
        assertEquals(3, resp.totalPages());
    }

    @Test
    void paginate_pageEqualsTotalPagesNotThrown() {
        PaginationParams p = PaginationParams.of(Optional.of(2), Optional.of(5));
        PaginatedResponse<Integer> resp = p.paginate(range(10));
        assertEquals(List.of(5, 6, 7, 8, 9), resp.items());
        assertEquals(2, resp.totalPages());
    }

    @Test
    void paginate_pageBeyondTotalThrows() {
        PaginationParams p = PaginationParams.of(Optional.of(5), Optional.of(3));
        assertThrows(IllegalArgumentException.class, () -> p.paginate(range(7)));
    }

    @Test
    void paginate_pageAboveOneOnEmptyListDoesNotThrow() {
        // Documents the empty-corpus edge: totalPages=0 short-circuits the bounds check.
        PaginationParams p = PaginationParams.of(Optional.of(99), Optional.of(20));
        PaginatedResponse<Integer> resp = p.paginate(List.of());
        assertTrue(resp.items().isEmpty());
        assertEquals(0, resp.totalPages());
    }
}
