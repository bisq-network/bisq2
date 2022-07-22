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

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AcademyView extends View<GridPane, AcademyModel, AcademyController> {
    private static final int PADDING = 20;
    private int rowIndex;

    public AcademyView(AcademyModel model, AcademyController controller) {
        super(new GridPane(), model, controller);

        root.setHgap(PADDING);
        root.setVgap(PADDING);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        root.getColumnConstraints().addAll(col1, col2);

        addHeaderBox();

        addSmallBox("dashboard-bisq", "onboarding-1-fraction",
                "bisq", "bitcoin",
                NavigationTarget.BISQ_ACADEMY, NavigationTarget.BITCOIN_ACADEMY);
        addSmallBox("onboarding-1-reputation", "onboarding-2-offer",
                "security", "privacy",
                NavigationTarget.SECURITY_ACADEMY, NavigationTarget.PRIVACY_ACADEMY);
        addSmallBox("onboarding-3-profile", "onboarding-2-chat",
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
        Label headlineLabel = new Label(Res.get("social.education.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-4");
        headlineLabel.setWrapText(true);

        Label contentLabel = new Label(Res.get("social.education.content"));
        contentLabel.getStyleClass().add("bisq-text-16");
        contentLabel.setWrapText(true);

        VBox vBox = new VBox(20, headlineLabel, contentLabel);
        vBox.getStyleClass().add("bisq-box-2");
        vBox.setPadding(new Insets(PADDING));
        GridPane.setHgrow(vBox, Priority.ALWAYS);
        GridPane.setVgrow(vBox, Priority.ALWAYS);
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
                Res.get("social.education." + leftTopic + ".headline"),
                Res.get("social.education." + leftTopic + ".content"),
                Res.get("social.education." + leftTopic + ".button"),
                leftNavigationTarget
        );

        VBox rightBox = getWidgetBox(
                rightIconId,
                Res.get("social.education." + rightTopic + ".headline"),
                Res.get("social.education." + rightTopic + ".content"),
                Res.get("social.education." + rightTopic + ".button"),
                rightNavigationTarget
        );
        GridPane.setHgrow(leftBox, Priority.ALWAYS);
        GridPane.setHgrow(rightBox, Priority.ALWAYS);
        GridPane.setVgrow(leftBox, Priority.ALWAYS);
        GridPane.setVgrow(rightBox, Priority.ALWAYS);
        root.add(leftBox, 0, ++rowIndex, 1, 1);
        root.add(rightBox, 1, rowIndex, 1, 1);
    }

    private VBox getWidgetBox(String iconId, String headline, String content, String buttonLabel, NavigationTarget navigationTarget) {
        Label headlineLabel = new Label(headline);
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        // headlineLabel.setGraphic(ImageUtil.getImageViewById(iconId));
        headlineLabel.setGraphicTextGap(15);
        headlineLabel.setWrapText(true);

        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().add("bisq-text-3");
        contentLabel.setWrapText(true);

        Button button = new Button(buttonLabel.toUpperCase());
        button.getStyleClass().addAll("text-button", "no-background");
        button.setOnAction(e -> controller.onSelect(navigationTarget));

        HBox.setMargin(button, new Insets(0, -10, 0, 0));
        HBox buttonHBox = new HBox(Spacer.fillHBox(), button);

        VBox.setVgrow(headlineLabel, Priority.ALWAYS);
        VBox.setVgrow(contentLabel, Priority.ALWAYS);
        VBox vBox = new VBox(20, headlineLabel, contentLabel, buttonHBox);
        vBox.setOnMouseClicked(e -> controller.onSelect(navigationTarget));
        vBox.getStyleClass().add("bisq-box-1");
        vBox.setPadding(new Insets(PADDING));

        // contentLabel adjusts to available height of vBox and does not wrap, so we enforce minHeight 
        // after it's rendered with a large vBox.
        vBox.setMinHeight(500);
        UIThread.runOnNextRenderFrame(() -> {
            contentLabel.setMinHeight(contentLabel.getHeight());
            vBox.setMinHeight(Region.USE_COMPUTED_SIZE);
        });
        return vBox;
    }
}
