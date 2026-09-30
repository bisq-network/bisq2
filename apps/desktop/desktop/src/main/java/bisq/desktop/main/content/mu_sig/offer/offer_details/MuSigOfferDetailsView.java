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

package bisq.desktop.main.content.mu_sig.offer.offer_details;

import bisq.desktop.common.Icons;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.TextFlowUtils;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class MuSigOfferDetailsView extends View<VBox, MuSigOfferDetailsModel, MuSigOfferDetailsController> {

    public static final String DESCRIPTION_STYLE = "offer-details-description";
    public static final String VALUE_STYLE = "offer-details-value";

    private final Button closeButton;
    private final Label offerDate, makersTradeTermsDescription, direction, quoteSideAmountDescription,
            baseSideAmountDescription, quoteSidePaymentMethodDescription, quoteSidePaymentMethod, offerId,
            securityDepositInfoIcon;
    private final TextFlow quoteSideAmount, baseSideAmount, price, priceDetails, securityDeposit, fee;
    private final BisqMenuItem offerIdCopyButton;
    private final TextArea makersTradeTerms;

    public MuSigOfferDetailsView(MuSigOfferDetailsModel model, MuSigOfferDetailsController controller) {
        super(new VBox(), model, controller);

        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        Label headline = new Label(Res.get("muSig.offer.details.headline"));
        headline.getStyleClass().add("bisq-text-17");
        headline.setMaxWidth(Double.MAX_VALUE);
        headline.setAlignment(Pos.CENTER);

        // Content
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setAlignment(Pos.CENTER);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(gridPane, 4);
        int rowIndex = -1;


        // Overview
        rowIndex++;
        Label overviewHeadline = new Label(Res.get("muSig.offer.details.overview").toUpperCase());
        overviewHeadline.getStyleClass().addAll("text-fill-grey-dimmed", "font-light", "medium-text");
        GridPane.setMargin(overviewHeadline, new Insets(5, 0, 0, 0));
        GridPane.setColumnSpan(overviewHeadline, 4);
        gridPane.add(overviewHeadline, 0, rowIndex);

        rowIndex++;
        Region line1 = getLine();
        GridPane.setMargin(line1, new Insets(-10, 0, -5, 0));
        GridPane.setColumnSpan(line1, 4);
        gridPane.add(line1, 0, rowIndex);


        // Direction
        rowIndex++;
        Label directionDescription = new Label(Res.get("muSig.offer.details.directionDescription"));
        directionDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(directionDescription, 0, rowIndex);

        direction = new Label();
        direction.getStyleClass().add(VALUE_STYLE);
        GridPane.setColumnSpan(direction, 3);
        gridPane.add(direction, 1, rowIndex);


        // Fiat amount
        rowIndex++;
        quoteSideAmountDescription = new Label();
        quoteSideAmountDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(quoteSideAmountDescription, 0, rowIndex);

        quoteSideAmount = new TextFlow();
        quoteSideAmount.getStyleClass().add(VALUE_STYLE);
        GridPane.setColumnSpan(quoteSideAmount, 3);
        gridPane.add(quoteSideAmount, 1, rowIndex);


        // BTC amount
        rowIndex++;
        baseSideAmountDescription = new Label();
        baseSideAmountDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(baseSideAmountDescription, 0, rowIndex);

        baseSideAmount = new TextFlow();
        baseSideAmount.getStyleClass().add(VALUE_STYLE);
        GridPane.setColumnSpan(baseSideAmount, 3);
        gridPane.add(baseSideAmount, 1, rowIndex);


        // Price
        rowIndex++;
        Label priceDescription = new Label(Res.get("muSig.offer.details.priceDescription"));
        priceDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(priceDescription, 0, rowIndex);

        price = new TextFlow();
        price.getStyleClass().add(VALUE_STYLE);
        GridPane.setColumnSpan(price, 3);
        gridPane.add(price, 1, rowIndex);

        // Price details
        rowIndex++;
        Label priceDetailsDescription = new Label(Res.get("muSig.offer.details.priceDetailsDescription"));
        priceDetailsDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(priceDetailsDescription, 0, rowIndex);

        priceDetails = new TextFlow();
        priceDetails.getStyleClass().add(VALUE_STYLE);
        GridPane.setColumnSpan(priceDetails, 3);
        gridPane.add(priceDetails, 1, rowIndex);


        // Fiat payment methods
        rowIndex++;
        quoteSidePaymentMethodDescription = new Label();
        quoteSidePaymentMethodDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(quoteSidePaymentMethodDescription, 0, rowIndex);

        quoteSidePaymentMethod = new Label();
        quoteSidePaymentMethod.getStyleClass().add(VALUE_STYLE);
        GridPane.setColumnSpan(quoteSidePaymentMethod, 3);
        gridPane.add(quoteSidePaymentMethod, 1, rowIndex);


        // Details
        rowIndex++;
        Label detailsHeadline = new Label(Res.get("muSig.offer.details.details").toUpperCase());
        detailsHeadline.getStyleClass().addAll("text-fill-grey-dimmed", "font-light", "medium-text");
        GridPane.setMargin(detailsHeadline, new Insets(20, 0, 0, 0));
        GridPane.setColumnSpan(detailsHeadline, 4);
        gridPane.add(detailsHeadline, 0, rowIndex);

        rowIndex++;
        Region line2 = getLine();
        GridPane.setMargin(line2, new Insets(-10, 0, -5, 0));
        GridPane.setColumnSpan(line2, 4);
        gridPane.add(line2, 0, rowIndex);


        // OfferId
        rowIndex++;
        Label offerIdDescription = new Label(Res.get("muSig.offer.details.offerIdDescription"));
        offerIdDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(offerIdDescription, 0, rowIndex);

        offerId = new Label();
        offerId.getStyleClass().add(VALUE_STYLE);
        offerIdCopyButton = getCopyButton(Res.get("action.copyToClipboard"));
        HBox offerIdBox = new HBox(offerId, Spacer.fillHBox(), offerIdCopyButton);
        GridPane.setColumnSpan(offerIdBox, 3);
        gridPane.add(offerIdBox, 1, rowIndex);


        // Offer date
        rowIndex++;
        Label offerDateDescription = new Label(Res.get("muSig.offer.details.dateDescription"));
        offerDateDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(offerDateDescription, 0, rowIndex);

        offerDate = new Label();
        offerDate.getStyleClass().add(VALUE_STYLE);
        GridPane.setColumnSpan(offerDate, 3);
        gridPane.add(offerDate, 1, rowIndex);


        // Security deposit
        rowIndex++;
        Label securityDepositDescription = new Label(Res.get("muSig.offer.details.securityDeposit.description"));
        securityDepositDescription.getStyleClass().add(DESCRIPTION_STYLE);

        securityDepositInfoIcon = Icons.getIcon(AwesomeIcon.INFO_SIGN, "1.1em");
        securityDepositInfoIcon.getStyleClass().add("text-fill-grey-dimmed");

        HBox.setMargin(securityDepositInfoIcon, new Insets(0.5, 0, 0, 0));
        HBox securityDepositDescriptionHBox = new HBox(7.5, securityDepositDescription, securityDepositInfoIcon);
        gridPane.add(securityDepositDescriptionHBox, 0, rowIndex);

        securityDeposit = new TextFlow();
        securityDeposit.getStyleClass().add(VALUE_STYLE);
        GridPane.setColumnSpan(securityDeposit, 3);
        gridPane.add(securityDeposit, 1, rowIndex);


        // Fees
        rowIndex++;
        Label feeInfoDescription = new Label(Res.get("muSig.offer.details.feeDescription"));
        feeInfoDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(feeInfoDescription, 0, rowIndex);

        fee = new TextFlow();
        fee.getStyleClass().add(VALUE_STYLE);
        GridPane.setColumnSpan(fee, 3);
        gridPane.add(fee, 1, rowIndex);


        // Makers trade terms (optional)
        rowIndex++;
        makersTradeTermsDescription = new Label(Res.get("muSig.offer.details.makersTradeTermsDescription"));
        makersTradeTermsDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(makersTradeTermsDescription, 0, rowIndex);

        makersTradeTerms = new TextArea();
        makersTradeTerms.setEditable(false);
        makersTradeTerms.setMaxHeight(70);
        makersTradeTerms.setWrapText(true);
        makersTradeTerms.getStyleClass().add(VALUE_STYLE);
        GridPane.setMargin(makersTradeTerms, new Insets(0, 0, 0, -7));
        GridPane.setColumnSpan(makersTradeTerms, 3);
        gridPane.add(makersTradeTerms, 1, rowIndex);


        // Set up scroll pane for content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setMargin(scrollPane, new Insets(0, 80, 40, 80));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        gridPane.setPadding(new Insets(0, 20, 0, 0));
        scrollPane.setContent(gridPane);

        root.setAlignment(Pos.TOP_CENTER);
        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        root.getChildren().addAll(closeButtonRow, headline, scrollPane);
    }

    @Override
    protected void onViewAttached() {
        direction.setText(model.getDirection());

        quoteSideAmountDescription.setText(Res.get("muSig.offer.details.quoteSideAmountDescription", model.getQuoteSideCurrencyCode()));
        TextFlowUtils.updateTextFlow(quoteSideAmount, model.getQuoteSideAmount());

        baseSideAmountDescription.setText(Res.get("muSig.offer.details.baseSideAmountDescription", model.getBaseSideCurrencyCode()));
        TextFlowUtils.updateTextFlow(baseSideAmount, model.getBaseSideAmount());

        TextFlowUtils.updateTextFlow(price, model.getPrice());
        TextFlowUtils.updateTextFlow(priceDetails, model.getPriceDetails());

        quoteSidePaymentMethodDescription.setText(model.getQuoteSidePaymentMethodDescription());
        quoteSidePaymentMethod.setText(model.getQuoteSidePaymentMethods());

        offerId.setText(model.getId());
        offerDate.setText(model.getDate());

        TextFlowUtils.updateTextFlow(securityDeposit, model.getSecurityDeposit());
        securityDepositInfoIcon.setTooltip(new BisqTooltip(Res.get("muSig.offer.details.securityDeposit.info")));

        TextFlowUtils.updateTextFlow(fee, model.getFee());

        makersTradeTerms.setText(model.getMakersTradeTerms());
        makersTradeTermsDescription.setVisible(model.isMakersTradeTermsVisible());
        makersTradeTermsDescription.setManaged(model.isMakersTradeTermsVisible());
        makersTradeTerms.setVisible(model.isMakersTradeTermsVisible());
        makersTradeTerms.setManaged(model.isMakersTradeTermsVisible());

        closeButton.setOnAction(e -> controller.onClose());
        offerIdCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getId()));
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
        offerIdCopyButton.setOnAction(null);
        securityDepositInfoIcon.setTooltip(null);
    }

    private static BisqMenuItem getCopyButton(String tooltip) {
        BisqMenuItem bisqMenuItem = new BisqMenuItem("copy-grey", "copy-white");
        bisqMenuItem.setTooltip(tooltip);
        return bisqMenuItem;
    }

    private static Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.getStyleClass().add("separator-line");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }
}
