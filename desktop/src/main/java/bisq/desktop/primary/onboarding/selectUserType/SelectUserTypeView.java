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
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SelectUserTypeView extends View<ScrollPane, SelectUserTypeModel, SelectUserTypeController> {
    private final VBox vBox;
    private final Button nextButton;
    private final BisqComboBox<SelectUserTypeModel.Type> userTypeBox;
    private final Label info;

    public SelectUserTypeView(SelectUserTypeModel model, SelectUserTypeController controller) {
        super(new ScrollPane(), model, controller);

        vBox = new VBox();
        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setSpacing(30);
        vBox.getStyleClass().add("content-pane");

        root.setContent(vBox);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setFitToWidth(true);
        // We must set setFitToHeight false as otherwise text wrapping does not work at labels
        // We need to apply prefViewportHeight once we know our vbox height.
        root.setFitToHeight(false);
        root.getStyleClass().add("content-pane");

        Label headLineLabel = new Label(Res.get("satoshisquareapp.selectTraderType.headline"));
        headLineLabel.setWrapText(true);
        headLineLabel.getStyleClass().add("bisq-big-light-headline-label");
        VBox.setMargin(headLineLabel, new Insets(50, 200, 0, 200));
        VBox.setVgrow(headLineLabel, Priority.ALWAYS);

        Label subTitleLabel = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.subTitle"));
        subTitleLabel.setWrapText(true);
        subTitleLabel.setTextAlignment(TextAlignment.CENTER);
        int width = 450;
        subTitleLabel.setMaxWidth(width);
        subTitleLabel.getStyleClass().add("bisq-small-light-label-dimmed");
        VBox.setMargin(subTitleLabel, new Insets(0, 200, 0, 200));
        VBox.setVgrow(subTitleLabel, Priority.ALWAYS);

        info = new Label();
        info.setWrapText(true);
        info.setMaxWidth(width);
        info.setTextAlignment(TextAlignment.CENTER);
        info.getStyleClass().add("bisq-sub-title-label");
        VBox.setVgrow(info, Priority.ALWAYS);
        VBox.setMargin(info, new Insets(0, 0, 0, 0));

        userTypeBox = new BisqComboBox<>();
        userTypeBox.setDescription(Res.get("satoshisquareapp.selectTraderType.description"));
        userTypeBox.setPrefWidth(width);
        
        nextButton = new Button(Res.get("shared.nextStep"));
        nextButton.setDefaultButton(true);
        VBox.setMargin(nextButton, new Insets(0, 0, 50, 0));

        Pane userTypeBoxRoot = userTypeBox.getRoot();
        VBox.setMargin(userTypeBoxRoot, new Insets(0, 0, 50, userTypeBoxRoot.getWidth()));

        // todo remove that hack once design is final
        HBox vBox = Layout.hBoxWith(Spacer.fillHBox(), userTypeBoxRoot, Spacer.fillHBox());
        this.vBox.getChildren().addAll(
                headLineLabel,
                subTitleLabel,
                vBox,
                info,
                nextButton
        );
    }

    @Override
    protected void onViewAttached() {
        info.textProperty().bind(model.getInfo());
        nextButton.textProperty().bind(model.getButtonText());

        userTypeBox.setItems(model.getUserTypes());
        userTypeBox.selectItem(model.getUserTypes().get(0));
        userTypeBox.setOnAction(() -> controller.onSelect(userTypeBox.getSelectedItem()));

        nextButton.setOnAction(e -> controller.onAction());
    }

    @Override
    protected void onViewDetached() {
        info.textProperty().unbind();
        nextButton.textProperty().unbind();

        userTypeBox.setOnAction(null);
        nextButton.setOnAction(null);
    }
}
