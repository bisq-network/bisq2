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

package bisq.desktop.main.content;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.*;
import bisq.desktop.main.content.chat.BaseChatView;
import bisq.desktop.main.notification.NotificationPanelView;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public abstract class ContentTabView<M extends ContentTabModel, C extends ContentTabController<M>> extends TabView<M, C> {
    private static final Insets NOTIFICATION_PADDING = new Insets(0, 40, 0, 40);
    private Subscription isNotificationVisiblePin;

    public ContentTabView(M model, C controller) {
        super(model, controller);
    }

    @Override
    protected void setupTopBox() {
        super.setupTopBox();

        topBox.setPadding(model.getIsNotificationVisible().get() ? NOTIFICATION_PADDING : TabView.DEFAULT_TOP_PANE_PADDING);
    }

    @Override
    protected void setupLineAndMarker() {
        super.setupLineAndMarker();

        lineSidePadding = SIDE_PADDING;
        VBox.setMargin(lineAndMarker, new Insets(0, lineSidePadding, 20, lineSidePadding));
    }

    @Override
    protected void onViewAttached() {
        isNotificationVisiblePin = EasyBind.subscribe(model.getIsNotificationVisible(), visible -> {
            if (!visible) {
                UIScheduler.run(() -> topBox.setPadding(TabView.DEFAULT_TOP_PANE_PADDING))
                        .after(NotificationPanelView.DURATION);
            } else {
                topBox.setPadding(NOTIFICATION_PADDING);
            }
        });
    }

    @Override
    protected void onViewDetached() {
        isNotificationVisiblePin.unsubscribe();
    }

    @Override
    protected double getSelectionMarkerX(TabButton selectedTabButton) {
        return super.getSelectionMarkerX(selectedTabButton) - SIDE_PADDING;
    }

    @Override
    protected boolean useFitToHeight(View<? extends Parent, ? extends Model, ? extends Controller> childView) {
        return childView instanceof BaseChatView;
    }
}