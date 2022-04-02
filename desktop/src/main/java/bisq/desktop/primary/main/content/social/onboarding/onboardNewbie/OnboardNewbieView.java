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

package bisq.desktop.primary.main.content.social.onboarding.onboardNewbie;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.BisqScrollPane;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpans;

import java.util.Collection;

@Slf4j
public class OnboardNewbieView extends View<BisqScrollPane, OnboardNewbieModel, OnboardNewbieController> {
    private final StyleClassedTextArea offerPreview;
    private final BisqTextArea terms;
    private final BisqButton publishButton, skipButton;
    private Subscription offerPreviewSubscription;

    public OnboardNewbieView(OnboardNewbieModel model,
                             OnboardNewbieController controller,
                             Pane marketSelection,
                             Pane amountPrice,
                             PaymentMethodsSelection paymentMethods) {
        super(new BisqScrollPane(), model, controller);

        // Place content within a VBox, within a ScrollPane, to show scrollbars if window size is too small
        VBox vBox = new VBox();
        vBox.getStyleClass().add("content-pane");
        vBox.setSpacing(20);
        vBox.setFillWidth(true);
        root.setContent(vBox);

        Pane section1Headline = getSectionHeadline(Res.get("satoshisquareapp.createOffer.section1.headline"));
        VBox.setMargin(section1Headline, new Insets(0, -20, -20, -20));

        Pane section2Headline = getSectionHeadline(Res.get("satoshisquareapp.createOffer.section2.headline"));
        VBox.setMargin(section2Headline, new Insets(0, -20, -20, -20));

        double width = 560;
        paymentMethods.setWidth(width / 2 - 30);
        VBox leftBox = getVBox(width);
        leftBox.getChildren().addAll(section1Headline, marketSelection, amountPrice, section2Headline, paymentMethods.getRoot());

        VBox rightBox = getVBox(width);

        Pane section3Headline = getSectionHeadline(Res.get("satoshisquareapp.createOffer.section3.headline"));
        VBox.setMargin(section3Headline, new Insets(0, -20, -20, -20));

        offerPreview = new StyleClassedTextArea();
        offerPreview.setWrapText(true);
        offerPreview.setBackground(null);
        offerPreview.setStyle("-fx-fill: white");

        Pane section4Headline = getSectionHeadline(Res.get("satoshisquareapp.createOffer.section4.headline"));
        VBox.setMargin(section4Headline, new Insets(0, -20, -20, -20));

        terms = new BisqTextArea();
        terms.setEditable(true);

        rightBox.getChildren().addAll(section3Headline, offerPreview, section4Headline, terms);

        publishButton = new BisqButton(Res.get("satoshisquareapp.createOffer.publish"));
        publishButton.getStyleClass().add("action-button");
        skipButton = new BisqButton(Res.get("skip"));

        HBox hBox = Layout.hBoxWith(leftBox, rightBox);
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        HBox.setHgrow(rightBox, Priority.ALWAYS);
        HBox buttons = Layout.hBoxWith(Spacer.fillHBox(), skipButton, publishButton);
        VBox.setMargin(buttons, new Insets(0, 20, 20, 0));

        Label welcome = new Label(Res.get("satoshisquareapp.createOffer.welcome"));
        welcome.setStyle("-fx-font-size: 1.892em; -fx-text-fill: #ddd;");
        welcome.setPadding(new Insets(20, 0, 0, 20));

        Label intro = new Label(Res.get("satoshisquareapp.createOffer.intro"));
        intro.setStyle("-fx-font-size: 1.1em; -fx-text-fill: #ddd;");
        intro.setPadding(new Insets(-10, 0, 10, 20));
        vBox.getChildren().addAll(welcome, intro, hBox, buttons);
    }

    @Override
    public void onViewAttached() {
        publishButton.visibleProperty().bind(model.getCreateOfferButtonVisibleProperty());
        publishButton.managedProperty().bind(model.getCreateOfferButtonVisibleProperty());
        terms.textProperty().bindBidirectional(model.getTerms());
        publishButton.setOnAction(e -> controller.onCreateOffer());
        skipButton.setOnAction(e -> controller.onSkip());
        offerPreviewSubscription = EasyBind.subscribe(model.getOfferPreview(), text -> {
            if (text != null) {
                offerPreview.replaceText(0, 0, text);
                StyleSpans<Collection<String>> styleSpans = model.getStyleSpans().get();
                offerPreview.setStyleSpans(0, styleSpans);
            }
        });
    }

    @Override
    public void onViewDetached() {
        publishButton.visibleProperty().unbind();
        publishButton.managedProperty().unbind();
        terms.textProperty().unbindBidirectional(model.getTerms());
        publishButton.setOnAction(null);
        skipButton.setOnAction(null);
        offerPreviewSubscription.unsubscribe();
    }

    private VBox getVBox(double width) {
        VBox vBox = new VBox();
        vBox.setSpacing(30);
        vBox.setPrefWidth(width);
        vBox.setMinWidth(560);
        vBox.setPadding(new Insets(20, 20, 20, 20));
        vBox.setStyle("-fx-background-color: #181818; -fx-background-radius: 10");
        return vBox;
    }

    private Pane getSectionHeadline(String headline) {
        Label label = new Label(headline);
        label.setStyle("-fx-font-size: 1.5em; -fx-text-fill: #ddd;");
        label.setPadding(new Insets(10, 10, 10, 10));
        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: #111;");
        pane.getChildren().add(label);
        return pane;
    }

}
