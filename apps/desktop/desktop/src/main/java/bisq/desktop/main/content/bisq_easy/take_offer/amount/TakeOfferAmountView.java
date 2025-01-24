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

package bisq.desktop.main.content.bisq_easy.take_offer.amount;

import bisq.desktop.common.Browser;
import bisq.desktop.common.Icons;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TakeOfferAmountView extends View<StackPane, TakeOfferAmountModel, TakeOfferAmountController> {
    private final Label headlineLabel, amountLimitInfo, amountLimitInfoOverlayInfo, linkToWikiText, warningIcon;
    private final Hyperlink amountLimitInfoAmount, learnMore, linkToWiki;
    private final VBox content, amountLimitInfoOverlay;
    private final Button closeOverlayButton;
    private final HBox amountLimitInfoHBox;
    private Subscription isAmountLimitInfoVisiblePin, isWarningIconVisiblePin;

    public TakeOfferAmountView(TakeOfferAmountModel model,
                               TakeOfferAmountController controller,
                               VBox amountComponentRoot) {
        super(new StackPane(), model, controller);

        root.setAlignment(Pos.CENTER);
        content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        amountLimitInfo = new Label();
        amountLimitInfo.getStyleClass().add("trade-wizard-amount-limit-info");

        amountLimitInfoAmount = new Hyperlink();
        amountLimitInfoAmount.getStyleClass().add("trade-wizard-amount-limit-info-overlay-link");

        learnMore = new Hyperlink();
        learnMore.getStyleClass().add("trade-wizard-amount-limit-info-overlay-link");

        warningIcon = new Label();
        Icons.getIconForLabel(AwesomeIcon.WARNING_SIGN, warningIcon, "1.15em");
        warningIcon.getStyleClass().add("overlay-icon-warning");

        HBox.setMargin(warningIcon, new Insets(0, 5, 0, 0));
        HBox.setMargin(amountLimitInfoAmount, new Insets(0, 0, 0, -2.5));
        amountLimitInfoHBox = new HBox(5, warningIcon, amountLimitInfo, amountLimitInfoAmount, learnMore);
        amountLimitInfoHBox.setAlignment(Pos.BASELINE_CENTER);

        VBox.setMargin(headlineLabel, new Insets(-10, 0, 40, 0));
        VBox.setMargin(amountLimitInfoHBox, new Insets(15, 0, 15, 0));
        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, amountComponentRoot, amountLimitInfoHBox, Spacer.fillVBox());

        amountLimitInfoOverlayInfo = new Label();
        linkToWikiText = new Label();
        closeOverlayButton = new Button(Res.get("bisqEasy.tradeWizard.amount.limitInfo.overlay.close"));
        linkToWiki = new Hyperlink("https://bisq.wiki/Reputation");
        amountLimitInfoOverlay = getAmountLimitInfoOverlay(amountLimitInfoOverlayInfo, closeOverlayButton, linkToWikiText, linkToWiki);

        StackPane.setMargin(amountLimitInfoOverlay, new Insets(-TradeWizardView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, amountLimitInfoOverlay);
    }

    @Override
    protected void onViewAttached() {
        headlineLabel.setText(model.getHeadline());
        learnMore.setText(model.getAmountLimitInfoLink());
        linkToWikiText.setText(model.getLinkToWikiText());

        amountLimitInfo.textProperty().bind(model.getAmountLimitInfo());
        amountLimitInfoAmount.textProperty().bind(model.getAmountLimitInfoAmount());
        amountLimitInfoOverlayInfo.textProperty().bind(model.getAmountLimitInfoOverlayInfo());
        amountLimitInfoAmount.disableProperty().bind(model.getIsAmountHyperLinkDisabled());

        amountLimitInfoAmount.managedProperty().bind(model.getIsAmountLimitInfoVisible().and(model.getAmountLimitInfoAmount().isEmpty().not()));
        amountLimitInfoAmount.visibleProperty().bind(amountLimitInfoAmount.managedProperty());
        learnMore.managedProperty().bind(model.getIsAmountLimitInfoVisible());
        learnMore.visibleProperty().bind(model.getIsAmountLimitInfoVisible());
        amountLimitInfoHBox.managedProperty().bind(model.getIsAmountLimitInfoVisible());
        amountLimitInfoHBox.visibleProperty().bind(model.getIsAmountLimitInfoVisible());

        isAmountLimitInfoVisiblePin = EasyBind.subscribe(model.getIsAmountLimitInfoOverlayVisible(),
                isAmountLimitInfoVisible -> {
                    if (isAmountLimitInfoVisible) {
                        amountLimitInfoOverlay.setVisible(true);
                        amountLimitInfoOverlay.setOpacity(1);
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(amountLimitInfoOverlay, 450);
                    } else {
                        Transitions.removeEffect(content);
                        if (amountLimitInfoOverlay.isVisible()) {
                            Transitions.fadeOut(amountLimitInfoOverlay, Transitions.DEFAULT_DURATION / 2,
                                    () -> amountLimitInfoOverlay.setVisible(false));
                        }
                    }
                });

        isWarningIconVisiblePin  = EasyBind.subscribe(model.getIsWarningIconVisible(), isWarningIconVisible -> {
            warningIcon.setVisible(isWarningIconVisible);
            amountLimitInfo.getStyleClass().setAll("font-size-11", "wrap-text", "font-light");
            amountLimitInfo.getStyleClass().add(isWarningIconVisible ? "bisq-text-white" : "bisq-text-grey-9");
        });

        amountLimitInfoAmount.setOnAction(e -> controller.onSetReputationBasedAmount());
        learnMore.setOnAction(e -> controller.onShowAmountLimitInfoOverlay());
        linkToWiki.setOnAction(e -> controller.onOpenWiki(linkToWiki.getText()));
        closeOverlayButton.setOnAction(e -> controller.onCloseAmountLimitInfoOverlay());
    }

    @Override
    protected void onViewDetached() {
        amountLimitInfo.textProperty().unbind();
        amountLimitInfoAmount.textProperty().unbind();
        amountLimitInfoOverlayInfo.textProperty().unbind();
        amountLimitInfoAmount.disableProperty().unbind();

        amountLimitInfoAmount.managedProperty().unbind();
        amountLimitInfoAmount.visibleProperty().unbind();
        learnMore.managedProperty().unbind();
        learnMore.visibleProperty().unbind();
        amountLimitInfoHBox.managedProperty().unbind();
        amountLimitInfoHBox.visibleProperty().unbind();

        isAmountLimitInfoVisiblePin.unsubscribe();
        isWarningIconVisiblePin.unsubscribe();

        amountLimitInfoAmount.setOnAction(null);
        learnMore.setOnAction(null);
        linkToWiki.setOnAction(null);
        closeOverlayButton.setOnAction(null);
    }

    private static VBox getAmountLimitInfoOverlay(Label amountLimitInfoOverlayInfo,
                                                  Button closeOverlayButton,
                                                  Label linkToWikiText,
                                                  Hyperlink linkToWiki) {
        Label headlineLabel = new Label(Res.get("bisqEasy.tradeWizard.amount.limitInfo.overlay.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        amountLimitInfoOverlayInfo.setAlignment(Pos.BASELINE_LEFT);

        linkToWikiText.getStyleClass().addAll("bisq-text-21", "wrap-text");

        linkToWiki.getStyleClass().addAll("bisq-text-21");
        String tooltipText = Browser.hyperLinksGetCopiedWithoutPopup()
                ? Res.get("popup.hyperlink.copy.tooltip", linkToWiki.getText())
                : Res.get("popup.hyperlink.openInBrowser.tooltip", linkToWiki.getText());
        linkToWiki.setTooltip(new BisqTooltip(tooltipText));

        HBox linkBox = new HBox(5, linkToWikiText, linkToWiki);
        linkBox.setAlignment(Pos.BASELINE_LEFT);

        VBox overlayText = new VBox(20, amountLimitInfoOverlayInfo, linkBox);
        overlayText.setAlignment(Pos.TOP_LEFT);
        overlayText.setFillWidth(true);

        VBox.setMargin(linkBox, new Insets(-22.5, 0, 20, 0));
        VBox content = new VBox(20, headlineLabel, overlayText, closeOverlayButton);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().setAll("trade-wizard-feedback-bg");
        content.setPadding(new Insets(30));

        VBox vBox = new VBox(content, Spacer.fillVBox());
        vBox.setMaxWidth(700);
        return vBox;
    }
}
