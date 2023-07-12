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

package bisq.desktop.main.content.components;

import bisq.chat.ChatService;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.Citation;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.robohash.RoboHash;
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
import javafx.scene.text.Text;
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
        return Optional.of(new Citation(userProfile.getId(), text));
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatService chatService;
        private final UserProfileService userProfileService;


        private Controller(ServiceProvider serviceProvider) {
            this.chatService = serviceProvider.getChatService();
            userProfileService = serviceProvider.getUserService().getUserProfileService();
            model = new Model();
            view = new View(model, this);
        }

        private void reply(ChatMessage chatMessage) {
            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId()).ifPresent(author -> {
                model.author = author;
                model.userName.set(author.getUserName());
                model.roboHashNode.set(RoboHash.getImage(author.getPubKeyHash()));
                model.citation.set(chatMessage.getText());
                model.visible.set(true);
            });
        }

        private void close() {
            model.visible.set(false);
            model.citation.set(null);
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final BooleanProperty visible = new SimpleBooleanProperty();
        private final StringProperty citation = new SimpleStringProperty("");
        private final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        private final StringProperty userName = new SimpleStringProperty();
        private UserProfile author;

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final Label userName;
        private final Button closeButton;
        private final Text citation;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setStyle("-fx-background-color: -bisq-dark-grey;");
            root.setPadding(new Insets(0, 15, 0, 20));

            Label headline = new Label(Res.get("chat.message.citation.headline"));
            headline.setStyle("-fx-text-fill: -bisq-grey-10");
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
            userName.setStyle("-fx-text-fill: -bisq-grey-10");

            roboIconImageView = new ImageView();
            roboIconImageView.setFitWidth(25);
            roboIconImageView.setFitHeight(25);
            HBox userBox = new HBox(15, roboIconImageView, userName);
            VBox.setMargin(userBox, new Insets(0, 0, 0, 0));
            citation = new Text();
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
            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
                if (roboIcon != null) {
                    roboIconImageView.setImage(roboIcon);
                }
            });

            closeButton.setOnAction(e -> controller.close());
        }

        @Override
        protected void onViewDetached() {
            userName.textProperty().unbind();
            citation.textProperty().unbind();
            roboHashNodeSubscription.unsubscribe();
            closeButton.setOnAction(null);
        }
    }
}