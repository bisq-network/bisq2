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

package bisq.desktop.overlay.tac.risk_ack;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.WrappingText;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TacRiskAckView extends View<VBox, TacRiskAckModel, TacRiskAckController> {
    private static final double RISK_ICON_SIZE = 32;

    private final Button nextButton, rejectButton, closeButton;
    private final CheckBox lossAcknowledged, noRecoveryAcknowledged;

    public TacRiskAckView(TacRiskAckModel model, TacRiskAckController controller) {
        super(new VBox(14), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        VBox riskSections = new VBox(14,
                createRiskSection("tac-decentr",
                        Res.get("tac.risk.p2p.title"),
                        Res.get("tac.risk.p2p.body")),
                createRiskSection("tac-warning-green",
                        Res.get("tac.risk.financial.title"),
                        Res.get("tac.risk.financial.body")),
                createRiskSection("tac-refund",
                        Res.get("tac.risk.noGuarantees.title"),
                        Res.get("tac.risk.noGuarantees.body")));
        riskSections.getStyleClass().add("tac-risk-overview");

        Label confirmHeadline = new Label(Res.get("tac.risk.confirm.headline"));
        confirmHeadline.getStyleClass().add("tac-risk-title");
        confirmHeadline.setWrapText(true);

        lossAcknowledged = new CheckBox(Res.get("tac.risk.accept.loss"));
        lossAcknowledged.getStyleClass().add("tac-risk-checkbox");
        lossAcknowledged.setFocusTraversable(false);
        lossAcknowledged.setWrapText(true);

        noRecoveryAcknowledged = new CheckBox(Res.get("tac.risk.accept.noRecovery"));
        noRecoveryAcknowledged.getStyleClass().add("tac-risk-checkbox");
        noRecoveryAcknowledged.setFocusTraversable(false);
        noRecoveryAcknowledged.setWrapText(true);

        nextButton = new Button(Res.get("action.next"));
        nextButton.setFocusTraversable(false);
        nextButton.setDefaultButton(true);

        rejectButton = new Button(Res.get("tac.reject"));
        rejectButton.getStyleClass().add("outlined-button");
        rejectButton.setFocusTraversable(false);

        closeButton = new Button(Res.get("action.close"));
        closeButton.getStyleClass().add("outlined-button");
        closeButton.setFocusTraversable(false);

        HBox buttons = new HBox(20, Spacer.fillHBox(), rejectButton, closeButton, nextButton);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(confirmHeadline, new Insets(22, 0, 0, 0));
        VBox headerSection = createHeaderSection();
        VBox.setMargin(headerSection, new Insets(0, 0, 22, 0));

        root.getChildren().addAll(headerSection,
                riskSections,
                confirmHeadline,
                lossAcknowledged,
                noRecoveryAcknowledged,
                Spacer.fillVBox(),
                buttons);
    }

    CheckBox lossAcknowledgementToggle() {
        return lossAcknowledged;
    }

    CheckBox noRecoveryAcknowledgementToggle() {
        return noRecoveryAcknowledged;
    }

    Button nextAction() {
        return nextButton;
    }

    Button rejectAction() {
        return rejectButton;
    }

    Button closeAction() {
        return closeButton;
    }

    @Override
    protected void onViewAttached() {
        lossAcknowledged.disableProperty().bind(model.getReadOnly());
        noRecoveryAcknowledged.disableProperty().bind(model.getReadOnly());
        rejectButton.visibleProperty().bind(model.getReadOnly().not());
        rejectButton.managedProperty().bind(rejectButton.visibleProperty());
        closeButton.visibleProperty().bind(model.getReadOnly());
        closeButton.managedProperty().bind(closeButton.visibleProperty());

        lossAcknowledged.selectedProperty().bindBidirectional(model.getLossAcknowledged());
        noRecoveryAcknowledged.selectedProperty().bindBidirectional(model.getNoRecoveryAcknowledged());
        nextButton.disableProperty().bind(Bindings.createBooleanBinding(() -> !model.canContinue(),
                model.getLossAcknowledged(),
                model.getNoRecoveryAcknowledged(),
                model.getReadOnly()));

        lossAcknowledged.setOnAction(e -> controller.onLossAcknowledged(lossAcknowledged.isSelected()));
        noRecoveryAcknowledged.setOnAction(e -> controller.onNoRecoveryAcknowledged(noRecoveryAcknowledged.isSelected()));
        nextButton.setOnAction(e -> controller.onNext());
        rejectButton.setOnAction(e -> controller.onReject());
        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        lossAcknowledged.selectedProperty().unbindBidirectional(model.getLossAcknowledged());
        noRecoveryAcknowledged.selectedProperty().unbindBidirectional(model.getNoRecoveryAcknowledged());
        lossAcknowledged.disableProperty().unbind();
        noRecoveryAcknowledged.disableProperty().unbind();
        rejectButton.visibleProperty().unbind();
        rejectButton.managedProperty().unbind();
        closeButton.visibleProperty().unbind();
        closeButton.managedProperty().unbind();
        nextButton.disableProperty().unbind();

        lossAcknowledged.setOnAction(null);
        noRecoveryAcknowledged.setOnAction(null);
        nextButton.setOnAction(null);
        rejectButton.setOnAction(null);
        closeButton.setOnAction(null);
    }

    private VBox createHeaderSection() {
        Label headline = new Label(Res.get("tac.risk.headline"));
        headline.getStyleClass().add("tac-headline");
        headline.setWrapText(true);
        headline.setAlignment(Pos.CENTER);
        headline.setTextAlignment(TextAlignment.CENTER);
        headline.setMaxWidth(Double.MAX_VALUE);

        VBox section = new VBox(3, headline);
        section.setAlignment(Pos.TOP_CENTER);
        section.setMaxWidth(Double.MAX_VALUE);
        return section;
    }

    private HBox createRiskSection(String imageId, String title, String body) {
        ImageView icon = createIcon(imageId, RISK_ICON_SIZE);
        icon.getStyleClass().add("tac-risk-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("tac-risk-title");
        titleLabel.setWrapText(true);

        WrappingText bodyText = new WrappingText(body, "tac-risk-text");
        bodyText.setMaxWidth(Double.MAX_VALUE);

        VBox textBox = new VBox(2, titleLabel, bodyText);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox section = new HBox(18, icon, textBox);
        section.setMaxWidth(Double.MAX_VALUE);
        section.setAlignment(Pos.CENTER_LEFT);
        section.getStyleClass().add("tac-risk-row");
        return section;
    }

    private ImageView createIcon(String imageId, double size) {
        ImageView icon = ImageUtil.getImageViewById(imageId);
        icon.setFitWidth(size);
        icon.setFitHeight(size);
        icon.setPreserveRatio(true);
        icon.setSmooth(true);
        icon.setMouseTransparent(true);
        return icon;
    }
}
