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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig.details;

import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigMediationCaseDetailsView extends NavigationView<VBox, MuSigMediationCaseDetailsModel, MuSigMediationCaseDetailsController> {
    private final Button closeButton;


    public MuSigMediationCaseDetailsView(MuSigMediationCaseDetailsModel model,
                                         MuSigMediationCaseDetailsController controller,
                                         VBox mediationCaseOverviewComponent,
                                         VBox mediationCaseDetailComponent) {
        super(new VBox(), model, controller);

        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        Label headline = new Label(Res.get("authorizedRole.mediator.mediationCaseDetails.headline"));
        headline.getStyleClass().add("bisq-text-17");
        headline.setAlignment(Pos.CENTER);
        headline.setMaxWidth(Double.MAX_VALUE);

        VBox.setMargin(headline, new Insets(-5, 0, 5, 0));

        VBox content = new VBox(10,
                headline,
                mediationCaseOverviewComponent,
                mediationCaseDetailComponent
        );
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
        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        closeButton.setOnAction(null);
    }
}
