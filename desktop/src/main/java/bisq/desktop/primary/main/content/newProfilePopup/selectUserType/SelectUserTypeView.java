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
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class SelectUserTypeView extends View<ScrollPane, SelectUserTypeModel, SelectUserTypeController> {
    private final Button nextButton;
    private final ToggleButton userTypeNewButton, userTypeExperiencedButton;
    private final ToggleGroup userTypeToggleGroup;
    private Subscription selectedUserTypeSubscription;

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

        Label headLineLabel = new Label(Res.get("onboarding.direction.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.direction.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setMaxWidth(400);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 8, 0));

        userTypeToggleGroup = new ToggleGroup();

        VBox userTypeNewBox = getUserTypeSelectionBox(Res.get("onboarding.direction.buy"), Res.get("onboarding.direction.buy.info"));
        userTypeNewButton = (ToggleButton) userTypeNewBox.getChildren().get(0);

        VBox userTypeExperiencedBox = getUserTypeSelectionBox(Res.get("onboarding.direction.sell"), Res.get("onboarding.direction.sell.info"));
        userTypeExperiencedButton = (ToggleButton) userTypeExperiencedBox.getChildren().get(0);

        HBox userTypeBox = new HBox(28, userTypeNewBox, userTypeExperiencedBox);
        VBox.setMargin(userTypeBox, new Insets(40, 0, 40, 0));
        userTypeBox.setAlignment(Pos.CENTER);

        nextButton = new Button(Res.get("next"));
        nextButton.setDefaultButton(true);

        vBox.getChildren().addAll(headLineLabel, subtitleLabel, userTypeBox, nextButton);
    }

    @Override
    protected void onViewAttached() {
        userTypeNewButton.disableProperty().bind(userTypeNewButton.selectedProperty());
        userTypeExperiencedButton.disableProperty().bind(userTypeExperiencedButton.selectedProperty());
        userTypeNewButton.setOnAction(evt -> controller.onSelect(SelectUserTypeModel.Type.NEWBIE));
        userTypeExperiencedButton.setOnAction(evt -> controller.onSelect(SelectUserTypeModel.Type.EXPERIENCED));

        selectedUserTypeSubscription = EasyBind.subscribe(model.getSelectedType(), selectedType -> {
            if (selectedType != null) {
                userTypeToggleGroup.selectToggle(selectedType == SelectUserTypeModel.Type.NEWBIE ? userTypeNewButton : userTypeExperiencedButton);

            }
        });
        nextButton.setOnAction(e -> controller.onNext());
    }

    @Override
    protected void onViewDetached() {
        userTypeNewButton.disableProperty().unbind();
        userTypeExperiencedButton.disableProperty().unbind();
        userTypeNewButton.setOnAction(null);
        userTypeExperiencedButton.setOnAction(null);
        selectedUserTypeSubscription.unsubscribe();
        nextButton.setOnAction(null);
    }

    private VBox getUserTypeSelectionBox(String title, String info) {
        ToggleButton button = new ToggleButton(title);
        button.setToggleGroup(userTypeToggleGroup);
        button.getStyleClass().setAll("bisq-button-1");
        button.setAlignment(Pos.CENTER);
        button.setMinWidth(260);
        button.setMinHeight(125);
        Label infoLabel = new Label(info);
        VBox.setMargin(infoLabel, new Insets(13, 0, 0, 0));
        infoLabel.getStyleClass().add("bisq-text-3");
        VBox box = new VBox(5, button, infoLabel);
        box.setAlignment(Pos.CENTER);

        return box;
    }
}
