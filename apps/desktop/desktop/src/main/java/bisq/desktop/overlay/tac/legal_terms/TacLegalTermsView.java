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

package bisq.desktop.overlay.tac.legal_terms;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TacLegalTermsView extends View<VBox, TacLegalTermsModel, TacLegalTermsController> {
    private final Button acceptButton, rejectButton, closeButton, backButton;
    private final CheckBox confirmCheckBox;

    public TacLegalTermsView(TacLegalTermsModel model, TacLegalTermsController controller) {
        super(new VBox(16), model, controller);

        root.setAlignment(Pos.TOP_LEFT);
        root.setFillWidth(true);

        Label headline = new Label(Res.get("tac.legal.headline"));
        headline.getStyleClass().add("tac-headline");
        headline.setWrapText(true);
        headline.setAlignment(Pos.CENTER);
        headline.setTextAlignment(TextAlignment.CENTER);
        headline.setMaxWidth(Double.MAX_VALUE);

        VBox legalTextContent = new VBox(12,
                createLegalSection("1.", Res.get("tac.legal.section1.title"), Res.get("tac.legal.section1.body")),
                createLegalSection("2.", Res.get("tac.legal.section2.title"), Res.get("tac.legal.section2.body")),
                createLegalSection("3.", Res.get("tac.legal.section3.title"), Res.get("tac.legal.section3.body")),
                createLegalSection("4.", Res.get("tac.legal.section4.title"), Res.get("tac.legal.section4.body")),
                createLegalSection("5.", Res.get("tac.legal.section5.title"), Res.get("tac.legal.section5.body")),
                createLegalSection("6.", Res.get("tac.legal.section6.title"), Res.get("tac.legal.section6.body")),
                createLegalSection("7.", Res.get("tac.legal.section7.title"), Res.get("tac.legal.section7.body")));
        legalTextContent.getStyleClass().add("tac-legal-content");
        legalTextContent.setMaxWidth(Double.MAX_VALUE);

        ScrollPane legalTextScrollPane = new ScrollPane(legalTextContent);
        legalTextScrollPane.getStyleClass().add("tac-legal-scroll-pane");
        legalTextScrollPane.setFitToWidth(true);
        legalTextScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        legalTextScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        legalTextScrollPane.setMaxWidth(Double.MAX_VALUE);
        legalTextScrollPane.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(legalTextScrollPane, Priority.ALWAYS);
        VBox.setMargin(legalTextScrollPane, new Insets(8, 0, 8, 0));

        confirmCheckBox = new CheckBox(Res.get("tac.legal.confirm"));
        confirmCheckBox.getStyleClass().add("tac-legal-checkbox");
        confirmCheckBox.setFocusTraversable(false);
        confirmCheckBox.setWrapText(true);

        acceptButton = new Button(Res.get("tac.accept"));
        acceptButton.setFocusTraversable(false);

        rejectButton = new Button(Res.get("tac.reject"));
        rejectButton.getStyleClass().add("outlined-button");
        rejectButton.setFocusTraversable(false);

        closeButton = new Button(Res.get("action.close"));
        closeButton.getStyleClass().add("outlined-button");
        closeButton.setFocusTraversable(false);

        backButton = new Button(Res.get("action.back"));
        backButton.setFocusTraversable(false);

        HBox buttons = new HBox(20, backButton, Spacer.fillHBox(), rejectButton, acceptButton, closeButton);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setMaxWidth(Double.MAX_VALUE);

        root.getChildren().addAll(headline,
                legalTextScrollPane,
                confirmCheckBox,
                buttons);
    }

    CheckBox confirmationToggle() {
        return confirmCheckBox;
    }

    Button acceptAction() {
        return acceptButton;
    }

    Button rejectAction() {
        return rejectButton;
    }

    Button closeAction() {
        return closeButton;
    }

    Button backAction() {
        return backButton;
    }

    private HBox createLegalSection(String number, String title, String body) {
        Label numberLabel = new Label(number);
        numberLabel.getStyleClass().add("tac-legal-section-number");
        numberLabel.setMinWidth(24);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("tac-legal-section-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        Label bodyLabel = new Label(body);
        bodyLabel.getStyleClass().add("tac-legal-section-body");
        bodyLabel.setWrapText(true);
        bodyLabel.setMaxWidth(Double.MAX_VALUE);

        VBox textBox = new VBox(4, titleLabel, bodyLabel);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox section = new HBox(8, numberLabel, textBox);
        section.setAlignment(Pos.TOP_LEFT);
        section.setMaxWidth(Double.MAX_VALUE);
        return section;
    }

    @Override
    protected void onViewAttached() {
        confirmCheckBox.selectedProperty().bindBidirectional(model.getConfirmed());

        confirmCheckBox.disableProperty().bind(model.getReadOnly());
        acceptButton.visibleProperty().bind(model.getReadOnly().not());
        acceptButton.managedProperty().bind(acceptButton.visibleProperty());
        rejectButton.visibleProperty().bind(model.getReadOnly().not());
        rejectButton.managedProperty().bind(rejectButton.visibleProperty());
        closeButton.visibleProperty().bind(model.getReadOnly());
        closeButton.managedProperty().bind(closeButton.visibleProperty());

        acceptButton.disableProperty().bind(model.getConfirmed().not());
        acceptButton.defaultButtonProperty().bind(model.getConfirmed().and(model.getReadOnly().not()));
        closeButton.defaultButtonProperty().bind(model.getReadOnly());

        acceptButton.setOnAction(e -> controller.onAccept());
        rejectButton.setOnAction(e -> controller.onReject());
        closeButton.setOnAction(e -> controller.onClose());
        backButton.setOnAction(e -> controller.onBack());
    }

    @Override
    protected void onViewDetached() {
        confirmCheckBox.selectedProperty().unbindBidirectional(model.getConfirmed());
        confirmCheckBox.disableProperty().unbind();
        acceptButton.visibleProperty().unbind();
        acceptButton.managedProperty().unbind();
        rejectButton.visibleProperty().unbind();
        rejectButton.managedProperty().unbind();
        closeButton.visibleProperty().unbind();
        closeButton.managedProperty().unbind();
        acceptButton.disableProperty().unbind();
        acceptButton.defaultButtonProperty().unbind();
        closeButton.defaultButtonProperty().unbind();
        acceptButton.setOnAction(null);
        rejectButton.setOnAction(null);
        closeButton.setOnAction(null);
        backButton.setOnAction(null);
    }
}
