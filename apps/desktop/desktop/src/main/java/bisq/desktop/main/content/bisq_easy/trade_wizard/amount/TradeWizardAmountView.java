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

package bisq.desktop.main.content.bisq_easy.trade_wizard.amount;

import bisq.desktop.common.Browser;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.bisq_easy.components.amount_selection.AmountSelectionController;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeWizardAmountView extends View<VBox, TradeWizardAmountModel, TradeWizardAmountController> {
    private static final String SELECTED_PRICE_MODEL_STYLE_CLASS = "selected-model";

    private final AmountSelectionController amountSelectionController;
    private final Label amountLimitInfo, amountLimitInfoOverlayInfo, linkToWikiText;
    private final Hyperlink learnMoreHyperLink, linkToWiki;
    @Getter
    private final VBox amountLimitInfoOverlay;
    private final Button learnHowToBuildReputation, closeOverlayButton, fixedAmount, rangeAmount;
    private final HBox amountModelsBox;
    private final HBox amountLimitInfoHBox;
    private Subscription isRangeAmountEnabledPin;

    public TradeWizardAmountView(TradeWizardAmountModel model,
                                 TradeWizardAmountController controller,
                                 AmountSelectionController amountSelectionController) {
        super(new VBox(10), model, controller);

        this.amountSelectionController = amountSelectionController;

        VBox amountSelectionRoot = amountSelectionController.getView().getRoot();
        amountSelectionRoot.getStyleClass().add("min-amount");
        HBox amountBox = new HBox(0, amountSelectionRoot);
        amountBox.setAlignment(Pos.BASELINE_LEFT);
        amountBox.getStyleClass().add("amount-box");

        amountLimitInfo = new Label();
        amountLimitInfo.getStyleClass().add("trade-wizard-amount-limit-info");

        learnMoreHyperLink = new Hyperlink();
        learnMoreHyperLink.getStyleClass().add("trade-wizard-amount-limit-info-overlay-link");
        learnMoreHyperLink.setMinWidth(Hyperlink.USE_PREF_SIZE);

        amountLimitInfoHBox = new HBox(2.5, amountLimitInfo, learnMoreHyperLink);
        amountLimitInfoHBox.setAlignment(Pos.CENTER);

        // Amount model selection
        fixedAmount = new Button(Res.get("bisqEasy.tradeWizard.amount.amountModel.fixedAmount"));
        fixedAmount.getStyleClass().add("model-selection-item");
        rangeAmount = new Button(Res.get("bisqEasy.tradeWizard.amount.amountModel.rangeAmount"));
        rangeAmount.getStyleClass().add("model-selection-item");
        Label separator = new Label("|");

        HBox fixedAmountBox = new HBox(fixedAmount);
        fixedAmountBox.getStyleClass().add("model-selection-item-box");
        fixedAmountBox.setAlignment(Pos.CENTER_RIGHT);
        HBox rangeAmountBox = new HBox(rangeAmount);
        rangeAmountBox.getStyleClass().add("model-selection-item-box");
        rangeAmountBox.setAlignment(Pos.CENTER_LEFT);

        amountModelsBox = new HBox(30, fixedAmountBox, separator, rangeAmountBox);
        amountModelsBox.getStyleClass().addAll("selection-models", "bisq-text-3");

        amountLimitInfoOverlayInfo = new Label();
        closeOverlayButton = new Button(Res.get("bisqEasy.tradeWizard.amount.limitInfo.overlay.close"));
        learnHowToBuildReputation = new Button(Res.get("bisqEasy.tradeWizard.amount.limitInfo.overlay.learnHowToBuildReputation"));
        learnHowToBuildReputation.getStyleClass().add("outlined-button");
        linkToWikiText = new Label();
        linkToWiki = new Hyperlink("https://bisq.wiki/Reputation");
        linkToWiki.getStyleClass().add("text-fill-green");
        amountLimitInfoOverlay = getAmountLimitInfoOverlay(amountLimitInfoOverlayInfo, closeOverlayButton,
                linkToWikiText, linkToWiki, learnHowToBuildReputation);

        root.getChildren().addAll(amountModelsBox, amountBox, amountLimitInfoHBox);
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("bisq-easy-trade-wizard-amount-step");
    }

    @Override
    protected void onViewAttached() {
        learnMoreHyperLink.setText(model.getAmountLimitInfoLink());
        linkToWikiText.setText(model.getLinkToWikiText());

        amountLimitInfo.textProperty().bind(model.getAmountLimitInfo());
        amountLimitInfoOverlayInfo.textProperty().bind(model.getAmountLimitInfoOverlayInfo());
        amountLimitInfoHBox.visibleProperty().bind(model.getShouldShowAmountLimitInfo());
        amountLimitInfoHBox.managedProperty().bind(model.getShouldShowAmountLimitInfo());
        amountModelsBox.visibleProperty().bind(model.getShowRangeAmounts());
        amountModelsBox.managedProperty().bind(model.getShowRangeAmounts());
        learnHowToBuildReputation.visibleProperty().bind(model.getShouldShowHowToBuildReputationButton());
        learnHowToBuildReputation.managedProperty().bind(model.getShouldShowHowToBuildReputationButton());

        isRangeAmountEnabledPin = EasyBind.subscribe(model.getIsRangeAmountEnabled(), isRangeAmountEnabled -> {
            fixedAmount.getStyleClass().remove(SELECTED_PRICE_MODEL_STYLE_CLASS);
            rangeAmount.getStyleClass().remove(SELECTED_PRICE_MODEL_STYLE_CLASS);
            if (isRangeAmountEnabled) {
                rangeAmount.getStyleClass().add(SELECTED_PRICE_MODEL_STYLE_CLASS);
            } else {
                fixedAmount.getStyleClass().add(SELECTED_PRICE_MODEL_STYLE_CLASS);
            }
            amountSelectionController.setIsRangeAmountEnabled(isRangeAmountEnabled);
        });

        learnMoreHyperLink.setOnAction(e -> controller.onShowAmountLimitInfoOverlay());
        linkToWiki.setOnAction(e -> controller.onOpenWiki(linkToWiki.getText()));
        learnHowToBuildReputation.setOnAction(e -> controller.onLearnHowToBuildReputation());
        closeOverlayButton.setOnAction(e -> controller.onCloseAmountLimitInfoOverlay());
        fixedAmount.setOnAction(e -> controller.useFixedAmount());
        rangeAmount.setOnAction(e -> controller.useRangeAmount());
    }

    @Override
    protected void onViewDetached() {
        amountLimitInfo.textProperty().unbind();
        amountLimitInfoOverlayInfo.textProperty().unbind();
        amountLimitInfoHBox.visibleProperty().unbind();
        amountLimitInfoHBox.managedProperty().unbind();
        amountModelsBox.visibleProperty().unbind();
        amountModelsBox.managedProperty().unbind();
        learnHowToBuildReputation.visibleProperty().unbind();
        learnHowToBuildReputation.managedProperty().unbind();

        isRangeAmountEnabledPin.unsubscribe();

        learnMoreHyperLink.setOnAction(null);
        linkToWiki.setOnAction(null);
        closeOverlayButton.setOnAction(null);
        learnHowToBuildReputation.setOnAction(null);
        fixedAmount.setOnAction(null);
        rangeAmount.setOnAction(null);
    }

    private static VBox getAmountLimitInfoOverlay(Label amountLimitInfo,
                                                  Button closeOverlayButton,
                                                  Label linkToWikiText,
                                                  Hyperlink linkToWiki,
                                                  Button learnHowToBuildReputation) {
        Label headlineLabel = new Label(Res.get("bisqEasy.tradeWizard.amount.limitInfo.overlay.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        amountLimitInfo.getStyleClass().addAll("bisq-text-21", "wrap-text");
        HBox amountLimitInfoBox = new HBox(amountLimitInfo);
        amountLimitInfoBox.setAlignment(Pos.BASELINE_LEFT);

        linkToWikiText.getStyleClass().addAll("bisq-text-21", "wrap-text");

        linkToWiki.getStyleClass().addAll("bisq-text-21");
        String tooltipText = Browser.hyperLinksGetCopiedWithoutPopup()
                ? Res.get("popup.hyperlink.copy.tooltip", linkToWiki.getText())
                : Res.get("popup.hyperlink.openInBrowser.tooltip", linkToWiki.getText());
        linkToWiki.setTooltip(new BisqTooltip(tooltipText));

        HBox linkBox = new HBox(5, linkToWikiText, linkToWiki);
        linkBox.setAlignment(Pos.BASELINE_LEFT);

        VBox.setMargin(learnHowToBuildReputation, new Insets(0, 0, 40, 0));
        VBox.setMargin(linkBox, new Insets(-40, 0, 0, 0));
        VBox.setMargin(closeOverlayButton, new Insets(10, 0, 0, 0));
        VBox content = new VBox(40, headlineLabel, amountLimitInfoBox,
                learnHowToBuildReputation, linkBox, closeOverlayButton);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().setAll("trade-wizard-feedback-bg");
        content.setPadding(new Insets(30));

        VBox vBox = new VBox(content, Spacer.fillVBox());
        vBox.setMaxWidth(700);
        return vBox;
    }
}
