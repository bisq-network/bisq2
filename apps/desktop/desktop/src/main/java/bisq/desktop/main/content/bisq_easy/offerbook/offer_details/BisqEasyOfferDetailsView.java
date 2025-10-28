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

package bisq.desktop.main.content.bisq_easy.offerbook.offer_details;

import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.TextFlowUtils;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;

// Use offer creation review screen as design template
@Slf4j
public class BisqEasyOfferDetailsView extends View<VBox, BisqEasyOfferDetailsModel, BisqEasyOfferDetailsController> {
    private final Button closeButton;
    private final Label offerDate, makersTradeTermsDescription, direction, quoteSideAmountDescription,
            baseSideAmountDescription, priceDetails, baseSidePaymentMethodDescription, baseSidePaymentMethod,
            quoteSidePaymentMethodDescription, quoteSidePaymentMethod, offerId;
    private final TextFlow quoteSideAmount, baseSideAmount, price;
    private final BisqMenuItem offerIdCopyButton;
    private final TextArea makersTradeTerms;

    public BisqEasyOfferDetailsView(BisqEasyOfferDetailsModel model, BisqEasyOfferDetailsController controller) {
        super(new VBox(), model, controller);

        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        Label headline = new Label(Res.get("bisqEasy.offerDetails.headline"));
        headline.getStyleClass().add("bisq-text-17");
        headline.setMaxWidth(Double.MAX_VALUE);
        headline.setAlignment(Pos.CENTER);

        // Content
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setAlignment(Pos.CENTER);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(gridPane, 4);
        int rowIndex = 0;

        String descriptionStyle = "offer-details-description";
        String valueStyle = "offer-details-value";
        String detailsStyle = "offer-details-details";


        // Overview
        Label overviewHeadline = new Label(Res.get("bisqEasy.offerDetails.overview").toUpperCase());
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
        Label directionDescription = new Label(Res.get("bisqEasy.offerDetails.directionDescription"));
        directionDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(directionDescription, 0, rowIndex);

        direction = new Label();
        direction.getStyleClass().add(valueStyle);
        GridPane.setColumnSpan(direction, 3);
        gridPane.add(direction, 1, rowIndex);


        // Fiat amount
        rowIndex++;
        quoteSideAmountDescription = new Label();
        quoteSideAmountDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(quoteSideAmountDescription, 0, rowIndex);

        quoteSideAmount = new TextFlow();
        quoteSideAmount.getStyleClass().add(valueStyle);
        GridPane.setColumnSpan(quoteSideAmount, 3);
        gridPane.add(quoteSideAmount, 1, rowIndex);


        // BTC amount
        rowIndex++;
        baseSideAmountDescription = new Label();
        baseSideAmountDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(baseSideAmountDescription, 0, rowIndex);

        baseSideAmount = new TextFlow();
        baseSideAmount.getStyleClass().add(valueStyle);
        GridPane.setColumnSpan(baseSideAmount, 3);
        gridPane.add(baseSideAmount, 1, rowIndex);


        // Price
        rowIndex++;
        Label priceDescription = new Label(Res.get("bisqEasy.offerDetails.priceDescription"));
        priceDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(priceDescription, 0, rowIndex);

        price = new TextFlow();
        price.getStyleClass().add(valueStyle);
        GridPane.setColumnSpan(price, 3);
        gridPane.add(price, 1, rowIndex);

        // Price details
        rowIndex++;
        Label priceDetailsDescription = new Label(Res.get("bisqEasy.offerDetails.priceDetailsDescription"));
        priceDetailsDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(priceDetailsDescription, 0, rowIndex);

        priceDetails = new Label();
        priceDetails.getStyleClass().add(valueStyle);
        GridPane.setColumnSpan(priceDetails, 3);
        gridPane.add(priceDetails, 1, rowIndex);


        // Fiat payment methods
        rowIndex++;
        quoteSidePaymentMethodDescription = new Label();
        quoteSidePaymentMethodDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(quoteSidePaymentMethodDescription, 0, rowIndex);

        quoteSidePaymentMethod = new Label();
        quoteSidePaymentMethod.getStyleClass().add(valueStyle);
        GridPane.setColumnSpan(quoteSidePaymentMethod, 3);
        gridPane.add(quoteSidePaymentMethod, 1, rowIndex);


        // Bitcoin settlement methods
        rowIndex++;
        baseSidePaymentMethodDescription = new Label();
        baseSidePaymentMethodDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(baseSidePaymentMethodDescription, 0, rowIndex);

        baseSidePaymentMethod = new Label();
        baseSidePaymentMethod.getStyleClass().add(valueStyle);
        GridPane.setColumnSpan(baseSidePaymentMethod, 3);
        gridPane.add(baseSidePaymentMethod, 1, rowIndex);


        // Details
        rowIndex++;
        Label detailsHeadline = new Label(Res.get("bisqEasy.offerDetails.details").toUpperCase());
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
        Label offerIdDescription = new Label(Res.get("bisqEasy.offerDetails.offerIdDescription"));
        offerIdDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(offerIdDescription, 0, rowIndex);

        offerId = new Label();
        offerId.getStyleClass().add(valueStyle);
        offerIdCopyButton = getBisqMenuItem(Res.get("action.copyToClipboard"));
        HBox offerIdBox = new HBox(offerId, Spacer.fillHBox(), offerIdCopyButton);
        GridPane.setColumnSpan(offerIdBox, 3);
        gridPane.add(offerIdBox, 1, rowIndex);


        // Offer date
        rowIndex++;
        Label offerDateDescription = new Label(Res.get("bisqEasy.offerDetails.dateDescription"));
        offerDateDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(offerDateDescription, 0, rowIndex);

        offerDate = new Label();
        offerDate.getStyleClass().add(valueStyle);
        GridPane.setColumnSpan(offerDate, 3);
        gridPane.add(offerDate, 1, rowIndex);


        // Makers trade terms (optional)
        rowIndex++;
        makersTradeTermsDescription = new Label(Res.get("bisqEasy.offerDetails.makersTradeTermsDescription"));
        makersTradeTermsDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(makersTradeTermsDescription, 0, rowIndex);

        makersTradeTerms = new TextArea();
        makersTradeTerms.setMaxHeight(70);
        makersTradeTerms.setWrapText(true);
        makersTradeTerms.getStyleClass().add(valueStyle);
        GridPane.setMargin(makersTradeTerms, new Insets(0, 0, 0, -7));
        GridPane.setColumnSpan(makersTradeTerms, 3);
        gridPane.add(makersTradeTerms, 1, rowIndex);

        VBox content = new VBox(10, headline, gridPane);
        content.setAlignment(Pos.CENTER_LEFT);

        root.setAlignment(Pos.TOP_CENTER);
        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        VBox.setMargin(content, new Insets(-40, 80, 0, 80));
        VBox.setVgrow(content, Priority.ALWAYS);
        root.getChildren().addAll(closeButtonRow, content);
    }

    @Override
    protected void onViewAttached() {
        direction.setText(model.getDirection());

        quoteSideAmountDescription.setText(Res.get("bisqEasy.offerDetails.quoteSideAmountDescription", model.getQuoteSideCurrencyCode()));
        TextFlowUtils.updateTextFlow(quoteSideAmount, model.getQuoteSideAmount());

        baseSideAmountDescription.setText(Res.get("bisqEasy.offerDetails.baseSideAmountDescription", model.getBaseSideCurrencyCode()));
        TextFlowUtils.updateTextFlow(baseSideAmount, model.getBaseSideAmount());

        TextFlowUtils.updateTextFlow(price, model.getPrice());
        priceDetails.setText(model.getPriceDetails());

        quoteSidePaymentMethodDescription.setText(model.getQuoteSidePaymentMethodDescription());
        quoteSidePaymentMethod.setText(model.getQuoteSidePaymentMethods());

        baseSidePaymentMethodDescription.setText(model.getBaseSidePaymentMethodDescription());
        baseSidePaymentMethod.setText(model.getBaseSidePaymentMethods());

        offerId.setText(model.getId());
        offerDate.setText(model.getDate());
        makersTradeTerms.setText(model.getMakersTradeTerms());
        makersTradeTermsDescription.setVisible(model.getMakersTradeTermsVisible());
        makersTradeTermsDescription.setManaged(model.getMakersTradeTermsVisible());
        makersTradeTerms.setVisible(model.getMakersTradeTermsVisible());
        makersTradeTerms.setManaged(model.getMakersTradeTermsVisible());

        closeButton.setOnAction(e -> controller.onClose());
        offerIdCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getId()));
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
        offerIdCopyButton.setOnAction(null);
    }

    private static BisqMenuItem getBisqMenuItem(String tooltip) {
        BisqMenuItem bisqMenuItem = new BisqMenuItem("copy-grey", "copy-white");
        bisqMenuItem.setTooltip(tooltip);
        return bisqMenuItem;
    }

    private Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: -bisq-border-color-grey");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }
}
