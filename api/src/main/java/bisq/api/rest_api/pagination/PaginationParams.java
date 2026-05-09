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

    public static PaginationParams of(Optional<Integer> page, Optional<Integer> pageSize) {
        int resolvedPage = page.filter(p -> p >= 1).orElse(DEFAULT_PAGE);
        int resolvedSize = pageSize.filter(s -> s >= 1)
                .map(s -> Math.min(s, MAX_PAGE_SIZE))
                .orElse(DEFAULT_PAGE_SIZE);
        return new PaginationParams(resolvedPage, resolvedSize);
    }

    public <T> PaginatedResponse<T> paginate(List<T> items) {
        int total = items.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        if (page > totalPages && total > 0) {
            throw new IllegalArgumentException(
                    "Page " + page + " out of range; total pages: " + totalPages);
        }
        int fromIndex = Math.min((page - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        return new PaginatedResponse<>(
                items.subList(fromIndex, toIndex),
                page,
                pageSize,
                total,
                totalPages
        );
    }
}
