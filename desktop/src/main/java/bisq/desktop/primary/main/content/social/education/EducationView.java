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

package bisq.desktop.primary.main.content.social.education;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.fxmisc.easybind.EasyBind;

public class EducationView extends View<AnchorPane, EducationModel, EducationController> {
    public EducationView(EducationModel model, EducationController controller) {
        super(new AnchorPane(), model, controller);

        Pane headerBox = new Pane();
        Layout.pinToAnchorPane(headerBox, -8, 70, 0, 0);
        headerBox.setStyle("-fx-background-color: -bisq-dark-bg");

        Label headline = new Label(Res.get("social.education.headline"));
        headline.setWrapText(true);
        headline.setStyle("-fx-text-fill: -bisq-text; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 6.3em");
        headline.setPadding(new Insets(40, 66, 0, 66));

        Label content = new Label(Res.get("social.education.content"));
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: -bisq-text; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 2em");
        content.setPadding(new Insets(0, 66, 0, 66));
        EasyBind.subscribe(headline.heightProperty(), h -> {
            content.setLayoutY(h.doubleValue() + headline.getLayoutY() + 66);
        });

        EasyBind.subscribe(headerBox.widthProperty(), w -> {
            headline.setPrefWidth(w.doubleValue());
            content.setPrefWidth(w.doubleValue());
        });
        headerBox.setPadding(new Insets(0, 0, 70, 0));
        headerBox.getChildren().addAll(headline, content);

        VBox bisq = getWidgetBox(Res.get("social.education.wallets.headline"),
                Res.get("social.education.wallets.content"),
                Res.get("social.education.wallets.button"));
        VBox bitcoin = getWidgetBox(Res.get("social.education.wallets.headline"),
                Res.get("social.education.wallets.content"),
                Res.get("social.education.wallets.button"));
        HBox line1Box = Layout.hBoxWith(bisq, bitcoin);
        line1Box.setSpacing(60);

        VBox security = getWidgetBox(Res.get("social.education.wallets.headline"),
                Res.get("social.education.wallets.content"),
                Res.get("social.education.wallets.button"));
        VBox privacy = getWidgetBox(Res.get("social.education.wallets.headline"),
                Res.get("social.education.wallets.content"),
                Res.get("social.education.wallets.button"));
        HBox line2Box = Layout.hBoxWith(security, privacy);
        line1Box.setSpacing(60);

        VBox wallets = getWidgetBox(Res.get("social.education.wallets.headline"),
                Res.get("social.education.wallets.content"),
                Res.get("social.education.wallets.button"));
        VBox foss = getWidgetBox(Res.get("social.education.wallets.headline"),
                Res.get("social.education.wallets.content"),
                Res.get("social.education.wallets.button"));
        HBox line3Box = Layout.hBoxWith(wallets, foss);
        line1Box.setSpacing(60);

        root.getChildren().addAll(headerBox/*, line1Box, line2Box, line3Box*/);
    }

    //todo layout is screwed
    private VBox getWidgetBox(String headline, String content, String buttonLabel) {
        VBox box = new VBox();
        box.setStyle("-fx-background-color: -bisq-dark-bg");
        box.setSpacing(67);
        Label headlineLabel = new Label(headline);
        headlineLabel.setStyle("-fx-text-fill: -bisq-text; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 3em");
        VBox.setMargin(headlineLabel, new Insets(67, 0, 0, 0));

        TextArea textArea = new BisqTextArea(content);
        textArea.setStyle("-fx-text-fill: -bisq-text; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 2em");
        // VBox.setMargin(headline, new Insets(0, 0, 74, 0));
        VBox.setMargin(headlineLabel, new Insets(0, 0, 71, 0));

        Button button = new BisqButton(buttonLabel);
        // textArea.setStyle("-fx-text-fill: -bisq-text; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.46em");
        box.getChildren().addAll(headlineLabel, textArea, button);
        return box;
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
