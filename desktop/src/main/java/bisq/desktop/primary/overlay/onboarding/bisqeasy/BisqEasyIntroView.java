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

package bisq.desktop.primary.overlay.onboarding.bisqeasy;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyIntroView extends View<StackPane, BisqEasyIntroModel, BisqEasyIntroController> {
    private final Button nextButton, skipButton;

    public BisqEasyIntroView(BisqEasyIntroModel model, BisqEasyIntroController controller) {
        super(new StackPane(), model, controller);
      
      //  root.setSpacing(15);
        root.setAlignment(Pos.CENTER);

      
        ImageView template = ImageUtil.getImageViewById("bisq-easy-onboarding-dummy");
        
        ImageView logo = ImageUtil.getImageViewById("bisq-easy");
        logo.setScaleX(1.5);
        logo.setScaleY(1.5);

        Label headlineLabel = new Label(Res.get("bisqEasy.onBoarding.bisqEasy.intro.headline"));
       // headlineLabel.getStyleClass().add("bisq-content-headline-label");
        headlineLabel.getStyleClass().add("bisq-popup-green-headline-label");

        Label subtitleLabel = new Label(Res.get("bisqEasy.onBoarding.bisqEasy.intro.subTitle"));
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMaxWidth(300);
        subtitleLabel.getStyleClass().addAll("bisq-text-3");

        nextButton = new Button(Res.get("bisqEasy.onBoarding.bisqEasy.intro.createOffer"));
        nextButton.setDefaultButton(true);
        skipButton = new Button(Res.get("bisqEasy.onBoarding.bisqEasy.intro.skip"));
        HBox buttons = new HBox(7, skipButton, nextButton);
        buttons.setAlignment(Pos.CENTER);

        VBox.setMargin(logo, new Insets(50, 0, 0, 0));
        //VBox.setMargin(headlineLabel, new Insets(50, 0, 0, 0));
        VBox.setMargin(subtitleLabel, new Insets(10, 0, 5, 0));
        VBox.setMargin(buttons, new Insets(0, 0, 50, 0));
      /*  root.getChildren().addAll(logo,
                headlineLabel,
                subtitleLabel,
                getIconAndText(Res.get("bisqEasy.onBoarding.bisqEasy.intro.line1"), "onboarding-2-offer"),
                getIconAndText(Res.get("bisqEasy.onBoarding.bisqEasy.intro.line2"), "onboarding-2-chat"),
                getIconAndText(Res.get("bisqEasy.onBoarding.bisqEasy.intro.line3"), "onboarding-2-payment"),
                getIconAndText(Res.get("bisqEasy.onBoarding.bisqEasy.intro.line4"), "onboarding-1-reputation"),
                Spacer.fillVBox(),
                buttons);*/

        template.setFitWidth(859); 
        template.setFitHeight(352);
        buttons.setSpacing(114);
        nextButton.setMinWidth(330);
        skipButton.setMinWidth(nextButton.getMinWidth());
        nextButton.setMinHeight(50);
        skipButton.setMinHeight(nextButton.getMinHeight());
        buttons.setOpacity(0);
        StackPane.setMargin(buttons, new Insets(230,0,0,0));
        root.getChildren().addAll(template, buttons);
    }

    @Override
    protected void onViewAttached() {
        nextButton.setOnAction(e -> controller.onNext());
        skipButton.setOnAction(evt -> controller.onSkip());
    }

    @Override
    protected void onViewDetached() {
        nextButton.setOnAction(null);
        skipButton.setOnAction(null);
    }

    public HBox getIconAndText(String text, String imageId) {
        Label label = new Label(text);
        label.setId("bisq-easy-onboarding-label");
        label.setWrapText(true);
        ImageView bulletPoint = ImageUtil.getImageViewById(imageId);
        HBox.setMargin(bulletPoint, new Insets(-4, 0, 0, 4));
        HBox hBox = new HBox(15, bulletPoint, label);
        hBox.setAlignment(Pos.CENTER_LEFT);
        int width = 450;
        hBox.setMinWidth(width);
        hBox.setMaxWidth(width);
        return hBox;
    }
}
