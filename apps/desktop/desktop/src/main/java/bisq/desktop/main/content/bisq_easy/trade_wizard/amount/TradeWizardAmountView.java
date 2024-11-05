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

import bisq.common.util.StringUtils;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Icons;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.bisq_easy.components.amount_selection.AmountSelectionController;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeWizardAmountView extends View<StackPane, TradeWizardAmountModel, TradeWizardAmountController> {
    private static final String SELECTED_PRICE_MODEL_STYLE_CLASS = "selected-model";

    private final Label headlineLabel, amountLimitInfo, amountLimitInfoLeadLine, amountLimitInfoOverlayInfo, linkToWikiText, warningIcon;
    private final Hyperlink amountLimitInfoAmount, learnMoreHyperLink, linkToWiki;
    private final VBox minAmountRoot, content, amountLimitInfoOverlay;
    private final Button closeOverlayButton, fixedAmount, rangeAmount;
    private final HBox amountLimitInfoHBox, amountModelsBox;
    private final HBox amountLimitInfoWithWarnIcon;
    private Subscription isAmountLimitInfoVisiblePin, amountLimitInfoLeadLinePin, isRangeAmountEnabledPin;

    public TradeWizardAmountView(TradeWizardAmountModel model,
                                 TradeWizardAmountController controller,
                                 AmountSelectionController minAmountSelectionController,
                                 AmountSelectionController maxOrFixAmountSelectionController) {
        super(new StackPane(), model, controller);

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        minAmountRoot = minAmountSelectionController.getView().getRoot();
        HBox amountBox = new HBox(minAmountRoot, maxOrFixAmountSelectionController.getView().getRoot());
        amountBox.setAlignment(Pos.CENTER);

        amountLimitInfo = new Label();
        amountLimitInfo.getStyleClass().add("trade-wizard-amount-limit-info");

        amountLimitInfoAmount = new Hyperlink();
        amountLimitInfoAmount.getStyleClass().add("trade-wizard-amount-limit-info-overlay-link");

        learnMoreHyperLink = new Hyperlink();
        learnMoreHyperLink.getStyleClass().add("trade-wizard-amount-limit-info-overlay-link");

        amountLimitInfoHBox = new HBox(2.5, amountLimitInfo, amountLimitInfoAmount, learnMoreHyperLink);
        amountLimitInfoHBox.setAlignment(Pos.BASELINE_CENTER);

        amountLimitInfoLeadLine = new Label();
        amountLimitInfoLeadLine.getStyleClass().add("trade-wizard-amount-limit-info");
        VBox amountLimitInfoVBox = new VBox(-2.5, amountLimitInfoLeadLine, amountLimitInfoHBox);

        warningIcon = new Label();
        Icons.getIconForLabel(AwesomeIcon.WARNING_SIGN, warningIcon, "1.15em");
        warningIcon.getStyleClass().add("overlay-icon-warning");

        amountLimitInfoWithWarnIcon = new HBox(10, warningIcon, amountLimitInfoVBox);
        amountLimitInfoWithWarnIcon.setAlignment(Pos.CENTER);

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

        VBox.setMargin(headlineLabel, new Insets(-10, 0, 0, 0));
//        VBox.setMargin(amountLimitInfoWithWarnIcon, new Insets(15, 0, 15, 0));
        content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, amountModelsBox, amountBox, amountLimitInfoWithWarnIcon, Spacer.fillVBox());
        content.getStyleClass().add("bisq-easy-trade-wizard-amount-step");

        amountLimitInfoOverlayInfo = new Label();
        linkToWikiText = new Label();
        closeOverlayButton = new Button(Res.get("bisqEasy.tradeWizard.amount.limitInfo.overlay.close"));
        linkToWiki = new Hyperlink("https://bisq.wiki/Reputation");
        amountLimitInfoOverlay = getAmountLimitInfoOverlay(amountLimitInfoOverlayInfo, closeOverlayButton, linkToWikiText, linkToWiki);

        StackPane.setMargin(amountLimitInfoOverlay, new Insets(-TradeWizardView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, amountLimitInfoOverlay);
        root.setAlignment(Pos.CENTER);
    }

    @Override
    protected void onViewAttached() {
        headlineLabel.setText(model.getHeadline());
        learnMoreHyperLink.setText(model.getAmountLimitInfoLink());
        linkToWikiText.setText(model.getLinkToWikiText());

        amountLimitInfo.textProperty().bind(model.getAmountLimitInfo());
        amountLimitInfoLeadLine.textProperty().bind(model.getAmountLimitInfoLeadLine());
        amountLimitInfoAmount.textProperty().bind(model.getAmountLimitInfoAmount());
        amountLimitInfoOverlayInfo.textProperty().bind(model.getAmountLimitInfoOverlayInfo());
        amountLimitInfoAmount.disableProperty().bind(model.getIsAmountHyperLinkDisabled());

        learnMoreHyperLink.visibleProperty().bind(model.getIsLearnMoreVisible());
        learnMoreHyperLink.managedProperty().bind(model.getIsLearnMoreVisible());
        warningIcon.visibleProperty().bind(model.getIsWarningIconVisible());
        amountLimitInfoAmount.visibleProperty().bind(model.getAmountLimitInfoAmount().isEmpty().not());
        amountLimitInfoAmount.managedProperty().bind(model.getAmountLimitInfoAmount().isEmpty().not());
        minAmountRoot.visibleProperty().bind(model.getIsRangeAmountEnabled());
        minAmountRoot.managedProperty().bind(model.getIsRangeAmountEnabled());
        amountModelsBox.visibleProperty().bind(model.getShowRangeAmounts());
        amountModelsBox.managedProperty().bind(model.getShowRangeAmounts());

        amountLimitInfoLeadLinePin = EasyBind.subscribe(model.getAmountLimitInfoLeadLine(), value -> {
            boolean isEmpty = StringUtils.isEmpty(value);
            amountLimitInfoLeadLine.setVisible(!isEmpty);
            amountLimitInfoLeadLine.setManaged(!isEmpty);
            double top = isEmpty ? 0 : -22.5;
            HBox.setMargin(warningIcon, new Insets(top, 0, 0, 0));
        });

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

        isRangeAmountEnabledPin = EasyBind.subscribe(model.getIsRangeAmountEnabled(), isRangeAmountEnabled -> {
            fixedAmount.getStyleClass().remove(SELECTED_PRICE_MODEL_STYLE_CLASS);
            rangeAmount.getStyleClass().remove(SELECTED_PRICE_MODEL_STYLE_CLASS);
            if (isRangeAmountEnabled) {
                rangeAmount.getStyleClass().add(SELECTED_PRICE_MODEL_STYLE_CLASS);
            } else {
                fixedAmount.getStyleClass().add(SELECTED_PRICE_MODEL_STYLE_CLASS);
            }
        });

        amountLimitInfoAmount.setOnAction(e -> controller.onSetReputationBasedAmount());
        learnMoreHyperLink.setOnAction(e -> controller.onShowAmountLimitInfoOverlay());
        linkToWiki.setOnAction(e -> controller.onOpenWiki(linkToWiki.getText()));
        closeOverlayButton.setOnAction(e -> controller.onCloseAmountLimitInfoOverlay());
        fixedAmount.setOnAction(e -> controller.useFixedAmount());
        rangeAmount.setOnAction(e -> controller.useRangeAmount());
    }

    @Override
    protected void onViewDetached() {
        amountLimitInfo.textProperty().unbind();
        amountLimitInfoLeadLine.textProperty().unbind();
        amountLimitInfoAmount.textProperty().unbind();
        amountLimitInfoOverlayInfo.textProperty().unbind();
        amountLimitInfoAmount.disableProperty().unbind();

        learnMoreHyperLink.visibleProperty().unbind();
        learnMoreHyperLink.managedProperty().unbind();
        warningIcon.visibleProperty().unbind();
        minAmountRoot.visibleProperty().unbind();
        minAmountRoot.managedProperty().unbind();
        amountLimitInfoAmount.visibleProperty().unbind();
        amountLimitInfoAmount.managedProperty().unbind();
        amountModelsBox.visibleProperty().unbind();
        amountModelsBox.managedProperty().unbind();

        amountLimitInfoLeadLinePin.unsubscribe();
        isAmountLimitInfoVisiblePin.unsubscribe();
        isRangeAmountEnabledPin.unsubscribe();

        amountLimitInfoAmount.setOnAction(null);
        learnMoreHyperLink.setOnAction(null);
        linkToWiki.setOnAction(null);
        closeOverlayButton.setOnAction(null);
        fixedAmount.setOnAction(null);
        rangeAmount.setOnAction(null);
    }

    private static VBox getAmountLimitInfoOverlay(Label amountLimitInfoOverlayInfo,
                                                  Button closeOverlayButton,
                                                  Label linkToWikiText,
                                                  Hyperlink linkToWiki) {
        Label headlineLabel = new Label(Res.get("bisqEasy.tradeWizard.amount.limitInfo.overlay.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        amountLimitInfoOverlayInfo.getStyleClass().addAll("bisq-text-21", "wrap-text");
        amountLimitInfoOverlayInfo.setAlignment(Pos.BASELINE_LEFT);

        linkToWikiText.getStyleClass().addAll("bisq-text-21", "wrap-text");

        linkToWiki.getStyleClass().addAll("bisq-text-21");
        String tooltipText = Browser.hyperLinksGetCopiedWithoutPopup()
                ? Res.get("popup.hyperlink.copy.tooltip", linkToWiki.getText())
                : Res.get("popup.hyperlink.openInBrowser.tooltip", linkToWiki.getText());
        linkToWiki.setTooltip(new BisqTooltip(tooltipText));

        HBox linkBox = new HBox(5, linkToWikiText, linkToWiki);
        linkBox.setAlignment(Pos.BASELINE_LEFT);

        VBox.setMargin(linkBox, new Insets(-22.5, 0, 20, 0));
        VBox content = new VBox(20, headlineLabel, amountLimitInfoOverlayInfo, linkBox, closeOverlayButton);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().setAll("trade-wizard-feedback-bg");
        content.setPadding(new Insets(30));

        VBox vBox = new VBox(content, Spacer.fillVBox());
        vBox.setMaxWidth(700);
        return vBox;
    }
}
