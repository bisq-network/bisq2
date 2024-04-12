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
    void testGetFiveSystemScore() {
        assertEquals(0, ReputationService.getFiveSystemScore(0));
        assertEquals(0.5, ReputationService.getFiveSystemScore(1_200));
        assertEquals(1, ReputationService.getFiveSystemScore(5_000));
        assertEquals(1.5, ReputationService.getFiveSystemScore(15_000));
        assertEquals(2, ReputationService.getFiveSystemScore(20_000));
        assertEquals(2.5, ReputationService.getFiveSystemScore(25_000));
        assertEquals(3, ReputationService.getFiveSystemScore(30_000));
        assertEquals(3.5, ReputationService.getFiveSystemScore(35_000));
        assertEquals(4, ReputationService.getFiveSystemScore(40_000));
        assertEquals(4.5, ReputationService.getFiveSystemScore(60_000));
        assertEquals(5, ReputationService.getFiveSystemScore(100_000));
    }
}
