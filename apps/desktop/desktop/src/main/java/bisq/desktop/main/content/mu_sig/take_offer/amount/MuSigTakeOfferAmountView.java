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

package bisq.desktop.main.content.mu_sig.take_offer.amount;

import bisq.desktop.common.Browser;
import bisq.desktop.common.Icons;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.containers.WizardOverlay;
import bisq.desktop.components.controls.BisqTooltip;
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
public class MuSigTakeOfferAmountView extends View<StackPane, MuSigTakeOfferAmountModel, MuSigTakeOfferAmountController> {
    private final Label headlineLabel, amountLimitInfo, amountLimitInfoOverlayInfo, linkToWikiText, warningIcon;
    private final Hyperlink amountLimitInfoAmount, learnMore, linkToWiki;
    private final VBox content;
    private final WizardOverlay amountLimitInfoOverlay;
    private final Button closeOverlayButton;
    private final HBox amountLimitInfoHBox;
    private Subscription isAmountLimitInfoVisiblePin, isWarningIconVisiblePin;

    public MuSigTakeOfferAmountView(MuSigTakeOfferAmountModel model,
                                    MuSigTakeOfferAmountController controller,
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
        closeOverlayButton = new Button(Res.get("bisqEasy.tradeWizard.amount.limitInfo.overlay.close"));
        linkToWikiText = new Label();
        linkToWiki = new Hyperlink("https://bisq.wiki/Reputation");
        amountLimitInfoOverlay = new WizardOverlay(root)
                .yellowWarning()
                .headline("bisqEasy.tradeWizard.amount.limitInfo.overlay.headline")
                .description(createAndGetOverlayContent(amountLimitInfoOverlayInfo, linkToWikiText, linkToWiki))
                .buttons(closeOverlayButton)
                .build();

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

        isAmountLimitInfoVisiblePin = EasyBind.subscribe(model.getIsAmountLimitInfoOverlayVisible(), isAmountLimitInfoVisible ->
            amountLimitInfoOverlay.updateOverlayVisibility(content, isAmountLimitInfoVisible, controller::onKeyPressedWhileShowingOverlay));

        isWarningIconVisiblePin = EasyBind.subscribe(model.getIsWarningIconVisible(), isWarningIconVisible -> {
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

        root.setOnKeyPressed(null);
    }

    private static VBox createAndGetOverlayContent(Label amountLimitInfoOverlayInfo,
                                                   Label linkToWikiText,
                                                   Hyperlink linkToWiki) {
        amountLimitInfoOverlayInfo.setMinWidth(WizardOverlay.OVERLAY_WIDTH - 100);
        amountLimitInfoOverlayInfo.setMaxWidth(amountLimitInfoOverlayInfo.getMinWidth());
        amountLimitInfoOverlayInfo.setMinHeight(Label.USE_PREF_SIZE);
        amountLimitInfoOverlayInfo.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");

        linkToWikiText.setMaxWidth(linkToWikiText.getMinWidth());
        linkToWikiText.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");

        linkToWiki.getStyleClass().addAll("normal-text", "text-fill-green");
        String tooltipText = Browser.hyperLinksGetCopiedWithoutPopup()
                ? Res.get("popup.hyperlink.copy.tooltip", linkToWiki.getText())
                : Res.get("popup.hyperlink.openInBrowser.tooltip", linkToWiki.getText());
        linkToWiki.setTooltip(new BisqTooltip(tooltipText));

        HBox linkBox = new HBox(5, linkToWikiText, linkToWiki);
        linkBox.setAlignment(Pos.BASELINE_LEFT);

        return new VBox(amountLimitInfoOverlayInfo, linkBox);
    }
}
