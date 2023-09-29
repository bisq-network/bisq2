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

package bisq.desktop.main.content.academy.overview;

import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OverviewAcademyView extends View<GridPane, OverviewAcademyModel, OverviewAcademyController> {
    private static final int PADDING = 20;
    private int rowIndex = 0;

    public OverviewAcademyView(OverviewAcademyModel model, OverviewAcademyController controller) {
        super(new GridPane(), model, controller);

        root.setPadding(new Insets(0, 0, 0, 0));
        root.setHgap(PADDING);
        root.setVgap(PADDING);
        root.setCursor(Cursor.HAND);

        GridPaneUtil.setGridPaneTwoColumnsConstraints(root);

        addHeaderBox();

        addSmallBox("learn-bisq", "learn-bitcoin",
                "bisq", "bitcoin",
                NavigationTarget.BISQ_ACADEMY, NavigationTarget.BITCOIN_ACADEMY);
        addSmallBox("learn-wallets", "learn-security",
                "wallets", "security",
                NavigationTarget.WALLETS_ACADEMY, NavigationTarget.SECURITY_ACADEMY);
        addSmallBox("learn-privacy", "learn-openSource",
                "privacy", "foss",
                NavigationTarget.PRIVACY_ACADEMY, NavigationTarget.FOSS_ACADEMY);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    private void addHeaderBox() {
        Label headlineLabel = new Label(Res.get("academy.overview.subHeadline"));
        headlineLabel.setWrapText(true);
        headlineLabel.getStyleClass().add("bisq-text-headline-4");
        headlineLabel.setMinHeight(55);

        Label contentLabel = new Label(Res.get("academy.overview.content"));
        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("bisq-text-16");

        VBox.setVgrow(headlineLabel, Priority.ALWAYS);
        VBox.setVgrow(contentLabel, Priority.ALWAYS);
        VBox vBox = new VBox(20, headlineLabel, contentLabel);
        GridPane.setHgrow(vBox, Priority.ALWAYS);
        root.add(vBox, 0, rowIndex, 2, 1);
    }

    private void addSmallBox(String leftIconId,
                             String rightIconId,
                             String leftTopic,
                             String rightTopic,
                             NavigationTarget leftNavigationTarget,
                             NavigationTarget rightNavigationTarget) {

        Insets gridPaneInsets = new Insets(0, 0, 0, 0);
        GridPane gridPane = GridPaneUtil.getGridPane(20, 0, gridPaneInsets);
        GridPaneUtil.setGridPaneTwoColumnsConstraints(gridPane);

        Insets groupInsets = new Insets(20, 20, 20, 20);
        Insets headlineInsets = new Insets(20, 20, 0, 20);
        Insets infoInsets = new Insets(20, 20, 0, 20);
        Insets buttonInsets = new Insets(20, 20, 20, 20);

        Button leftBoxButton = new Button(Res.get("academy.overview.selectButton"));
        leftBoxButton.getStyleClass().addAll("medium-large-button",
                "outlined-button",
                "grey-outlined-button");
        leftBoxButton.setOnAction(e -> controller.onSelect(leftNavigationTarget));
        GridPaneUtil.fillColumn(gridPane,
                0,
                leftBoxButton,
                "",
                buttonInsets,
                Res.get("academy.overview." + leftTopic),
                "bisq-text-headline-2",
                leftIconId,
                headlineInsets,
                Res.get("academy.overview." + leftTopic + ".content"),
                "bisq-text-3",
                infoInsets,
                "bisq-box-2",
                groupInsets);

        Button rightBoxButton = new Button(Res.get("academy.overview.selectButton"));
        rightBoxButton.getStyleClass().addAll("medium-large-button",
                "outlined-button",
                "grey-outlined-button");
        rightBoxButton.setOnAction(e -> controller.onSelect(rightNavigationTarget));
        GridPaneUtil.fillColumn(gridPane,
                1,
                rightBoxButton,
                "",
                buttonInsets,
                Res.get("academy.overview." + rightTopic),
                "bisq-text-headline-2",
                rightIconId,
                headlineInsets,
                Res.get("academy.overview." + rightTopic + ".content"),
                "bisq-text-3",
                infoInsets,
                "bisq-box-2",
                groupInsets);
        root.add(gridPane, 0, ++rowIndex, 2, 1);
    }
}
