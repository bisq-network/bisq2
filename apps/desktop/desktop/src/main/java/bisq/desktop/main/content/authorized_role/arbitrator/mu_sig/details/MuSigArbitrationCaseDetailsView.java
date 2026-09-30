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

package bisq.desktop.main.content.authorized_role.arbitrator.mu_sig.details;

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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.desktop.components.helpers.LabeledValueRowFactory.createSeparatorLine;

@Slf4j
public class MuSigArbitrationCaseDetailsView extends
        NavigationView<VBox, MuSigArbitrationCaseDetailsModel, MuSigArbitrationCaseDetailsController> {
    private final Button closeButton;
    private final VBox arbitrationResultContainer, mediationResultSection;

    public MuSigArbitrationCaseDetailsView(MuSigArbitrationCaseDetailsModel model,
                                           MuSigArbitrationCaseDetailsController controller,
                                           VBox arbitrationCaseOverviewComponent,
                                           VBox arbitrationCaseDetailComponent,
                                           VBox arbitrationCaseMediationResultComponent) {
        super(new VBox(10), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        Label headline = new Label(Res.get("authorizedRole.arbitrator.arbitrationCaseDetails.headline"));
        headline.getStyleClass().add("bisq-text-17");
        headline.setAlignment(Pos.CENTER);
        headline.setMaxWidth(Double.MAX_VALUE);

        VBox.setMargin(headline, new Insets(-5, 0, 5, 0));

        VBox overviewSection = createSection(Res.get("bisqEasy.openTrades.tradeDetails.overview"),
                0,
                arbitrationCaseOverviewComponent);
        VBox detailsSection = createSection(Res.get("bisqEasy.openTrades.tradeDetails.details"),
                15,
                arbitrationCaseDetailComponent);
        mediationResultSection = createSection(Res.get("authorizedRole.mediator.mediationCaseDetails.mediationResult"),
                15,
                arbitrationCaseMediationResultComponent);

        arbitrationResultContainer = new VBox();
        VBox arbitrationResultSection = createSection(Res.get("authorizedRole.arbitrator.arbitrationCaseDetails.arbitrationResult"),
                15,
                arbitrationResultContainer);
        arbitrationResultSection.setVisible(false);
        arbitrationResultSection.setManaged(false);

        VBox content = new VBox(10,
                overviewSection,
                detailsSection,
                arbitrationResultSection,
                mediationResultSection
        );
        content.setPadding(new Insets(0, 20, 0, 0));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setContent(content);
        VBox.setMargin(scrollPane, new Insets(0, 80, 40, 80));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(closeButtonRow, headline, scrollPane);
    }

    public void setArbitrationResultComponent(Optional<VBox> optionalArbitrationResultComponent) {
        arbitrationResultContainer.getChildren().clear();
        optionalArbitrationResultComponent.ifPresent(component -> arbitrationResultContainer.getChildren().add(component));
        boolean hasArbitrationResultComponent = optionalArbitrationResultComponent.isPresent();
        Region arbitrationResultSection = (Region) arbitrationResultContainer.getParent();
        arbitrationResultSection.setManaged(hasArbitrationResultComponent);
        arbitrationResultSection.setVisible(hasArbitrationResultComponent);
        mediationResultSection.setManaged(!hasArbitrationResultComponent);
        mediationResultSection.setVisible(!hasArbitrationResultComponent);
    }

    private static VBox createSection(String sectionLabelText, double sectionLabelTopMargin, VBox sectionContent) {
        Label sectionLabel = new Label(sectionLabelText.toUpperCase());
        sectionLabel.getStyleClass().addAll("text-fill-grey-dimmed", "font-light", "medium-text");
        VBox.setMargin(sectionLabel, new Insets(sectionLabelTopMargin, 0, -5, 0));
        Region sectionLine = createSeparatorLine();
        VBox section = new VBox(10, sectionLabel, sectionLine, sectionContent);
        section.setAlignment(Pos.CENTER_LEFT);
        return section;
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
