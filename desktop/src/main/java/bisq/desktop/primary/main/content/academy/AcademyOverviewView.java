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

package bisq.desktop.primary.main.content.academy;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MultiLineLabel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AcademyOverviewView extends View<GridPane, AcademyOverviewModel, AcademyOverviewController> {
    private static final int PADDING = 20;
    private int rowIndex;

    public AcademyOverviewView(AcademyOverviewModel model, AcademyOverviewController controller) {
        super(new GridPane(), model, controller);

        root.setHgap(PADDING);
        root.setVgap(PADDING);
        root.setCursor(Cursor.HAND);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        root.getColumnConstraints().addAll(col1, col2);

        addHeaderBox();

        addSmallBox("learn-bisq", "learn-bitcoin",
                "bisq", "bitcoin",
                NavigationTarget.BISQ_ACADEMY, NavigationTarget.BITCOIN_ACADEMY);
        addSmallBox("learn-security", "learn-privacy",
                "security", "privacy",
                NavigationTarget.SECURITY_ACADEMY, NavigationTarget.PRIVACY_ACADEMY);
        addSmallBox("learn-wallets", "learn-openSource",
                "wallets", "foss",
                NavigationTarget.WALLETS_ACADEMY, NavigationTarget.FOSS_ACADEMY);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    private void addHeaderBox() {
        Label headlineLabel = new MultiLineLabel(Res.get("academy.overview.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-4");
        headlineLabel.setMinHeight(35);

        Label contentLabel = new MultiLineLabel(Res.get("academy.overview.content"));
        contentLabel.getStyleClass().add("bisq-text-16");

        VBox.setVgrow(headlineLabel, Priority.ALWAYS);
        VBox.setVgrow(contentLabel, Priority.ALWAYS);
        VBox vBox = new VBox(20, headlineLabel, contentLabel);
        GridPane.setHgrow(vBox, Priority.ALWAYS);
        root.add(vBox, 0, 0, 2, 1);
    }

    private void addSmallBox(String leftIconId,
                             String rightIconId,
                             String leftTopic,
                             String rightTopic,
                             NavigationTarget leftNavigationTarget,
                             NavigationTarget rightNavigationTarget) {
        VBox leftBox = getWidgetBox(
                leftIconId,
                Res.get("academy.overview." + leftTopic),
                Res.get("academy.overview." + leftTopic + ".content"),
                Res.get("academy.overview.selectButton"),
                leftNavigationTarget
        );

        VBox rightBox = getWidgetBox(
                rightIconId,
                Res.get("academy.overview." + rightTopic),
                Res.get("academy.overview." + rightTopic + ".content"),
                Res.get("academy.overview.selectButton"),
                rightNavigationTarget
        );
        GridPane.setHgrow(leftBox, Priority.ALWAYS);
        GridPane.setHgrow(rightBox, Priority.ALWAYS);
        root.add(leftBox, 0, ++rowIndex, 1, 1);
        root.add(rightBox, 1, rowIndex, 1, 1);
    }

    private VBox getWidgetBox(String iconId, String headline, String content, String buttonLabel, NavigationTarget navigationTarget) {
        Label headlineLabel = new MultiLineLabel(headline);
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        headlineLabel.setGraphic(ImageUtil.getImageViewById(iconId));
        headlineLabel.setGraphicTextGap(15);
        headlineLabel.setMinHeight(35);
        headlineLabel.setWrapText(true);

        Label contentLabel = new MultiLineLabel(content);
        contentLabel.getStyleClass().add("bisq-text-3");
        contentLabel.setWrapText(true);

        Button button = new Button(buttonLabel);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().addAll("medium-large-button", "outlined-button", "grey-outlined-button");
        button.setOnAction(e -> controller.onSelect(navigationTarget));

        VBox.setVgrow(headlineLabel, Priority.ALWAYS);
        VBox.setVgrow(contentLabel, Priority.ALWAYS);
        VBox.setMargin(button, new Insets(10, 0, 10, 0));
        VBox vBox = new VBox(20, headlineLabel, contentLabel, button);
        vBox.setOnMouseClicked(e -> controller.onSelect(navigationTarget));
        vBox.getStyleClass().add("bisq-box-1");
        vBox.setPadding(new Insets(PADDING));
        return vBox;
    }
}
