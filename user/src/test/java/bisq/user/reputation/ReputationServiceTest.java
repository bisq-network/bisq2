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

package bisq.user.reputation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReputationServiceTest {

    @Test
    void testGetIndex() {
        List<Long> scores = List.of(1L, 2L, 3L, 4L, 5L);
        assertEquals(0, ReputationService.getIndex(1, scores));
        assertEquals(4, ReputationService.getIndex(5, scores));
        assertEquals(1, ReputationService.getIndex(2, List.of(1L, 2L, 2L, 4L, 5L)));
        assertEquals(-1, ReputationService.getIndex(0, scores));
        assertEquals(-1, ReputationService.getIndex(1, List.of()));
    }


    @Test
    void testGetRelativeScore() {
        List<Long> scores = List.of(1L, 2L, 3L, 4L, 5L);
        assertEquals(1 / 5d, ReputationService.getRelativeScore(1, scores));
        assertEquals(1, ReputationService.getRelativeScore(5, scores));
        assertEquals(5000 / 5001d, ReputationService.getRelativeScore(5000, List.of(1L, 2L, 3L, 4L, 5000L, 5001L)));
        assertEquals(0, ReputationService.getRelativeScore(0, scores));
        assertEquals(0, ReputationService.getRelativeScore(1, List.of()));
    }
}