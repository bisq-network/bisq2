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

package bisq.desktop.common.view;

import bisq.desktop.navigation.NavigationTarget;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the navigation dispatch and child-target resolution behind #4114, where navigating to the
 * private chat (CHAT_PRIVATE) landed in the public channel. Root cause: dispatch iterated the
 * unordered controllers map, so the lazily-created CHAT host could register after the dispatch had
 * already passed it and never received the target. Dispatch now follows the navigation path
 * (parents before children) so a host created by its parent mid-dispatch is reached afterwards.
 * Also covers the history-recording behaviour behind #4821 (automatic tab selection must not push a
 * history entry).
 */
class NavigationControllerTest {

    // --- history: automatic tab selection dispatches but must not record a history entry (#4821) ---

    @Test
    void navigateToWithoutAddingToHistoryDoesNotAffectBackNavigation() {
        List<NavigationTarget> processOrder = new ArrayList<>();
        RecordingController chat = new RecordingController(NavigationTarget.CHAT, processOrder);
        Navigation.addNavigationController(NavigationTarget.CHAT, chat);
        try {
            // A user-initiated navigation records a history entry...
            Navigation.navigateTo(NavigationTarget.CHAT_PRIVATE);
            // ...while an automatic tab selection dispatches the target but must not record one.
            Navigation.navigateToWithoutAddingToHistory(NavigationTarget.CHAT_DISCUSSION);

            // Both were dispatched to the host.
            assertEquals(List.of(NavigationTarget.CHAT_PRIVATE, NavigationTarget.CHAT_DISCUSSION),
                    chat.received);

            // Back returns to the recorded target (CHAT_PRIVATE), proving the automatic selection
            // (CHAT_DISCUSSION) never entered the history.
            Navigation.back();
            assertEquals(List.of(NavigationTarget.CHAT_PRIVATE, NavigationTarget.CHAT_DISCUSSION,
                    NavigationTarget.CHAT_PRIVATE), chat.received);
        } finally {
            Navigation.removeNavigationController(NavigationTarget.CHAT, chat);
        }
    }

    // --- dispatch ordering: a host registered by its parent mid-dispatch must still be reached ---

    @Test
    void dispatchReachesHostRegisteredLazilyByItsParentDuringDispatch() {
        List<NavigationTarget> processOrder = new ArrayList<>();
        RecordingController content = new RecordingController(NavigationTarget.CONTENT, processOrder);
        RecordingController chat = new RecordingController(NavigationTarget.CHAT, processOrder);
        // CONTENT lazily creates and registers the CHAT host while being processed, mirroring
        // ContentController creating the Chat controller on demand (#4114).
        content.onProcess = () -> Navigation.addNavigationController(NavigationTarget.CHAT, chat);

        Navigation.addNavigationController(NavigationTarget.CONTENT, content);
        try {
            Navigation.navigateTo(NavigationTarget.CHAT_PRIVATE);

            // CHAT registered only after CONTENT ran, yet path-order dispatch still delivered the
            // target to it, and parents were processed before children.
            assertEquals(List.of(NavigationTarget.CHAT_PRIVATE), chat.received);
            assertEquals(List.of(NavigationTarget.CONTENT, NavigationTarget.CHAT), processOrder);
        } finally {
            Navigation.removeNavigationController(NavigationTarget.CONTENT, content);
            Navigation.removeNavigationController(NavigationTarget.CHAT, chat);
        }
    }

    // --- resolveChildTarget: which segment of a target's path sits directly under a host ---

    @Test
    void resolveChildTarget_directLeafUnderHost() {
        assertEquals(Optional.of(NavigationTarget.CHAT_PRIVATE),
                NavigationController.resolveChildTarget(NavigationTarget.CHAT_PRIVATE, NavigationTarget.CHAT));
    }

    @Test
    void resolveChildTarget_intermediateSegmentUnderHost() {
        // Under CONTENT, the segment of CHAT_PRIVATE's path is CHAT.
        assertEquals(Optional.of(NavigationTarget.CHAT),
                NavigationController.resolveChildTarget(NavigationTarget.CHAT_PRIVATE, NavigationTarget.CONTENT));
    }

    @Test
    void resolveChildTarget_publicLeafUnderHost() {
        assertEquals(Optional.of(NavigationTarget.CHAT_DISCUSSION),
                NavigationController.resolveChildTarget(NavigationTarget.CHAT_DISCUSSION, NavigationTarget.CHAT));
    }

    @Test
    void resolveChildTarget_hostNotOnPath_empty() {
        assertEquals(Optional.empty(),
                NavigationController.resolveChildTarget(NavigationTarget.CHAT_PRIVATE, NavigationTarget.DASHBOARD));
    }

    @Test
    void resolveChildTarget_targetEqualsHost_empty() {
        assertEquals(Optional.empty(),
                NavigationController.resolveChildTarget(NavigationTarget.CHAT, NavigationTarget.CHAT));
    }

    /**
     * Records the targets it receives and the order in which hosts are processed, and optionally runs
     * a hook to simulate a parent lazily registering a child host mid-dispatch. processNavigationTarget
     * is overridden to bypass the real model/view wiring, keeping the test free of JavaFX.
     */
    private static class RecordingController extends NavigationController {
        private final List<NavigationTarget> received = new ArrayList<>();
        private final List<NavigationTarget> processOrder;
        private Runnable onProcess;

        private RecordingController(NavigationTarget host, List<NavigationTarget> processOrder) {
            super(host);
            this.processOrder = processOrder;
        }

        @Override
        void processNavigationTarget(NavigationTarget navigationTarget, Optional<Object> data) {
            received.add(navigationTarget);
            processOrder.add(host);
            if (onProcess != null) {
                onProcess.run();
            }
        }

        @Override
        protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
            return Optional.empty();
        }

        @Override
        protected NavigationModel getModel() {
            return null;
        }

        @Override
        public View<? extends Parent, ? extends Model, ? extends Controller> getView() {
            return null;
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }
    }
}
