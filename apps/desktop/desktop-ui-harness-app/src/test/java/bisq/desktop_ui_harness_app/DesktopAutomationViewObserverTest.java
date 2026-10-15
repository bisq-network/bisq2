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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop_ui_harness_app;

import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DesktopAutomationViewObserverTest {
    @Test
    void dispatchesAttachedViewToMatchingBinder() {
        TestView view = TestView.create();
        RecordingBinder binder = new RecordingBinder();

        new DesktopAutomationViewObserver(List.of(binder)).onViewAttached(view);

        assertThat(binder.boundView).isSameAs(view);
    }

    @Test
    void ignoresUnsupportedViews() {
        TestView view = TestView.create();
        DesktopAutomationViewBinder<View<?, ?, ?>> unsupportedBinder = new DesktopAutomationViewBinder<>() {
            @Override
            public Class<View<?, ?, ?>> viewType() {
                return uncheckedViewClass();
            }

            @Override
            public void bind(View<?, ?, ?> view) {
                throw new AssertionError("unsupported binder must not be invoked");
            }
        };

        new DesktopAutomationViewObserver(List.of(unsupportedBinder)).onViewAttached(view);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Class<View<?, ?, ?>> uncheckedViewClass() {
        return (Class) UnsupportedView.class;
    }

    private static final class RecordingBinder implements DesktopAutomationViewBinder<TestView> {
        private TestView boundView;

        @Override
        public Class<TestView> viewType() {
            return TestView.class;
        }

        @Override
        public void bind(TestView view) {
            boundView = view;
        }
    }

    private static final class TestView extends View<Pane, TestModel, TestController> {
        private static TestView create() {
            TestController controller = new TestController();
            TestView view = new TestView(controller);
            controller.view = view;
            return view;
        }

        private TestView(TestController controller) {
            super(new Pane(), new TestModel(), controller);
        }

        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }
    }

    private static final class UnsupportedView extends View<Pane, TestModel, TestController> {
        private UnsupportedView(TestController controller) {
            super(new Pane(), new TestModel(), controller);
        }

        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }
    }

    private static final class TestModel implements Model {
    }

    private static final class TestController implements Controller {
        private View<? extends Parent, ? extends Model, ? extends Controller> view;

        @Override
        public View<? extends Parent, ? extends Model, ? extends Controller> getView() {
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
