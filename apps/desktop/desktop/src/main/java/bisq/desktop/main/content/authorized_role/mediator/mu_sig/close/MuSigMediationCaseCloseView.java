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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig.close;

import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigMediationCaseCloseView extends NavigationView<VBox, MuSigMediationCaseCloseModel, MuSigMediationCaseCloseController> {
    private final Button closeButton, closeCaseButton;

    public MuSigMediationCaseCloseView(MuSigMediationCaseCloseModel model,
                                       MuSigMediationCaseCloseController controller,
                                       VBox mediationCaseOverviewComponent,
                                       VBox mediationCaseDetailComponent,
                                       VBox mediationResultComponent) {
        super(new VBox(10), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        closeCaseButton = new Button(Res.get("authorizedRole.disputeActor.disputeCaseClose.closeCase"));
        closeCaseButton.setDefaultButton(true);
        HBox closeCaseButtonRow = new HBox(closeCaseButton);
        closeCaseButtonRow.setAlignment(Pos.CENTER);
        closeCaseButtonRow.setMaxWidth(Double.MAX_VALUE);

        Label headline = new Label(Res.get("authorizedRole.mediator.mediationCaseClose.headline"));
        headline.getStyleClass().add("bisq-text-17");
        headline.setAlignment(Pos.CENTER);
        headline.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(10,
                mediationCaseOverviewComponent,
                mediationCaseDetailComponent,
                mediationResultComponent,
                closeCaseButtonRow
        );
        VBox.setMargin(mediationResultComponent, new Insets(10, 0, 0, 0));
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(0, 20, 0, 0));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setContent(content);
        VBox.setMargin(scrollPane, new Insets(0, 80, 40, 80));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox.setMargin(headline, new Insets(-5, 0, 5, 0));
        root.getChildren().addAll(closeButtonRow, headline, scrollPane);
    }

    @Override
    protected void onViewAttached() {
        closeCaseButton.disableProperty().bind(model.getCloseCaseButtonDisabled());
        closeButton.setOnAction(e -> controller.onClose());
        closeCaseButton.setOnAction(e -> controller.onCloseCase());
    }

    @Override
    protected void onViewDetached() {
        closeCaseButton.disableProperty().unbind();
        closeButton.setOnAction(null);
        closeCaseButton.setOnAction(null);
    }
}
