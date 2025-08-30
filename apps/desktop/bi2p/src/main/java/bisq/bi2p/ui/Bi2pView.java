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

package bisq.bi2p.ui;

import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import static bisq.network.i2p.router.state.RouterState.RUNNING_OK;

@Slf4j
public class Bi2pView extends StackPane {
    private final Bi2pController controller;
    private final Bi2pModel model;
    private final VBox errorBox;
    private final Label routerState, errorHeadline, errorDetails, duration, routerStateDetails, routerSetupDetails;
    private final TextArea errorMessage;
    private final Button button;
    private final ProgressBar progressBar;
    private Subscription statePin;

    public Bi2pView(Bi2pController controller, Bi2pModel model) {
        this.controller = controller;
        this.model = model;

        double stageWidth = model.getWidth();
        double stageHeight = model.getHeight();

        setPrefWidth(stageWidth);
        setPrefHeight(stageHeight);
        getStyleClass().add("background");

        ImageView logo = new ImageView();
        logo.setId("bi2p-splash");

        progressBar = new ProgressBar();
        progressBar.setMinHeight(3);
        progressBar.setMaxHeight(3);
        progressBar.setPrefWidth(450);

        routerState = new Label();
        routerState.getStyleClass().add("white-text");

        duration = new Label();
        duration.getStyleClass().add("white-text");

        HBox routerStateAndDurationBox = new HBox(12.5, routerState, getSeparator(), duration);
        routerStateAndDurationBox.setAlignment(Pos.CENTER);

        routerStateDetails = new Label();
        routerStateDetails.setWrapText(true);
        routerStateDetails.getStyleClass().addAll("router-state-details");

        //  VBox.setMargin(progressBar, new Insets(10, 0, 5, 0));
        VBox startupBox = new VBox(12.5, routerStateAndDurationBox, progressBar, routerStateDetails);
        startupBox.setAlignment(Pos.CENTER);

        Label info = new Label(Res.get("bi2p.info"));
        info.setWrapText(true);
        info.getStyleClass().add("grey-text");
        info.setTextAlignment(TextAlignment.CENTER);

        button = new Button();

        routerSetupDetails = new Label();
        routerSetupDetails.getStyleClass().add("router-setup-details");

        errorHeadline = new Label();
        errorHeadline.getStyleClass().add("error-headline");

        errorDetails = new Label();
        errorDetails.setWrapText(true);
        errorDetails.getStyleClass().add("error-details");

        errorMessage = new TextArea();
        errorMessage.setContextMenu(new ContextMenu());
        errorMessage.setEditable(false);
        errorMessage.setWrapText(true);
        errorMessage.getStyleClass().addAll("error-message");
        errorMessage.setMaxHeight(150);

        VBox.setMargin(errorMessage, new Insets(10, 40, -20, 40));
        VBox.setMargin(errorDetails, new Insets(-10, 0, 0, 0));
        errorBox = new VBox(errorHeadline, errorDetails, errorMessage);


        VBox.setMargin(info, new Insets(30, 0, 30, 0));
        VBox.setMargin(button, new Insets(20, 0, 10, 0));
        VBox contentBox = new VBox(10, logo, info, startupBox, button, errorBox, getVSpacer(), routerSetupDetails);
        contentBox.setPadding(new Insets(40, 40, 20, 40));
        contentBox.setAlignment(Pos.TOP_CENTER);

        getChildren().addAll(contentBox);
    }

    void onViewAttached() {
        routerState.textProperty().bind(model.getRouterStateString());
        duration.textProperty().bind(model.getStartupDurationString());
        progressBar.progressProperty().bind(model.getProgress());
        routerStateDetails.textProperty().bind(model.getRouterStateDetails());
        button.textProperty().bind(model.getButtonLabel());
        button.disableProperty().bind(model.getButtonDisabled());
        routerSetupDetails.textProperty().bind(model.getRouterSetupDetails());

        errorBox.visibleProperty().bind(model.getErrorBoxVisible());
        errorBox.managedProperty().bind(model.getErrorBoxVisible());
        errorHeadline.textProperty().bind(model.getErrorHeadline());
        errorMessage.textProperty().bind(model.getErrorMessage());
        errorDetails.textProperty().bind(model.getErrorMessage());

        statePin = EasyBind.subscribe(model.getRouterState(), state -> {
            if (state != null) {
                boolean isStarting = state.ordinal() < RUNNING_OK.ordinal();
                progressBar.setVisible(isStarting);
                progressBar.setManaged(isStarting);

                switch (state) {
                    case NEW, STARTING, RUNNING_TESTING -> {
                        duration.getStyleClass().add("highlight");
                    }
                    case RUNNING_OK -> {
                        routerState.getStyleClass().add("highlight");
                        duration.getStyleClass().add("highlight");
                    }
                    case RUNNING_FIREWALLED -> {
                        routerState.getStyleClass().remove("highlight");
                        routerState.getStyleClass().add("warn");
                    }
                    case STOPPING -> {
                        routerState.getStyleClass().remove("highlight");
                        routerState.getStyleClass().add("warn");
                        duration.getStyleClass().remove("highlight");
                    }
                    case RUNNING_DISCONNECTED, STOPPED, FAILED -> {
                        routerState.getStyleClass().remove("highlight");
                        routerState.getStyleClass().add("warn");
                        duration.getStyleClass().remove("highlight");
                    }
                }
            }
        });

        button.setOnAction(e -> controller.onButtonClicked());
    }

    void onViewDetached() {
        routerState.textProperty().unbind();
        duration.textProperty().unbind();
        progressBar.progressProperty().unbind();
        routerStateDetails.textProperty().unbind();
        button.textProperty().unbind();
        button.disableProperty().unbind();
        routerSetupDetails.textProperty().unbind();

        errorBox.visibleProperty().unbind();
        errorBox.managedProperty().unbind();
        errorHeadline.textProperty().unbind();
        errorMessage.textProperty().unbind();
        errorDetails.textProperty().unbind();

        statePin.unsubscribe();

        button.setOnAction(null);
    }

    private static Label getSeparator() {
        Label separator2 = new Label("|");
        separator2.setOpacity(0.75);
        separator2.getStyleClass().add("state");
        return separator2;
    }

    private static Region getVSpacer() {
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        return bottomSpacer;
    }
}