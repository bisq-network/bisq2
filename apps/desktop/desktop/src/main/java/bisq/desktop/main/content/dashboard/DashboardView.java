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

package bisq.desktop.main.content.dashboard;

import bisq.desktop.common.ManagedDuration;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.View;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class DashboardView extends View<ScrollPane, DashboardModel, DashboardController> {
    private static final Insets DEFAULT_PADDING = new Insets(30, 40, 40, 40);
    private static final Insets BANNER_PADDING = new Insets(10, 40, 40, 40);

    private final VBox vBox;
    private final GridPane bisqEasyDashBoardColumns;
    private final GridPane musigDashboardColumns;
    private Subscription isBannerVisiblePin;

    public DashboardView(DashboardModel model,
                         DashboardController controller,
                         HBox dashboardTopPanel,
                         GridPane bisqEasyDashBoardColumns,
                         GridPane musigDashboardColumns) {
        super(new ScrollPane(), model, controller);

        this.bisqEasyDashBoardColumns = bisqEasyDashBoardColumns;
        this.musigDashboardColumns = musigDashboardColumns;

        vBox = new VBox(20, dashboardTopPanel, bisqEasyDashBoardColumns, musigDashboardColumns);

        root.setFitToWidth(true);
        root.setFitToHeight(true);
        root.setContent(vBox);
    }

    @Override
    protected void onViewAttached() {
        bisqEasyDashBoardColumns.visibleProperty().bind(model.getMuSigActivated().not());
        bisqEasyDashBoardColumns.managedProperty().bind(model.getMuSigActivated().not());
        musigDashboardColumns.visibleProperty().bind(model.getMuSigActivated());
        musigDashboardColumns.managedProperty().bind(model.getMuSigActivated());

        isBannerVisiblePin = EasyBind.subscribe(model.getIsBannerVisible(), visible -> {
            if (!visible) {
                UIScheduler.run(() -> vBox.setPadding(DEFAULT_PADDING))
                        .after(ManagedDuration.getNotificationPanelDurationMillis());
            } else {
                vBox.setPadding(BANNER_PADDING);
            }
        });
    }

    @Override
    protected void onViewDetached() {
        bisqEasyDashBoardColumns.visibleProperty().unbind();
        bisqEasyDashBoardColumns.managedProperty().unbind();
        musigDashboardColumns.visibleProperty().unbind();
        musigDashboardColumns.managedProperty().unbind();

        isBannerVisiblePin.unsubscribe();
    }
}
