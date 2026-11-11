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

package bisq.common.fsm;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Other threads read the sets of a model while its Fsm changes them: the persistence reads the event queue and the
 * processed event types, and the hash code of a model reads both sets, for example when a trade store copies its trades
 * for a write. The test changes a set while it iterates it, which is what such a read meets when the Fsm changes the
 * set at the same time.
 */
class FsmModelTest {
    @Test
    void processedEventTypesCanChangeWhileTheyAreIterated() {
        FsmModel model = new FsmModel(FsmTest.MockState.S1,
                Set.of(),
                Set.of(FsmTest.MockEvent1.class.getName(), FsmTest.MockEvent2.class.getName()));
        Iterator<String> iterator = model.processedEventTypes.iterator();
        iterator.next();

        model.processedEventTypes.add(FsmTest.MockEvent3.class.getName());

        assertDoesNotThrow(iterator::next);
        assertTrue(model.getProcessedEventTypes().contains(FsmTest.MockEvent3.class.getName()));
    }

    @Test
    void eventQueueCanChangeWhileItIsIterated() {
        FsmModel model = new FsmModel(FsmTest.MockState.S1,
                Set.of(new FsmTest.MockEvent1(null, "first"), new FsmTest.MockEvent2(null, "second")),
                Set.of());
        Iterator<Event> iterator = model.eventQueue.iterator();
        iterator.next();

        model.eventQueue.add(new FsmTest.MockEvent3(null, "third"));

        assertDoesNotThrow(iterator::next);
    }
}
