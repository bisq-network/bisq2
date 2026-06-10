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
 * You should have received a copy of the GNU Affero General Public
 * License along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.common.view;

import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ViewLifecycleObserversTest {
    @Test
    void notifiesRegisteredObserversWhenViewIsAttached() throws Exception {
        List<View<?, ?, ?>> attachedViews = new ArrayList<>();
        AutoCloseable registration = ViewLifecycleObservers.register(new ViewLifecycleObserver() {
            @Override
            public void onViewAttached(View<?, ?, ?> view) {
                attachedViews.add(view);
            }
        });

        try {
            DummyController controller = new DummyController();
            DummyView view = new DummyView(controller);
            controller.view = view;
            ViewLifecycleObservers.onViewAttached(view);

            assertThat(attachedViews).containsExactly(view);
        } finally {
            registration.close();
        }
    }

    @Test
    void unregistersObservers() throws Exception {
        List<View<?, ?, ?>> attachedViews = new ArrayList<>();
        AutoCloseable registration = ViewLifecycleObservers.register(new ViewLifecycleObserver() {
            @Override
            public void onViewAttached(View<?, ?, ?> view) {
                attachedViews.add(view);
            }
        });
        registration.close();

        DummyController controller = new DummyController();
        DummyView view = new DummyView(controller);
        controller.view = view;
        ViewLifecycleObservers.onViewAttached(view);

        assertThat(attachedViews).isEmpty();
    }

    private static final class DummyView extends View<Pane, DummyModel, DummyController> {
        private DummyView(DummyController controller) {
            super(new Pane(), new DummyModel(), controller);
        }

        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }
    }

    private static final class DummyModel implements Model {
    }

    private static final class DummyController implements Controller {
        private DummyView view;

        @Override
        public View<?, ?, ?> getView() {
            return view;
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }
    }
}
