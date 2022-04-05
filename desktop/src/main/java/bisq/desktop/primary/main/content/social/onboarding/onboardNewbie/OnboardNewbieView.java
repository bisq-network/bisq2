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
import bisq.desktop.components.containers.SectionBox;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqTaggableTextArea;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.layout.Layout;
import bisq.desktop.overlay.Popup;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class OnboardNewbieView extends View<BisqScrollPane, OnboardNewbieModel, OnboardNewbieController> {
    private final BisqTaggableTextArea offerPreview;
    private final BisqTextArea terms;
    private final BisqButton publishButton, skipButton;
    private final ChangeListener<Boolean> termsEditableListener;
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
        vBox.setPadding(new Insets(0, 20, 20, 20));
        root.setContent(vBox);

        double width = 560;
        paymentMethods.setWidth(width / 2 - 30);
        SectionBox leftBox = new SectionBox(Res.get("satoshisquareapp.createOffer.section1.headline"));
        leftBox.setPrefWidth(width);
        Pane section2Headline = SectionBox.getHeadline(Res.get("satoshisquareapp.createOffer.section2.headline"));
        VBox.setMargin(section2Headline, new Insets(0, -20, -20, -20));
        leftBox.getChildren().addAll(marketSelection, amountPrice, section2Headline, paymentMethods.getRoot());

        SectionBox rightBox = new SectionBox(Res.get("satoshisquareapp.createOffer.section3.headline"));
        leftBox.setPrefWidth(width);
        
        offerPreview = new BisqTaggableTextArea();

        Pane section4Headline = SectionBox.getHeadline(Res.get("satoshisquareapp.createOffer.section4.headline"));
        VBox.setMargin(section4Headline, new Insets(0, -20, -20, -20));

        terms = new BisqTextArea();
        terms.setEditable(true);
        terms.setInitialHeight(120);
        VBox.setMargin(terms, new Insets(5, 0, 0, 0));

        rightBox.getChildren().addAll(offerPreview, section4Headline, terms);

        publishButton = new BisqButton(Res.get("satoshisquareapp.createOffer.publish"));
        publishButton.getStyleClass().add("action-button");
        skipButton = new BisqButton(Res.get("satoshisquareapp.createOffer.skip"));

        HBox hBox = Layout.hBoxWith(leftBox, rightBox);
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        HBox.setHgrow(rightBox, Priority.ALWAYS);
        HBox buttons = Layout.hBoxWith(Spacer.fillHBox(), skipButton, publishButton);
        VBox.setMargin(buttons, new Insets(0, 20, 20, 0));
        vBox.getChildren().addAll(hBox, buttons);
        termsEditableListener = (observable, oldValue, newValue) -> {
            if (!newValue) {
                new Popup()
                        .warning(Res.get("satoshisquareapp.createOffer.termsTooLong", OnboardNewbieModel.MAX_INPUT_TERMS))
                        .show();
            }
        };
    }

    @Override
    public void onViewAttached() {
        terms.textProperty().bindBidirectional(model.getTerms());
        terms.editableProperty().bind(model.getTermsEditable());
        publishButton.visibleProperty().bind(model.getCreateOfferButtonVisibleProperty());
        publishButton.managedProperty().bind(model.getCreateOfferButtonVisibleProperty());
        publishButton.disableProperty().bind(model.getIsInvalidTradeIntent());
        publishButton.setOnAction(e -> controller.onCreateOffer());
        skipButton.setOnAction(e -> controller.onSkip());
        offerPreviewSubscription = EasyBind.subscribe(model.getOfferPreview(), text -> {
            if (text != null) {
                offerPreview.setText(text);
                offerPreview.setStyleSpans(0,  model.getStyleSpans().get());
            }
        });
        
        terms.editableProperty().addListener(termsEditableListener);
    }

    @Override
    public void onViewDetached() {
        terms.textProperty().unbindBidirectional(model.getTerms());
        terms.editableProperty().unbind();
        publishButton.visibleProperty().unbind();
        publishButton.managedProperty().unbind();
        publishButton.disableProperty().unbind();
        publishButton.setOnAction(null);
        skipButton.setOnAction(null);
        offerPreviewSubscription.unsubscribe();
        terms.editableProperty().removeListener(termsEditableListener);
    }
}
