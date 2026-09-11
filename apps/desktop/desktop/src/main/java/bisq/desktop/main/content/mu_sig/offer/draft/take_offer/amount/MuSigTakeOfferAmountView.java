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

package bisq.desktop.main.content.mu_sig.offer.draft.take_offer.amount;

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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MuSigTakeOfferAmountView extends View<StackPane, MuSigTakeOfferAmountModel, MuSigTakeOfferAmountController> {
    private static final String SELECTED_MODEL_STYLE_CLASS = "selected-model";

    private final Label headlineLabel, amountLimitInfo, amountLimitInfoOverlayInfo, linkToWikiText, warningIcon;
    private final Hyperlink learnMore, linkToWiki;
    @Getter
    private final VBox overlay;
    private final Button learnHowToBuildReputation, closeOverlayButton;
    private final HBox amountLimitInfoHBox, learnHowToBuildReputationBox;
    private final VBox content;
    private final Set<Subscription> subscriptions = new HashSet<>();

    public MuSigTakeOfferAmountView(MuSigTakeOfferAmountModel model,
                                    MuSigTakeOfferAmountController controller,
                                    VBox amountComponents) {
        super(new StackPane(), model, controller);

        root.setAlignment(Pos.CENTER);
        content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("bisq-text-headline-2");


        // Amount component
        amountComponents.getStyleClass().add("min-amount");
        HBox amountBox = new HBox(0, amountComponents);
        amountBox.getStyleClass().add("amount-box");
        amountBox.setAlignment(Pos.TOP_CENTER);

        // Amount limit info
        warningIcon = new Label();
        warningIcon.getStyleClass().add("text-fill-grey-dimmed");
        warningIcon.setPadding(new Insets(0, 2.5, 0, 0));
        warningIcon.setMinWidth(Label.USE_PREF_SIZE);
        Icons.getIconForLabel(AwesomeIcon.WARNING_SIGN, warningIcon, "1em");

        amountLimitInfo = new Label();
        amountLimitInfo.getStyleClass().add("trade-wizard-amount-limit-info");

        learnMore = new Hyperlink();
        learnMore.getStyleClass().add("trade-wizard-amount-limit-info-overlay-link");
        learnMore.setMinWidth(Hyperlink.USE_PREF_SIZE);

        amountLimitInfoHBox = new HBox(2.5, warningIcon, amountLimitInfo, learnMore);
        amountLimitInfoHBox.setAlignment(Pos.TOP_CENTER);


        // Amount limit overlay
        amountLimitInfoOverlayInfo = new Label();

        linkToWikiText = new Label();
        linkToWiki = new Hyperlink("https://bisq.wiki/Reputation");

        learnHowToBuildReputation = new Button(Res.get("muSig.offer.create.amount.limitInfo.overlay.learnHowToBuildReputation"));
        learnHowToBuildReputation.getStyleClass().add("outlined-button");
        learnHowToBuildReputationBox = new HBox(learnHowToBuildReputation);

        closeOverlayButton = new Button(Res.get("muSig.offer.wizard.amount.limitInfo.overlay.close"));

        overlay = new WizardOverlay(root)
                .warning()
                .headlineFromI18nKey("muSig.offer.wizard.amount.limitInfo.overlay.headline")
                .description(createAndGetOverlayContent(amountLimitInfoOverlayInfo, linkToWikiText, linkToWiki, learnHowToBuildReputationBox))
                .buttons(closeOverlayButton)
                .build();


        VBox.setMargin(headlineLabel, new Insets(-10, 0, 40, 0));
        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, amountBox, amountLimitInfoHBox, Spacer.fillVBox());

        root.getChildren().addAll(content, overlay);
    }

    @Override
    protected void onViewAttached() {
        headlineLabel.setText(model.getHeadline());

        learnMore.setText(model.getAmountLimitInfoLink());
        linkToWikiText.setText(model.getLinkToWikiText());

        amountLimitInfo.textProperty().bind(model.getAmountLimitInfo());
        amountLimitInfoOverlayInfo.textProperty().bind(model.getAmountLimitInfoOverlayInfo());
        amountLimitInfoHBox.visibleProperty().bind(model.getShouldShowAmountLimitInfo());
        amountLimitInfoHBox.managedProperty().bind(model.getShouldShowAmountLimitInfo());
        learnMore.visibleProperty().bind(model.getLearnMoreVisible());
        learnMore.managedProperty().bind(model.getLearnMoreVisible());
        learnHowToBuildReputationBox.visibleProperty().bind(model.getShouldShowHowToBuildReputationButton());
        learnHowToBuildReputationBox.managedProperty().bind(model.getShouldShowHowToBuildReputationButton());
        warningIcon.visibleProperty().bind(model.getShouldShowWarningIcon());
        warningIcon.managedProperty().bind(model.getShouldShowWarningIcon());

        subscriptions.add(EasyBind.subscribe(model.getIsOverlayVisible(), isOverlayVisible -> {
            if (isOverlayVisible) {
                root.setOnKeyPressed(controller::onKeyPressedWhileShowingOverlay);
            } else {
                root.setOnKeyPressed(null);
            }
        }));

        learnMore.setOnAction(e -> controller.onShowOverlay());
        linkToWiki.setOnAction(e -> controller.onOpenWiki(linkToWiki.getText()));
        //learnHowToBuildReputation.setOnAction(e -> controller.onLearnHowToBuildReputation());
        closeOverlayButton.setOnAction(e -> controller.onCloseOverlay());
    }

    @Override
    protected void onViewDetached() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();

        amountLimitInfo.textProperty().unbind();
        amountLimitInfoOverlayInfo.textProperty().unbind();
        learnMore.visibleProperty().unbind();
        learnMore.managedProperty().unbind();
        amountLimitInfoHBox.visibleProperty().unbind();
        amountLimitInfoHBox.managedProperty().unbind();
        learnHowToBuildReputationBox.visibleProperty().unbind();
        learnHowToBuildReputationBox.managedProperty().unbind();
        warningIcon.visibleProperty().unbind();
        warningIcon.managedProperty().unbind();

        learnMore.setOnAction(null);
        linkToWiki.setOnAction(null);
        closeOverlayButton.setOnAction(null);
        learnHowToBuildReputation.setOnAction(null);

        root.setOnKeyPressed(null);
    }

    private static VBox createAndGetOverlayContent(Label amountLimitInfo,
                                                   Label linkToWikiText,
                                                   Hyperlink linkToWiki,
                                                   HBox learnHowToBuildReputationBox) {
        amountLimitInfo.setMinWidth(WizardOverlay.OVERLAY_WIDTH - 100);
        amountLimitInfo.setMaxWidth(amountLimitInfo.getMinWidth());
        amountLimitInfo.setMinHeight(Label.USE_PREF_SIZE);
        amountLimitInfo.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");

        learnHowToBuildReputationBox.setAlignment(Pos.CENTER);

        linkToWikiText.setMaxWidth(linkToWikiText.getMinWidth());
        linkToWikiText.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");

        linkToWiki.getStyleClass().addAll("normal-text", "text-fill-green");
        String tooltipText = Browser.hyperLinksGetCopiedWithoutPopup()
                ? Res.get("popup.hyperlink.copy.tooltip", linkToWiki.getText())
                : Res.get("popup.hyperlink.openInBrowser.tooltip", linkToWiki.getText());
        linkToWiki.setTooltip(new BisqTooltip(tooltipText));

        HBox linkBox = new HBox(5, linkToWikiText, linkToWiki);
        linkBox.setAlignment(Pos.BASELINE_LEFT);

        VBox.setMargin(learnHowToBuildReputationBox, new Insets(0, 0, 40, 0));
        VBox.setMargin(linkBox, new Insets(-40, 0, 0, 0));

        VBox vBox = new VBox(40, amountLimitInfo, learnHowToBuildReputationBox, linkBox);
        vBox.setPadding(WizardOverlay.TEXT_CONTENT_PADDING);
        return vBox;
    }
}
