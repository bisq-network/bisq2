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

package bisq.desktop.primary.overlay.onboarding;

import bisq.common.data.Triple;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;

@Slf4j
public class OnboardingView extends NavigationView<VBox, OnboardingModel, OnboardingController> {
    private static final double WIDTH = 920;
    private static final double HEIGHT = 550;

    private final Button skipButton;
    private final List<Label> navigationProgressLabelList;
    private final HBox topPaneBox;
    private Subscription navigationProgressIndexSubscription, navigationProgressVisibleSubscription;

    public OnboardingView(OnboardingModel model, OnboardingController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(WIDTH);
        root.setPrefHeight(HEIGHT);

        Triple<HBox, Button, List<Label>> topPane = getTopPane();
        topPaneBox = topPane.first();
        skipButton = topPane.second();
        navigationProgressLabelList = topPane.third();
        root.getChildren().add(topPaneBox);

        model.getView().addListener((observable, oldValue, newValue) -> {
            Region childRoot = newValue.getRoot();
            if (model.getNavigationProgressVisible().get()) {
                childRoot.setPrefHeight(root.getHeight() - 55);
            } else {
                childRoot.setPrefHeight(root.getHeight());
            }
            root.getChildren().add(childRoot);
            if (oldValue != null) {
                Transitions.transitHorizontal(childRoot, oldValue.getRoot());
            } else {
                Transitions.fadeIn(childRoot);
            }
        });
    }
    public void getBackGroundTransition() {
        
    }

    @Override
    protected void onViewAttached() {
        skipButton.textProperty().bind(model.getSkipButtonText());
        skipButton.visibleProperty().bind(model.getSkipButtonVisible());
        skipButton.setOnAction(e -> controller.onSkip());
        navigationProgressIndexSubscription = EasyBind.subscribe(model.getNavigationProgressIndex(), progressIndex -> {
            navigationProgressLabelList.forEach(label -> label.getStyleClass().remove("bisq-text-white"));
            Label label = navigationProgressLabelList.get((int) progressIndex);
            label.getStyleClass().add("bisq-text-white");
        });
        navigationProgressVisibleSubscription = EasyBind.subscribe(model.getNavigationProgressVisible(), visible -> {
            navigationProgressLabelList.forEach(label -> {
                label.setVisible(visible);
                label.setManaged(visible);
            });
            if (visible) {
                topPaneBox.setId("onboarding-top-panel");
            } else {
                topPaneBox.setId(null);
            }
        });
    }

    @Override
    protected void onViewDetached() {
        skipButton.textProperty().unbind();
        skipButton.visibleProperty().unbind();
        skipButton.setOnAction(null);
        navigationProgressIndexSubscription.unsubscribe();
        navigationProgressVisibleSubscription.unsubscribe();
    }

    public Triple<HBox, Button, List<Label>> getTopPane() {
        Label direction = getTopPaneLabel(Res.get("onboarding.navProgress.direction"));
        Label market = getTopPaneLabel(Res.get("onboarding.navProgress.market"));
        Label amount = getTopPaneLabel(Res.get("onboarding.navProgress.amount"));
        Label method = getTopPaneLabel(Res.get("onboarding.navProgress.method"));
        Label complete = getTopPaneLabel(Res.get("onboarding.navProgress.complete"));

        Button skipButton = new Button(Res.get("onboarding.navProgress.skip"));
        skipButton.getStyleClass().add("bisq-transparent-grey-button");

        HBox hBox = new HBox(50);
        hBox.setAlignment(Pos.CENTER);
        hBox.setId("onboarding-top-panel");
        hBox.setMinHeight(55);
        HBox.setMargin(skipButton, new Insets(0, 20, 0, -135));
        hBox.getChildren().addAll(Spacer.fillHBox(), direction, market, amount, method, complete, Spacer.fillHBox(), skipButton);

        return new Triple<>(hBox, skipButton, List.of(direction, market, amount, method, complete));
    }

    private Label getTopPaneLabel(String text) {
        Label label = new Label(text.toUpperCase());
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().addAll("bisq-text-4");
        return label;
    }
}
