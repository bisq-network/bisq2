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

import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.social.chat.ChatService;
import bisq.social.user.ChatUser;
import bisq.social.user.ChatUserIdentity;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class EditUserProfile {
    private final Controller controller;

    public EditUserProfile(ChatService chatService, ChatUserIdentity chatUserIdentity) {
        controller = new Controller(chatService, chatUserIdentity);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final ChatService chatService;
        private final Model model;
        @Getter
        private final View view;


        private Controller(ChatService chatService, ChatUserIdentity chatUserIdentity) {
            this.chatService = chatService;
            model = new Model(chatUserIdentity);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            ChatUser chatUser = model.chatUserIdentity.getChatUser();
            if (chatUser == null) {
                return;
            }

            model.id.set(Res.get("social.createUserProfile.id", chatUser.getId()));
            model.bio.set(chatUser.getBio());
            model.terms.set(chatUser.getTerms());
            model.reputationScore.set(chatUser.getBurnScoreAsString());
            model.profileAge.set(chatUser.getAccountAgeAsString());

            model.nym.set(chatUser.getNym());
            model.nickName.set(chatUser.getNickName());
            model.roboHashNode.set(RoboHash.getImage(chatUser.getProofOfWork().getPayload()));
        }

        @Override
        public void onDeactivate() {
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatUserIdentity chatUserIdentity;
        private final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        private final StringProperty nym = new SimpleStringProperty();
        private final StringProperty nickName = new SimpleStringProperty();
        private final StringProperty id = new SimpleStringProperty();
        private final StringProperty bio = new SimpleStringProperty();
        private final StringProperty terms = new SimpleStringProperty();
        private final StringProperty reputationScore = new SimpleStringProperty();
        private final StringProperty profileAge = new SimpleStringProperty();

        private Model(ChatUserIdentity chatUserIdentity) {
            this.chatUserIdentity = chatUserIdentity;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final Label nym, nickName, bio, reputationScore, profileAge, terms;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);
            root.setMaxWidth(200);
            root.setPadding(new Insets(0, 25, 0, 35));
            root.setAlignment(Pos.TOP_CENTER);

            nickName = new Label();
            nickName.getStyleClass().addAll("bisq-text-9", "font-semi-bold");
            nickName.setAlignment(Pos.CENTER);
            nickName.setMaxWidth(200);
            nickName.setMinWidth(200);
            VBox.setMargin(nickName, new Insets(-20, 0, 5, 0));

            roboIconImageView = new ImageView();
            roboIconImageView.setFitWidth(100);
            roboIconImageView.setFitHeight(100);

            nym = new Label();
            nym.getStyleClass().addAll("bisq-text-7");
            nym.setAlignment(Pos.CENTER);
            nym.setMaxWidth(200);
            nym.setMinWidth(200);
            VBox.setMargin(nym, new Insets(0, 0, 24, 0));


            VBox bioBox = getInfoBox(Res.get("social.chatUser.bio"), false);
            bio = (Label) bioBox.getChildren().get(1);

            VBox reputationScoreBox = getInfoBox(Res.get("social.chatUser.reputationScore"), false);
            reputationScore = (Label) reputationScoreBox.getChildren().get(1);

            VBox profileAgeBox = getInfoBox(Res.get("social.chatUser.profileAge"), false);
            profileAge = (Label) profileAgeBox.getChildren().get(1);

            Region separator = Layout.separator();
            VBox.setMargin(separator, new Insets(24, -45, 15, -55));

            VBox chatRulesBox = getInfoBox(Res.get("social.chat.chatRules.headline"), true);
            terms = (Label) chatRulesBox.getChildren().get(1);
            terms.setText(Res.get("social.chat.chatRules.content"));

            root.getChildren().addAll(nickName, roboIconImageView, nym,
                    bioBox, reputationScoreBox, profileAgeBox,
                    separator, chatRulesBox);
        }

        @Override
        protected void onViewAttached() {
            nym.textProperty().bind(model.nym);
            nickName.textProperty().bind(model.nickName);
            bio.textProperty().bind(model.bio);
            terms.textProperty().bind(model.terms);
            reputationScore.textProperty().bind(model.reputationScore);
            profileAge.textProperty().bind(model.profileAge);
            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
                if (roboIcon != null) {
                    roboIconImageView.setImage(roboIcon);
                }
            });
        }

        @Override
        protected void onViewDetached() {
            nym.textProperty().unbind();
            nickName.textProperty().unbind();
            bio.textProperty().unbind();
            terms.textProperty().unbind();
            reputationScore.textProperty().unbind();
            profileAge.textProperty().unbind();
            roboHashNodeSubscription.unsubscribe();
        }

        private VBox getInfoBox(String title, boolean smaller) {
            Label titleLabel = new Label(title.toUpperCase());
            titleLabel.getStyleClass().addAll("bisq-text-4", "bisq-text-grey-9", "font-semi-bold");
            Label contentLabel = new Label();

            contentLabel.getStyleClass().addAll(smaller ? "bisq-text-7" : "bisq-text-6", "wrap-text");
            VBox box = new VBox(2, titleLabel, contentLabel);
            VBox.setMargin(box, new Insets(2, 0, 0, 0));

            return box;
        }
    }
}