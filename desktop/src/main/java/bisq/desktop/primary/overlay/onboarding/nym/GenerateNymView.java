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

package bisq.desktop.primary.overlay.onboarding.nym;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateNymView extends View<VBox, GenerateNymModel, GenerateNymController> {
    private final Button regenerateButton;
    private final Button createProfileButton;
    private final Label nymId;
    private final ImageView roboIconView;
    private final ProgressIndicator powProgressIndicator;
    private final ProgressIndicator createProfileIndicator;

    public GenerateNymView(GenerateNymModel model, GenerateNymController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setSpacing(8);
        root.setPadding(new Insets(10, 0, 30, 0));

        Label headLineLabel = new Label(Res.get("generateNym.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("generateNym.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setMaxWidth(300);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        roboIconView = new ImageView();
        roboIconView.setCursor(Cursor.HAND);
        int size = 128;
        roboIconView.setFitWidth(size);
        roboIconView.setFitHeight(size);

        int indicatorSize = size / 2;
        powProgressIndicator = new ProgressIndicator();
        powProgressIndicator.setMinSize(indicatorSize, indicatorSize);
        powProgressIndicator.setMaxSize(indicatorSize, indicatorSize);
        powProgressIndicator.setOpacity(0.5);

        StackPane stackPane = new StackPane();
        stackPane.setMinSize(size, size);
        stackPane.setMaxSize(size, size);
        stackPane.getChildren().addAll(powProgressIndicator, roboIconView);

        VBox profileIdBox = getValueBox(Res.get("generateNym.nymId"));
        nymId = (Label) profileIdBox.getChildren().get(1);

        regenerateButton = new Button(Res.get("generateNym.regenerate"));
        regenerateButton.getStyleClass().setAll("bisq-transparent-button", "bisq-text-3", "text-underline");

        createProfileButton = new Button(Res.get("generateNym.createProfile"));
        createProfileButton.setGraphicTextGap(8.0);
        createProfileButton.setContentDisplay(ContentDisplay.RIGHT);
        createProfileButton.setDefaultButton(true);

        createProfileIndicator = new ProgressIndicator();
        createProfileIndicator.setProgress(0);
        createProfileIndicator.setMaxWidth(24);
        createProfileIndicator.setMaxHeight(24);
        createProfileIndicator.setManaged(false);
        createProfileIndicator.setVisible(false);
        HBox hBox = new HBox(10, createProfileButton, createProfileIndicator);
        hBox.setAlignment(Pos.CENTER);

        VBox.setMargin(headLineLabel, new Insets(40, 0, 0, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 8, 0));
        VBox.setMargin(profileIdBox, new Insets(0, 0, 16, 0));
        VBox.setMargin(regenerateButton, new Insets(0, 0, 16, 0));
        root.getChildren().addAll(
                headLineLabel,
                subtitleLabel,
                stackPane,
                profileIdBox,
                regenerateButton,
                hBox
        );
    }

    @Override
    protected void onViewAttached() {
        regenerateButton.mouseTransparentProperty().bind(model.regenerateButtonMouseTransparent);
        roboIconView.imageProperty().bind(model.roboHashImage);

        roboIconView.managedProperty().bind(model.roboHashIconVisible);
        roboIconView.visibleProperty().bind(model.roboHashIconVisible);
        powProgressIndicator.managedProperty().bind(model.roboHashIconVisible.not());
        powProgressIndicator.visibleProperty().bind(model.roboHashIconVisible.not());
        createProfileIndicator.managedProperty().bind(model.createProfileProgress.lessThan(0));
        createProfileIndicator.visibleProperty().bind(model.createProfileProgress.lessThan(0));

        powProgressIndicator.progressProperty().bind(model.powProgress);
        createProfileIndicator.progressProperty().bind(model.createProfileProgress);

        nymId.textProperty().bind(model.nymId);
        nymId.disableProperty().bind(model.roboHashIconVisible.not());
        createProfileButton.disableProperty().bind(model.createProfileProgress.lessThan(0));

        regenerateButton.setOnAction(e -> controller.onCreateTempIdentity());
        createProfileButton.setOnAction(e -> controller.onCreateNymProfile());
    }

    @Override
    protected void onViewDetached() {
        regenerateButton.mouseTransparentProperty().unbind();
        roboIconView.imageProperty().unbind();

        roboIconView.managedProperty().unbind();
        roboIconView.visibleProperty().unbind();
        powProgressIndicator.managedProperty().unbind();
        powProgressIndicator.visibleProperty().unbind();
        createProfileIndicator.managedProperty().unbind();
        createProfileIndicator.visibleProperty().unbind();

        powProgressIndicator.progressProperty().unbind();
        createProfileIndicator.progressProperty().unbind();

        nymId.textProperty().unbind();
        nymId.disableProperty().unbind();
        createProfileButton.disableProperty().unbind();

        regenerateButton.setOnAction(null);
        createProfileButton.setOnAction(null);
    }

    private VBox getValueBox(String title) {
        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.getStyleClass().add("bisq-text-4");

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("bisq-text-1");

        VBox box = new VBox(titleLabel, valueLabel);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
