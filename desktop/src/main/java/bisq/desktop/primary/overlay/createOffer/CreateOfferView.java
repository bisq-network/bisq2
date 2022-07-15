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

package bisq.desktop.primary.overlay.createOffer;

import bisq.common.data.Triple;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.primary.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;

@Slf4j
public class CreateOfferView extends NavigationView<VBox, CreateOfferModel, CreateOfferController> {
    public static final double TOP_PANE_HEIGHT = 55;
    private static final double OPACITY = 0.35;

    private final Button skipButton;
    private final List<Label> navigationProgressLabelList;
    private final HBox topPaneBox;
    private final Button nextButton, backButton;
    private final HBox buttons;
    private final VBox content;
    private final ChangeListener<Number> currentIndexListener;
    private Subscription topPaneBoxVisibleSubscription;
    private Scene rootScene;

    public CreateOfferView(CreateOfferModel model, CreateOfferController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        Triple<HBox, Button, List<Label>> topPane = getTopPane();
        topPaneBox = topPane.getFirst();
        skipButton = topPane.getSecond();
        navigationProgressLabelList = topPane.getThird();

        nextButton = new Button(Res.get("next"));
        nextButton.setDefaultButton(true);

        backButton = new Button(Res.get("back"));
        buttons = new HBox(10, backButton, nextButton);
        buttons.setAlignment(Pos.CENTER);

        content = new VBox();
        content.setMinHeight(420);
        content.setMaxHeight(420);
        root.getChildren().addAll(topPaneBox, content, Spacer.fillVBox(), buttons);

        VBox.setMargin(buttons, new Insets(0, 0, 40, 0));
        model.getView().addListener((observable, oldValue, newValue) -> {
            content.getChildren().clear();
            if (newValue != null) {
                Region childRoot = newValue.getRoot();
                content.getChildren().add(childRoot);
                if (oldValue != null) {
                    if (model.isAnimateRightOut()) {
                        Transitions.transitRightOut(childRoot, oldValue.getRoot());
                    } else {
                        Transitions.transitLeftOut(childRoot, oldValue.getRoot());
                    }
                } else {
                    Transitions.fadeIn(childRoot);
                }
            }
        });

        currentIndexListener = (observable, oldValue, newValue) -> applyProgress(newValue.intValue(), true);
    }

    @Override
    protected void onViewAttached() {
        nextButton.textProperty().bind(model.getNextButtonText());
        backButton.textProperty().bind(model.getBackButtonText());

        nextButton.visibleProperty().bind(model.getNextButtonVisible());
        nextButton.managedProperty().bind(model.getNextButtonVisible());
        backButton.visibleProperty().bind(model.getBackButtonVisible());
        backButton.managedProperty().bind(model.getBackButtonVisible());
        skipButton.visibleProperty().bind(model.getSkipButtonVisible());
        topPaneBox.visibleProperty().bind(model.getTopPaneBoxVisible());
        nextButton.disableProperty().bind(model.getNextButtonDisabled());

        topPaneBoxVisibleSubscription = EasyBind.subscribe(model.getTopPaneBoxVisible(), visible -> {
            if (visible) {
                VBox.setMargin(buttons, new Insets(0, 0, 40, 0));
            } else {
                VBox.setMargin(buttons, new Insets(0, 0, 240, 0));
            }
        });

        model.getCurrentIndex().addListener(currentIndexListener);
        applyProgress(model.getCurrentIndex().get(), false);

        nextButton.setOnAction(e -> controller.onNext());
        backButton.setOnAction(evt -> controller.onBack());
        skipButton.setOnAction(e -> controller.onSkip());
        rootScene = root.getScene();
        rootScene.setOnKeyReleased(keyEvent -> {
            KeyHandlerUtil.handleShutDownKeyEvent(keyEvent, controller::onQuit);
            KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, controller::onSkip);
            KeyHandlerUtil.handleDevModeKeyEvent(keyEvent);
        });
    }

    @Override
    protected void onViewDetached() {
        nextButton.textProperty().unbind();
        backButton.textProperty().unbind();
        skipButton.textProperty().unbind();

        nextButton.visibleProperty().unbind();
        nextButton.managedProperty().unbind();
        backButton.visibleProperty().unbind();
        backButton.managedProperty().unbind();
        skipButton.visibleProperty().unbind();
        topPaneBox.visibleProperty().unbind();
        nextButton.disableProperty().unbind();

        nextButton.setOnAction(null);
        backButton.setOnAction(null);
        skipButton.setOnAction(null);
        rootScene.setOnKeyReleased(null);
        model.getCurrentIndex().removeListener(currentIndexListener);
        topPaneBoxVisibleSubscription.unsubscribe();
        boolean firstOpened = false;
    }

    private Triple<HBox, Button, List<Label>> getTopPane() {
        Label direction = getTopPaneLabel(Res.get("onboarding.navProgress.direction"));
        Label market = getTopPaneLabel(Res.get("onboarding.navProgress.market"));
        Label amount = getTopPaneLabel(Res.get("onboarding.navProgress.amount"));
        Label method = getTopPaneLabel(Res.get("onboarding.navProgress.method"));
        Label complete = getTopPaneLabel(Res.get("onboarding.navProgress.review"));

        Button skipButton = BisqIconButton.createIconButton("close");

        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.CENTER);
        hBox.setId("onboarding-top-panel");
        hBox.setMinHeight(TOP_PANE_HEIGHT);
        HBox.setMargin(skipButton, new Insets(0, 20, 0, -135));
        hBox.getChildren().addAll(Spacer.fillHBox(),
                direction,
                getSeparator(),
                market,
                getSeparator(),
                amount,
                getSeparator(),
                method,
                getSeparator(),
                complete,
                Spacer.fillHBox(), skipButton);

        return new Triple<>(hBox, skipButton, List.of(direction, market, amount, method, complete));
    }

    private Separator getSeparator() {
        Separator line = new Separator();
        line.setPrefWidth(30);
        return line;
    }

    private Label getTopPaneLabel(String text) {
        Label label = new Label(text.toUpperCase());
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().addAll("bisq-text-14");

        label.setOpacity(OPACITY);
        return label;
    }

    private void applyProgress(int progressIndex, boolean delay) {
        if (progressIndex < navigationProgressLabelList.size()) {
            navigationProgressLabelList.forEach(label -> label.setOpacity(OPACITY));
            Label label = navigationProgressLabelList.get(progressIndex);
            if (delay) {
                UIScheduler.run(() -> Transitions.fade(label, OPACITY, 1, Transitions.DEFAULT_DURATION / 2))
                        .after(Transitions.DEFAULT_DURATION / 2);
            } else {
                label.setOpacity(1);
            }
        }
    }
}
