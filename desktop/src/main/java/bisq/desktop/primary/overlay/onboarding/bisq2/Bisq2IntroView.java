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

package bisq.desktop.primary.overlay.onboarding.bisq2;

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
public class Bisq2IntroView extends View<VBox, Bisq2IntroModel, Bisq2IntroController> {
    private final Button nextButton;

    public Bisq2IntroView(Bisq2IntroModel model, Bisq2IntroController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(23);
        root.setAlignment(Pos.CENTER);

        ImageView logo = ImageUtil.getImageViewById("logo-mark-midsize");

        Label headlineLabel = new Label(Res.get("bisqEasy.onBoarding.bisq2.intro.headline"));
        headlineLabel.getStyleClass().add("bisq-popup-green-headline-label");

        nextButton = new Button(Res.get("start"));
        nextButton.setDefaultButton(true);

        HBox hBox = new HBox(23, getWidgetBox(Res.get("bisqEasy.onBoarding.bisq2.intro.headline1"),
                Res.get("bisqEasy.onBoarding.bisq2.intro.line1"),
                "intro-1"),
                getWidgetBox(Res.get("bisqEasy.onBoarding.bisq2.intro.headline2"),
                        Res.get("bisqEasy.onBoarding.bisq2.intro.line2"),
                        "intro-2"),
                getWidgetBox(Res.get("bisqEasy.onBoarding.bisq2.intro.headline3"),
                        Res.get("bisqEasy.onBoarding.bisq2.intro.line3"),
                        "intro-3"));
        hBox.setAlignment(Pos.CENTER);
        hBox.setPadding(new Insets(20));

        VBox.setMargin(logo, new Insets(30, 0, 0, 0));
        VBox.setMargin(hBox, new Insets(10, 0, 0, 0));
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
