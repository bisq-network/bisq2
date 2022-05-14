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

package bisq.desktop.primary.main.content.newProfilePopup.selectUserType;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.RoboIconWithId;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SelectUserTypeView extends View<ScrollPane, SelectUserTypeModel, SelectUserTypeController> {
    private final Button nextButton, backButton;
    private final AutoCompleteComboBox<SelectUserTypeModel.Type> userTypeBox;
    private final Label info;

    public SelectUserTypeView(SelectUserTypeModel model, SelectUserTypeController controller) {
        super(new ScrollPane(), model, controller);

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setSpacing(8);
        vBox.getStyleClass().add("bisq-content-bg");
        vBox.setPadding(new Insets(30, 0, 30, 0));

        root.setContent(vBox);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setFitToWidth(true);
        // We must set setFitToHeight false as otherwise text wrapping does not work at labels
        // We need to apply prefViewportHeight once we know our vbox height.
        root.setFitToHeight(false);
        root.getStyleClass().add("bisq-content-bg");

        Label headLineLabel = new Label(Res.get("satoshisquareapp.selectTraderType.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setMaxWidth(450);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 8, 0));

        info = new Label();
        info.setWrapText(true);
        info.setMaxWidth(450);
        info.setTextAlignment(TextAlignment.CENTER);
        info.getStyleClass().add("bisq-sub-title-label");
        VBox.setVgrow(info, Priority.ALWAYS);
        VBox.setMargin(info, new Insets(0, 0, 28, 0));

        RoboIconWithId roboIconWithId = new RoboIconWithId(300);
        roboIconWithId.setProfileId(model.getProfileId());
        roboIconWithId.setRoboHashImage(model.getRoboHashNode());
        roboIconWithId.setMouseTransparent(false);
        VBox.setMargin(roboIconWithId, new Insets(0, 0, 28, 0));

        userTypeBox = new AutoCompleteComboBox<>(model.getUserTypes(), Res.get("satoshisquareapp.selectTraderType.description"));
        userTypeBox.setPrefWidth(300);
        VBox.setMargin(userTypeBox, new Insets(0, 0, 28, 0));

        backButton = new Button(Res.get("back"));
        nextButton = new Button(Res.get("shared.nextStep"));
        nextButton.setDefaultButton(true);
        HBox buttons = Layout.hBoxWith(backButton, nextButton);
        buttons.setAlignment(Pos.CENTER);

        vBox.getChildren().addAll(
                headLineLabel,
                subtitleLabel,
                roboIconWithId,
                userTypeBox,
                /* info,*/
                buttons
        );
    }

    @Override
    protected void onViewAttached() {
        info.textProperty().bind(model.getInfo());

        userTypeBox.getSelectionModel().select(model.getUserTypes().get(0));
        userTypeBox.setOnAction(e -> controller.onSelect(userTypeBox.getSelectionModel().getSelectedItem()));

        nextButton.setOnAction(e -> controller.onAction());
        backButton.setOnAction(e -> controller.onGoBack());
    }

    @Override
    protected void onViewDetached() {
        info.textProperty().unbind();
        nextButton.textProperty().unbind();

        userTypeBox.setOnAction(null);
        nextButton.setOnAction(null);
    }
}
