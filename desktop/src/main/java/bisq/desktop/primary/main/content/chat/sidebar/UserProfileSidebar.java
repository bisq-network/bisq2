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

package bisq.desktop.primary.main.content.chat.sidebar;

import bisq.chat.ChatService;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class UserProfileSidebar implements Comparable<UserProfileSidebar> {
    private final Controller controller;

    public UserProfileSidebar(UserProfileService userProfileService,
                              ChatService chatService,
                              ReputationService reputationService,
                              UserProfile userProfile,
                              Runnable closeHandler) {
        controller = new Controller(userProfileService, chatService, reputationService, userProfile, closeHandler);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setOnMentionUserHandler(Consumer<UserProfile> handler) {
        controller.model.mentionUserHandler = Optional.ofNullable(handler);
    }

    public void setOnSendPrivateMessageHandler(Consumer<UserProfile> handler) {
        controller.model.sendPrivateMessageHandler = Optional.ofNullable(handler);
    }

    public void setIgnoreUserStateHandler(Runnable handler) {
        controller.model.ignoreUserStateHandler = Optional.ofNullable(handler);
    }

    @Override
    public int compareTo(UserProfileSidebar o) {
        return controller.model.userProfile.getUserName().compareTo(o.controller.model.userProfile.getUserName());
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;
        private final ReputationService reputationService;
        private final Runnable closeHandler;


        private Controller(UserProfileService userProfileService,
                           ChatService chatService,
                           ReputationService reputationService,
                           UserProfile userProfile,
                           Runnable closeHandler) {
            this.userProfileService = userProfileService;
            this.reputationService = reputationService;
            this.closeHandler = closeHandler;
            model = new Model(chatService, userProfile);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            UserProfile userProfile = model.userProfile;
            if (userProfile == null) {
                return;
            }

            model.ignoreButtonText.set(Res.get("social.ignore"));
            model.id.set(Res.get("social.createUserProfile.id", userProfile.getId()));
            model.statement.set(userProfile.getStatement());
            model.terms.set(userProfile.getTerms());

            model.nym.set(userProfile.getNym());
            model.nickName.set(userProfile.getNickName());
            model.roboHashNode.set(RoboHash.getImage(userProfile.getPubKeyHash()));

            // todo add tooltip
            model.reputationScore.set(String.valueOf(reputationService.getReputationScore(userProfile).getTotalScore()));

            model.profileAge.set(reputationService.getProfileAgeService().getProfileAge(userProfile)
                    .map(TimeFormatter::formatAgeInDays)
                    .orElse(Res.get("na")));
        }

        @Override
        public void onDeactivate() {
        }

        void onSendPrivateMessage() {
            model.sendPrivateMessageHandler.ifPresent(handler -> handler.accept(model.userProfile));
        }

        void onMentionUser() {
            model.mentionUserHandler.ifPresent(handler -> handler.accept(model.userProfile));
        }

        void onToggleIgnoreUser() {
            model.ignoreUserSelected.set(!model.ignoreUserSelected.get());
            if (model.ignoreUserSelected.get()) {
                userProfileService.ignoreUserProfile(model.userProfile);
                model.ignoreButtonText.set(Res.get("social.undoIgnore"));
            } else {
                userProfileService.undoIgnoreUserProfile(model.userProfile);
                model.ignoreButtonText.set(Res.get("social.ignore"));
            }
            model.ignoreUserStateHandler.ifPresent(Runnable::run);
        }

        void onReportUser() {
            // todo open popup for editing reason
            model.chatService.reportUserProfile(model.userProfile, "");
        }

        void onClose() {
            closeHandler.run();
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatService chatService;
        private final UserProfile userProfile;
        private Optional<Consumer<UserProfile>> mentionUserHandler = Optional.empty();
        private Optional<Consumer<UserProfile>> sendPrivateMessageHandler = Optional.empty();
        private Optional<Runnable> ignoreUserStateHandler = Optional.empty();
        private final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        private final StringProperty nym = new SimpleStringProperty();
        private final StringProperty nickName = new SimpleStringProperty();
        private final StringProperty id = new SimpleStringProperty();
        private final StringProperty statement = new SimpleStringProperty();
        private final StringProperty terms = new SimpleStringProperty();
        private final StringProperty reputationScore = new SimpleStringProperty();
        private final StringProperty profileAge = new SimpleStringProperty();
        private final BooleanProperty ignoreUserSelected = new SimpleBooleanProperty();
        private final StringProperty ignoreButtonText = new SimpleStringProperty();

        private Model(ChatService chatService, UserProfile userProfile) {
            this.chatService = chatService;
            this.userProfile = userProfile;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final Label nym;
        private final Label nickName;
        private final Label statement;
        private final Label reputationScore;
        private final Label profileAge;
        private final Hyperlink mention, ignore, report;
        private final Label terms;
        private final Button privateMsgButton;
        private final VBox statementBox;
        private final VBox termsBox;
        private Subscription roboHashNodeSubscription;
        private final Button closeButton;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);
            root.setMinWidth(260);
            root.setPadding(new Insets(0, 20, 20, 20));

            //root.setPadding(new Insets(0, 25, 0, 35));
            root.setAlignment(Pos.TOP_CENTER);

            Label headline = new Label(Res.get("chat.sidebar.userProfile.headline"));
            headline.setId("chat-sidebar-headline");

            closeButton = BisqIconButton.createIconButton("close");
            HBox.setMargin(headline, new Insets(18, 0, 0, 0));
            HBox.setMargin(closeButton, new Insets(10, 10, 0, 0));
            HBox topHBox = new HBox(headline, Spacer.fillHBox(), closeButton);

            nickName = new Label();
            nickName.getStyleClass().addAll("bisq-text-9", "font-semi-bold");
            nickName.setAlignment(Pos.CENTER);
            VBox.setMargin(nickName, new Insets(-20, 0, 5, 0));

            roboIconImageView = new ImageView();
            roboIconImageView.setFitWidth(100);
            roboIconImageView.setFitHeight(100);

            nym = new Label();
            nym.getStyleClass().addAll("bisq-text-7");
            nym.setAlignment(Pos.CENTER);
            VBox.setMargin(nym, new Insets(0, 0, 25, 0));

            privateMsgButton = new Button(Res.get("social.sendPrivateMessage"));
            VBox.setMargin(privateMsgButton, new Insets(0, 0, 13, 0));

            statementBox = getInfoBox(Res.get("social.chatUser.statement"), false);
            statement = (Label) statementBox.getChildren().get(1);

            VBox reputationScoreBox = getInfoBox(Res.get("social.chatUser.reputationScore"), false);
            reputationScore = (Label) reputationScoreBox.getChildren().get(1);

            VBox profileAgeBox = getInfoBox(Res.get("social.chatUser.profileAge"), false);
            profileAge = (Label) profileAgeBox.getChildren().get(1);

            Label optionsLabel = new Label(Res.get("social.chatUser.options").toUpperCase());
            optionsLabel.getStyleClass().addAll("bisq-text-7", "bisq-text-grey-9", "font-semi-bold");

            mention = new Hyperlink(Res.get("social.mention"));
            ignore = new Hyperlink();
            report = new Hyperlink(Res.get("social.reports"));
            VBox optionsBox = new VBox(5, optionsLabel, mention, ignore, report);
            optionsBox.setAlignment(Pos.CENTER_LEFT);
            VBox.setMargin(optionsBox, new Insets(8, 0, 0, 0));

            Region separator = Layout.separator();
            VBox.setMargin(separator, new Insets(25, -20, 15, -20));

            termsBox = getInfoBox(Res.get("social.chatUser.terms"), true);
            terms = (Label) termsBox.getChildren().get(1);
            VBox.setMargin(topHBox, new Insets(0, -20, 30, 0));
            root.getChildren().addAll(topHBox, nickName, roboIconImageView, nym, privateMsgButton,
                    statementBox, reputationScoreBox, profileAgeBox,
                    optionsBox, separator, termsBox);
        }

        @Override
        protected void onViewAttached() {
            nym.textProperty().bind(model.nym);
            nickName.textProperty().bind(model.nickName);
            statement.textProperty().bind(model.statement);
            statementBox.visibleProperty().bind(model.statement.isEmpty().not());
            statementBox.managedProperty().bind(model.statement.isEmpty().not());
            terms.textProperty().bind(model.terms);
            termsBox.visibleProperty().bind(model.terms.isEmpty().not());
            termsBox.managedProperty().bind(model.terms.isEmpty().not());
            reputationScore.textProperty().bind(model.reputationScore);
            profileAge.textProperty().bind(model.profileAge);
            ignore.textProperty().bind(model.ignoreButtonText);
            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
                if (roboIcon != null) {
                    roboIconImageView.setImage(roboIcon);
                }
            });

            privateMsgButton.setOnAction(e -> controller.onSendPrivateMessage());
            mention.setOnAction(e -> controller.onMentionUser());
            ignore.setOnAction(e -> controller.onToggleIgnoreUser());
            report.setOnAction(e -> controller.onReportUser());
            closeButton.setOnAction(e -> controller.onClose());
        }

        @Override
        protected void onViewDetached() {
            nym.textProperty().unbind();
            nickName.textProperty().unbind();
            statement.textProperty().unbind();
            statementBox.visibleProperty().unbind();
            statementBox.managedProperty().unbind();
            terms.textProperty().unbind();
            terms.visibleProperty().unbind();
            terms.managedProperty().unbind();
            reputationScore.textProperty().unbind();
            profileAge.textProperty().unbind();
            ignore.textProperty().unbind();

            roboHashNodeSubscription.unsubscribe();

            privateMsgButton.setOnAction(null);
            mention.setOnAction(null);
            ignore.setOnAction(null);
            report.setOnAction(null);
            closeButton.setOnAction(null);
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