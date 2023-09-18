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

package bisq.desktop.overlay.onboarding.welcome;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WelcomeView extends View<VBox, WelcomeModel, WelcomeController> {
    private final Button nextButton;

    public WelcomeView(WelcomeModel model, WelcomeController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(23);
        root.setAlignment(Pos.CENTER);

        ImageView logo = ImageUtil.getImageViewById("logo-mark-midsize");

        Label headlineLabel = new Label(Res.get("onboarding.bisq2.headline"));
        headlineLabel.getStyleClass().add("bisq2-welcome-headline");

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

        HBox hBox = new HBox(23, getWidgetBox(Res.get("onboarding.bisq2.headline1"),
                Res.get("onboarding.bisq2.line1"),
                "bisq-easy"),
                getWidgetBox(Res.get("onboarding.bisq2.headline2"),
                        Res.get("onboarding.bisq2.line2"),
                        "learn"),
                getWidgetBox(Res.get("onboarding.bisq2.headline3"),
                        Res.get("onboarding.bisq2.line3"),
                        "fiat-btc"));
        hBox.setAlignment(Pos.CENTER);
        hBox.setPadding(new Insets(20));

        VBox.setMargin(logo, new Insets(50, 0, -10, 0));
        VBox.setMargin(hBox, new Insets(0, 0, 0, 0));
        VBox.setMargin(nextButton, new Insets(30, 0, 30, 0));
        root.getChildren().addAll(logo,
                headlineLabel,
                hBox,
                nextButton);
    }

    @Override
    protected void onViewAttached() {
        nextButton.setOnMouseClicked(e -> controller.onNext());
        root.setOnKeyReleased(keyEvent -> KeyHandlerUtil.handleEnterKeyEvent(keyEvent, controller::onNext));
    }

    @Override
    protected void onViewDetached() {
        nextButton.setOnMouseClicked(null);
        root.setOnKeyReleased(null);
    }

    private VBox getWidgetBox(String headline, String content, String imageId) {
        ImageView icon = ImageUtil.getImageViewById(imageId);

        Label headlineLabel = new Label(headline);
        headlineLabel.getStyleClass().addAll("bisq-text-headline-2");

        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().addAll("bisq-text-3");
        contentLabel.setWrapText(true);
        contentLabel.setTextAlignment(TextAlignment.CENTER);
        contentLabel.setAlignment(Pos.CENTER);
        contentLabel.setPrefWidth(350);

        VBox vBox = new VBox(16, icon, headlineLabel, contentLabel);
        vBox.setAlignment(Pos.CENTER);

        return vBox;
    }
}
