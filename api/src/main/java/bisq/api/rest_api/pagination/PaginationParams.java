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

import java.util.List;
import java.util.Optional;

public record PaginationParams(int page, int pageSize) {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Defaults apply only when a parameter is absent. An explicitly supplied value below 1 is a
     * client error and is rejected (the endpoints map {@link IllegalArgumentException} to 400)
     * rather than silently replaced — silent replacement would hide client bugs behind page 1.
     */
    public static PaginationParams of(Optional<Integer> page, Optional<Integer> pageSize) {
        page.filter(p -> p < 1).ifPresent(p -> {
            throw new IllegalArgumentException("page must be >= 1, got: " + p);
        });
        pageSize.filter(s -> s < 1).ifPresent(s -> {
            throw new IllegalArgumentException("pageSize must be >= 1, got: " + s);
        });
        int resolvedPage = page.orElse(DEFAULT_PAGE);
        int resolvedSize = pageSize.map(s -> Math.min(s, MAX_PAGE_SIZE)).orElse(DEFAULT_PAGE_SIZE);
        return new PaginationParams(resolvedPage, resolvedSize);
    }

    public <T> PaginatedResponse<T> paginate(List<T> items) {
        int total = items.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        if (page > totalPages && total > 0) {
            throw new IllegalArgumentException(
                    "Page " + page + " out of range; total pages: " + totalPages);
        }
        // Long arithmetic: with total == 0 the out-of-range guard above does not fire, and an
        // extreme page value would overflow int multiplication into a negative fromIndex.
        int fromIndex = (int) Math.min((long) (page - 1) * pageSize, total);
        int toIndex = (int) Math.min((long) fromIndex + pageSize, total);
        return new PaginatedResponse<>(
                items.subList(fromIndex, toIndex),
                page,
                pageSize,
                total,
                totalPages
        );
    }
}
