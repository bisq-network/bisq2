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

package bisq.desktop.primary.main.content.settings.userProfile;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class UserProfileView extends View<VBox, UserProfileModel, UserProfileController> {
    private final Button createNewProfileButton, deleteProfileButton;
    private Subscription chatUserDetailsPin;

    public UserProfileView(UserProfileModel model,
                           UserProfileController controller,
                           Pane userProfileSelection) {
        super(new VBox(), model, controller);

        root.setSpacing(30);
        root.setPadding(new Insets(20));

        Label selectLabel = new Label(Res.get("settings.userProfile.select").toUpperCase());
        selectLabel.getStyleClass().add("bisq-text-4");
        userProfileSelection.setMinHeight(50);
        VBox.setVgrow(selectLabel, Priority.ALWAYS);
        VBox selectionVBox = new VBox(0, selectLabel, userProfileSelection);

        createNewProfileButton = new Button(Res.get("settings.userProfile.createNewProfile"));
        createNewProfileButton.setDefaultButton(true);
        createNewProfileButton.setMinWidth(300);

        deleteProfileButton = new Button(Res.get("settings.userProfile.deleteProfile"));    //todo
        deleteProfileButton.setDefaultButton(false);
        deleteProfileButton.setMinWidth(300);

        VBox.setMargin(createNewProfileButton, new Insets(-15, 0, -20, 0));
        root.getChildren().addAll(selectionVBox, new Pane(), createNewProfileButton, deleteProfileButton, Spacer.fillVBox());
/*

        Button defaultbutton = new Button("Default button".toUpperCase());
        defaultbutton.setDefaultButton(true);

        Button defaultbuttonDis = new Button("Default button disabled".toUpperCase());
        defaultbuttonDis.setDefaultButton(true);
        defaultbuttonDis.setDisable(true);

        Button red = new Button("Red button".toUpperCase());
        red.getStyleClass().add("red-button");

        Button button1 = new Button("button".toUpperCase());

        Button button1Dis = new Button("button disabled".toUpperCase());
        button1Dis.setDisable(true);

        Button outlined = new Button("outlined button".toUpperCase());
        outlined.getStyleClass().add("outlined-button");

        Button outlinedDis = new Button("outlined button disabled".toUpperCase());
        outlinedDis.getStyleClass().add("outlined-button");
        outlinedDis.setDisable(true);

        Button text = new Button("text button".toUpperCase());
        text.getStyleClass().add("text-button");

        Button textDis = new Button("text button disabled".toUpperCase());
        textDis.getStyleClass().add("text-button");
        textDis.setDisable(true);

        ToggleButton toggleButton1 = new ToggleButton("ToggleButton".toUpperCase());
        ToggleButton toggleButtonSel = new ToggleButton("ToggleButton selected".toUpperCase());
        toggleButtonSel.setSelected(true);

        ToggleButton toggleButtonDis = new ToggleButton("ToggleButton disabled".toUpperCase());
        toggleButtonDis.setDisable(true);

        ChipButton cb = new ChipButton("Chip");
        ChipButton cb2 = new ChipButton("Chip selected");
        cb2.setSelected(true);
        MaterialTextField tf = new MaterialTextField("Text field");
        MaterialTextField tf2 = new MaterialTextField("Text field", "Enter text", "Help text");
        Hyperlink hyperlink = new Hyperlink("Hyperlink");
        Hyperlink hyperlinkVis = new Hyperlink("Hyperlink Visited");
        hyperlinkVis.setVisited(true);
        Hyperlink hyperlinkDis = new Hyperlink("Hyperlink Disable");
        hyperlinkDis.setDisable(true);

        root.getChildren().addAll(new HBox(10, defaultbutton, red, defaultbuttonDis),
                new HBox(10, button1, button1Dis),
                new HBox(10, outlined, outlinedDis),
                new HBox(10, text, textDis),
                new HBox(10, toggleButton1, toggleButtonSel, toggleButtonDis),
                new HBox(10, cb, cb2),
                new HBox(10, tf, tf2),
                new HBox(10, hyperlink, hyperlinkVis, hyperlinkDis)
        );*/
    }

    @Override
    protected void onViewAttached() {
        chatUserDetailsPin = EasyBind.subscribe(model.getUserProfileDisplayPane(), editUserProfilePane -> {
            editUserProfilePane.setMaxWidth(300);
            VBox.setMargin(editUserProfilePane, new Insets(-40, 0, 0, 0));
            VBox.setVgrow(editUserProfilePane, Priority.ALWAYS);
            root.getChildren().set(1, editUserProfilePane);
        });

        createNewProfileButton.setOnAction(e -> controller.onAddNewChatUser());
        deleteProfileButton.setOnAction(e -> controller.onDeleteChatUser());
    }

    @Override
    protected void onViewDetached() {
        chatUserDetailsPin.unsubscribe();
        createNewProfileButton.setOnAction(null);
        deleteProfileButton.setOnAction(null);
    }
}
