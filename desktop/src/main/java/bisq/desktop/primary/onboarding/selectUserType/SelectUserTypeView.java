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

package bisq.desktop.primary.onboarding.selectUserType;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.SectionBox;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class SelectUserTypeView extends View<HBox, SelectUserTypeModel, SelectUserTypeController> {
    private final ComboBox<SelectUserTypeModel.Type> userTypeBox;
    private final BisqLabel info;
    private final BisqButton button;

    public SelectUserTypeView(SelectUserTypeModel model, SelectUserTypeController controller) {
        super(new HBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        // root.getStyleClass().add("content-pane");
        root.setFillHeight(false);

        ImageView roboIconImageView = new ImageView();
        roboIconImageView.setImage(model.getRoboHashImage());
        VBox.setMargin(roboIconImageView, new Insets(10, 0, 0, 0));

        Label userName = new Label(model.getChatUserName());
        userName.setStyle("-fx-background-color: -bs-content-background-gray;-fx-text-fill: -fx-dark-text-color;");
        userName.setMaxWidth(300);
        userName.setMinWidth(300);
        userName.setAlignment(Pos.CENTER);
        userName.setPadding(new Insets(7, 7, 7, 7));
        VBox.setMargin(userName, new Insets(-30, 0, 0, 0));

        userTypeBox = new BisqComboBox<>();
        userTypeBox.setItems(model.getUserTypes());
        userTypeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SelectUserTypeModel.Type value) {
                return value != null ? value.getDisplayString() : "";
            }

            @Override
            public SelectUserTypeModel.Type fromString(String string) {
                return null;
            }
        });

        info = new BisqLabel(Res.get("satoshisquareapp.setDefaultUserProfile.tryOther.info"));
        info.setWrapText(true);

        button = new BisqButton();
        button.setActionButton(true);

        SectionBox sectionBox = new SectionBox(Res.get("satoshisquareapp.selectTraderType.headline"));
        sectionBox.setPrefWidth(450);
        sectionBox.getChildren().addAll(roboIconImageView, userName, userTypeBox, info, button);

        root.getChildren().addAll(sectionBox);
    }

    @Override
    protected void onViewAttached() {
        info.textProperty().bind(model.getInfo());
        button.textProperty().bind(model.getButtonText());
        
        userTypeBox.getSelectionModel().select(0);
        userTypeBox.setOnAction(e -> controller.onSelect(userTypeBox.getSelectionModel().getSelectedItem()));
        
        button.setOnAction(e -> controller.onAction());
    }

    @Override
    protected void onViewDetached() {
        info.textProperty().unbind();
        button.textProperty().unbind();
        
        userTypeBox.setOnAction(null);
        button.setOnAction(null);
    }

}
