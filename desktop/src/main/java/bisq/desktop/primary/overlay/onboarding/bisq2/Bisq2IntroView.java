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
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Bisq2IntroView extends View<VBox, Bisq2IntroModel, Bisq2IntroController> {
    private final Button nextButton;

    public Bisq2IntroView(Bisq2IntroModel model, Bisq2IntroController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(15);
        root.setAlignment(Pos.CENTER);

        ImageView logo = ImageUtil.getImageViewById("logo-mark-midsize");

        Label headlineLabel = new Label(Res.get("bisqEasy.onBoarding.bisq2.intro.headline"));
        headlineLabel.getStyleClass().add("bisq-popup-green-headline-label");

        Label subtitleLabel = new Label(Res.get("bisqEasy.onBoarding.bisq2.intro.subTitle").toUpperCase());
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMaxWidth(300);
        subtitleLabel.getStyleClass().addAll("bisq-text-3");
       
        nextButton = new Button(Res.get("start"));
        nextButton.setId("bisq-easy-next-button");
        
        VBox.setMargin(logo, new Insets(50, 0, 0, 0 ));
        VBox.setMargin(subtitleLabel, new Insets(10, 0, 5, 0));
        VBox.setMargin(nextButton, new Insets(0, 0, 50, 0));
        root.getChildren().addAll(logo,
                headlineLabel,
                subtitleLabel,
                getIconAndText(Res.get("bisqEasy.onBoarding.bisq2.intro.line1"), "onboarding-1-easy"),
                getIconAndText(Res.get("bisqEasy.onBoarding.bisq2.intro.line2"), "onboarding-1-fraction"),
                getIconAndText(Res.get("bisqEasy.onBoarding.bisq2.intro.line3"), "onboarding-2-payment"),
                Spacer.fillVBox(),
                nextButton);
    }

    @Override
    protected void onViewAttached() {
        nextButton.setOnAction(e -> controller.onNext());
    }

    @Override
    protected void onViewDetached() {
        nextButton.setOnAction(null);
    }

    public HBox getIconAndText(String text, String imageId) {
        Label label = new Label(text);
        label.setId("bisq-easy-onboarding-label");
        label.setWrapText(true);
        ImageView bulletPoint = ImageUtil.getImageViewById(imageId);
        bulletPoint.setFitWidth(37.5);
        bulletPoint.setFitHeight(37.5);
        HBox.setMargin(bulletPoint, new Insets(0, 0, 0, 4));
        HBox hBox = new HBox(15, bulletPoint, label);
        hBox.setAlignment(Pos.CENTER_LEFT);
        int width = 450;
        hBox.setMinWidth(width);
        hBox.setMaxWidth(width);
        return hBox;
    }
}
