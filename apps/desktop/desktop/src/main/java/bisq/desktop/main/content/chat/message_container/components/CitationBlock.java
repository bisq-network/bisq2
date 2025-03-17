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

package bisq.desktop.main.content.chat.message_container.components;

import bisq.chat.ChatMessage;
import bisq.chat.Citation;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class CitationBlock {
    private final Controller controller;

    public CitationBlock(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void reply(ChatMessage chatMessage) {
        controller.reply(chatMessage);
    }

    public void close() {
        controller.close();
    }

    public Optional<Citation> getCitation() {
        String text = controller.model.citation.get();
        UserProfile userProfile = controller.model.author;
        if (text == null || text.isEmpty() || userProfile == null) {
            return Optional.empty();
        }

        String truncated = StringUtils.truncate(text, Citation.MAX_TEXT_LENGTH);
        String chatMessageId = controller.model.chatMessageId;
        return Optional.of(new Citation(userProfile.getId(), truncated, Optional.of(chatMessageId)));
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;

        private Controller(ServiceProvider serviceProvider) {
            userProfileService = serviceProvider.getUserService().getUserProfileService();
            model = new Model();
            view = new View(model, this);
        }

        private void reply(ChatMessage chatMessage) {
            model.chatMessageId = chatMessage.getId();
            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId()).ifPresent(author -> {
                model.author = author;
                model.userName.set(author.getUserName());
                model.catHashImage.set(CatHash.getImage(author, Model.CAT_HASH_IMAGE_SIZE));
                model.citation.set(chatMessage.getTextOrNA());
                model.visible.set(true);
            });
        }

        private void close() {
            model.visible.set(false);
            model.citation.set(null);
            model.catHashImage.set(null);
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
            model.catHashImage.set(null);
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private static final double CAT_HASH_IMAGE_SIZE = 25;

        private final BooleanProperty visible = new SimpleBooleanProperty();
        private final StringProperty citation = new SimpleStringProperty("");
        private final ObjectProperty<Image> catHashImage = new SimpleObjectProperty<>();
        private final StringProperty userName = new SimpleStringProperty();
        private String chatMessageId;
        private UserProfile author;

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView catHashImageView;
        private final Label userName;
        private final Button closeButton;
        private final Label citation;
        private Subscription catHashImagePin;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setStyle("-fx-background-color: -bisq-dark-grey-10;");
            root.setPadding(new Insets(0, 15, 0, 20));

            Label headline = new Label(Res.get("chat.message.citation.headline"));
            headline.setStyle("-fx-text-fill: -bisq-mid-grey-30");
            headline.getStyleClass().addAll("font-light", "font-size-11");

            closeButton = BisqIconButton.createDeleteIconButton();
            closeButton.setOpacity(0.5);
            HBox.setMargin(headline, new Insets(0, 0, 5, 0));
            HBox.setMargin(closeButton, new Insets(0, -22, 0, 0));
            HBox topBox = new HBox(15, headline, Spacer.fillHBox(), closeButton);
            topBox.setAlignment(Pos.CENTER);
            topBox.setPadding(new Insets(5, 30, -5, 0));

            userName = new Label();
            userName.setPadding(new Insets(3, 0, 0, -3));
            userName.getStyleClass().add("font-medium");
            userName.setStyle("-fx-text-fill: -bisq-mid-grey-30");

            catHashImageView = new ImageView();
            catHashImageView.setFitWidth(Model.CAT_HASH_IMAGE_SIZE);
            catHashImageView.setFitHeight(catHashImageView.getFitWidth());
            HBox userBox = new HBox(15, catHashImageView, userName);
            VBox.setMargin(userBox, new Insets(0, 0, 0, 0));
            citation = new Label();
            citation.setWrapText(true);
            citation.setStyle("-fx-fill: -fx-mid-text-color");
            VBox.setMargin(citation, new Insets(0, 0, 15, 0));
            root.getChildren().addAll(topBox, userBox, citation);
        }

        @Override
        protected void onViewAttached() {
            root.visibleProperty().bind(model.visible);
            root.managedProperty().bind(model.visible);
            userName.textProperty().bind(model.userName);
            citation.textProperty().bind(model.citation);
            catHashImagePin = EasyBind.subscribe(model.catHashImage, catHashImage -> {
                if (catHashImage != null) {
                    catHashImageView.setImage(catHashImage);
                }
            });

            closeButton.setOnAction(e -> controller.close());
        }

        @Override
        protected void onViewDetached() {
            userName.textProperty().unbind();
            citation.textProperty().unbind();
            catHashImagePin.unsubscribe();
            closeButton.setOnAction(null);
            catHashImageView.setImage(null);
        }
    }
}