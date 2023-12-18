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

package bisq.desktop.main.content.chat.sidebar;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatService;
import bisq.common.data.Triple;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Layout;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.main.content.components.ReportToModeratorWindow;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.i18n.Res;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class UserProfileSidebar implements Comparable<UserProfileSidebar> {
    private final Controller controller;

    public UserProfileSidebar(ServiceProvider serviceProvider,
                              UserProfile userProfile,
                              ChatChannel<? extends ChatMessage> selectedChannel,
                              Runnable closeHandler) {
        controller = new Controller(serviceProvider, userProfile, selectedChannel, closeHandler);
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
        private final UserIdentityService userIdentityService;
        private final ReputationService reputationService;
        private final Runnable closeHandler;
        private final BannedUserService bannedUserService;


        private Controller(ServiceProvider serviceProvider,
                           UserProfile userProfile,
                           ChatChannel<? extends ChatMessage> selectedChannel,
                           Runnable closeHandler) {
            userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            userProfileService = serviceProvider.getUserService().getUserProfileService();
            reputationService = serviceProvider.getUserService().getReputationService();
            bannedUserService = serviceProvider.getUserService().getBannedUserService();
            this.closeHandler = closeHandler;
            model = new Model(serviceProvider.getChatService(), userProfile, selectedChannel);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            UserProfile userProfile = model.userProfile;
            if (userProfile == null) {
                return;
            }

            String nickName = userProfile.getNickName();
            model.nickName.set(isUserProfileBanned() ? Res.get("user.userProfile.userName.banned", nickName) : nickName);
            model.nym.set(Res.get("chat.sideBar.userProfile.nym", userProfile.getNym()));
            model.userProfileIdString.set(Res.get("chat.sideBar.userProfile.id", userProfile.getId()));
            model.roboHashNode.set(RoboHash.getImage(userProfile.getPubKeyHash()));

            model.addressByTransport.set(userProfile.getAddressByTransportDisplayString(26));
            model.addressByTransportTooltip.set(userProfile.getAddressByTransportDisplayString());

            model.ignoreButtonText.set(Res.get("chat.sideBar.userProfile.ignore"));
            model.statement.set(userProfile.getStatement());
            model.terms.set(userProfile.getTerms());

            // todo add tooltip
            model.reputationScore.set(reputationService.getReputationScore(userProfile));

            model.profileAge.set(reputationService.getProfileAgeService().getProfileAge(userProfile)
                    .map(TimeFormatter::formatAgeInDays)
                    .orElse(Res.get("data.na")));

            // If we selected our own user we don't show certain features
            model.isPeer.set(!userIdentityService.isUserIdentityPresent(userProfile.getId()));
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
                model.ignoreButtonText.set(Res.get("chat.sideBar.userProfile.undoIgnore"));
            } else {
                userProfileService.undoIgnoreUserProfile(model.userProfile);
                model.ignoreButtonText.set(Res.get("chat.sideBar.userProfile.ignore"));
            }
            model.ignoreUserStateHandler.ifPresent(Runnable::run);
        }

        void onReportUser() {
            ChatChannelDomain chatChannelDomain = model.getSelectedChannel().getChatChannelDomain();
            Navigation.navigateTo(NavigationTarget.REPORT_TO_MODERATOR, new ReportToModeratorWindow.InitData(model.userProfile, chatChannelDomain));
        }

        void onClose() {
            closeHandler.run();
        }

        boolean isUserProfileBanned() {
            return bannedUserService.isUserProfileBanned(model.getUserProfile());
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatService chatService;
        private final UserProfile userProfile;
        private final ChatChannel<? extends ChatMessage> selectedChannel;
        private Optional<Consumer<UserProfile>> mentionUserHandler = Optional.empty();
        private Optional<Consumer<UserProfile>> sendPrivateMessageHandler = Optional.empty();
        private Optional<Runnable> ignoreUserStateHandler = Optional.empty();
        private final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        private final StringProperty nickName = new SimpleStringProperty();
        private final StringProperty nym = new SimpleStringProperty();
        private final StringProperty addressByTransport = new SimpleStringProperty();
        private final StringProperty addressByTransportTooltip = new SimpleStringProperty();
        private final StringProperty userProfileIdString = new SimpleStringProperty();
        private final StringProperty statement = new SimpleStringProperty();
        private final StringProperty terms = new SimpleStringProperty();
        private final ObjectProperty<ReputationScore> reputationScore = new SimpleObjectProperty<>();
        private final StringProperty profileAge = new SimpleStringProperty();
        private final BooleanProperty ignoreUserSelected = new SimpleBooleanProperty();
        private final StringProperty ignoreButtonText = new SimpleStringProperty();
        private final BooleanProperty isPeer = new SimpleBooleanProperty();

        private Model(ChatService chatService, UserProfile userProfile, ChatChannel<? extends ChatMessage> selectedChannel) {
            this.chatService = chatService;
            this.userProfile = userProfile;
            this.selectedChannel = selectedChannel;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final Label nickName, botId, userProfileId, addressByTransport, statement, totalReputationScore, profileAge;
        private final Hyperlink privateMsg, mention, ignore, report;
        private final VBox statementBox, termsBox, optionsVBox;
        private final ReputationScoreDisplay reputationScoreDisplay;
        private final TextArea terms;
        private final Button closeButton;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller) {
            super(new VBox(15), model, controller);

            root.setPadding(new Insets(0, 20, 20, 20));
            root.setAlignment(Pos.TOP_CENTER);
            root.setMinWidth(260);
            root.setMaxWidth(260);

            Label headline = new Label(Res.get("chat.sideBar.userProfile.headline"));
            headline.setId("chat-sidebar-headline");

            closeButton = BisqIconButton.createIconButton("close");
            HBox.setMargin(headline, new Insets(15.5, 0, 0, 0));
            HBox.setMargin(closeButton, new Insets(10, 10, 0, 0));
            HBox header = new HBox(headline, Spacer.fillHBox(), closeButton);

            nickName = new Label();
            nickName.getStyleClass().add("chat-side-bar-user-profile-nickname");
            if (controller.isUserProfileBanned()) {
                nickName.getStyleClass().add("error");
            }

            roboIconImageView = new ImageView();
            roboIconImageView.setFitWidth(100);
            roboIconImageView.setFitHeight(100);

            botId = new Label();
            botId.getStyleClass().add("chat-side-bar-user-profile-details");
            botId.setTooltip(new BisqTooltip(model.nym.get()));
            botId.setAlignment(Pos.CENTER_LEFT);
            botId.setTextAlignment(TextAlignment.LEFT);

            userProfileId = new Label();
            userProfileId.getStyleClass().add("chat-side-bar-user-profile-details");
            userProfileId.setTooltip(new BisqTooltip(model.userProfileIdString.get()));

            addressByTransport = new Label();
            addressByTransport.setWrapText(true);
            addressByTransport.getStyleClass().add("chat-side-bar-user-profile-details");
            addressByTransport.setTooltip(new BisqTooltip(model.addressByTransportTooltip.get()));

            Label reputationHeadline = new Label(Res.get("chat.sideBar.userProfile.reputation").toUpperCase());
            reputationHeadline.getStyleClass().add("chat-side-bar-user-profile-small-headline");
            reputationScoreDisplay = new ReputationScoreDisplay();
            reputationScoreDisplay.setAlignment(Pos.CENTER_LEFT);
            VBox.setMargin(reputationScoreDisplay, new Insets(0, 0, 5, 0));
            VBox reputationBox = new VBox(5, reputationHeadline, reputationScoreDisplay);

            Triple<Label, Label, VBox> totalReputationScoreTriple = getInfoBox(Res.get("chat.sideBar.userProfile.totalReputationScore"));
            VBox totalReputationScoreBox = totalReputationScoreTriple.getThird();
            totalReputationScore = totalReputationScoreTriple.getSecond();

            Triple<Label, Label, VBox> profileAgeTriple = getInfoBox(Res.get("chat.sideBar.userProfile.profileAge"));
            VBox profileAgeBox = profileAgeTriple.getThird();
            profileAge = profileAgeTriple.getSecond();

            privateMsg = new Hyperlink(Res.get("chat.sideBar.userProfile.sendPrivateMessage"));
            mention = new Hyperlink(Res.get("chat.sideBar.userProfile.mention"));
            ignore = new Hyperlink();
            report = new Hyperlink(Res.get("chat.sideBar.userProfile.report"));
            privateMsg.getStyleClass().add("chat-side-bar-user-profile-small-hyperlink");
            mention.getStyleClass().add("chat-side-bar-user-profile-small-hyperlink");
            ignore.getStyleClass().add("chat-side-bar-user-profile-small-hyperlink");
            report.getStyleClass().add("chat-side-bar-user-profile-small-hyperlink");

            Triple<Label, Label, VBox> statementTriple = getInfoBox(Res.get("chat.sideBar.userProfile.statement"));
            statementBox = statementTriple.getThird();
            statement = statementTriple.getSecond();

            Label termsHeadline = new Label(Res.get("chat.sideBar.userProfile.terms").toUpperCase());
            termsHeadline.getStyleClass().add("chat-side-bar-user-profile-small-headline");
            terms = new TextArea();
            terms.setMaxWidth(root.getMaxWidth() - 40);
            terms.setMaxHeight(100);
            terms.setWrapText(true);
            terms.getStyleClass().add("chat-side-bar-user-profile");
            termsBox = new VBox(7.5, termsHeadline, terms);

            Region separator = Layout.hLine();
            VBox.setMargin(separator, new Insets(20, -20, 10, -20));
            optionsVBox = new VBox(5, separator, privateMsg, mention, ignore, report);
            optionsVBox.setAlignment(Pos.CENTER_LEFT);

            VBox.setMargin(header, new Insets(0, -20, 0, 0));
            VBox.setMargin(nickName, new Insets(10, 0, 0, 0));
            VBox.setMargin(userProfileId, new Insets(-10, 0, 0, 0));
            VBox.setMargin(addressByTransport, new Insets(-10, 0, 0, 0));
            VBox.setMargin(reputationBox, new Insets(4, 0, 0, 0));
            root.getChildren().addAll(header,
                    nickName, roboIconImageView, botId, userProfileId, addressByTransport,
                    reputationBox, totalReputationScoreBox, profileAgeBox, statementBox, termsBox,
                    optionsVBox);
        }

        @Override
        protected void onViewAttached() {
            nickName.textProperty().bind(model.nickName);
            botId.textProperty().bind(model.nym);
            botId.getTooltip().textProperty().bind(model.nym);
            userProfileId.textProperty().bind(model.userProfileIdString);
            userProfileId.getTooltip().textProperty().bind(model.userProfileIdString);
            addressByTransport.textProperty().bind(model.addressByTransport);
            addressByTransport.getTooltip().textProperty().bind(model.addressByTransportTooltip);
            statement.textProperty().bind(model.statement);
            statementBox.visibleProperty().bind(model.statement.isEmpty().not());
            statementBox.managedProperty().bind(model.statement.isEmpty().not());
            terms.textProperty().bind(model.terms);
            termsBox.visibleProperty().bind(model.terms.isEmpty().not());
            termsBox.managedProperty().bind(model.terms.isEmpty().not());
            profileAge.textProperty().bind(model.profileAge);
            ignore.textProperty().bind(model.ignoreButtonText);
            optionsVBox.visibleProperty().bind(model.isPeer);
            optionsVBox.managedProperty().bind(model.isPeer);
            privateMsg.visibleProperty().bind(model.isPeer);
            privateMsg.managedProperty().bind(model.isPeer);

            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
                if (roboIcon != null) {
                    roboIconImageView.setImage(roboIcon);
                }
            });

            roboHashNodeSubscription = EasyBind.subscribe(model.reputationScore, reputationScore -> {
                if (reputationScore != null) {
                    reputationScoreDisplay.setReputationScore(reputationScore);
                    totalReputationScore.setText(String.valueOf(reputationScore.getTotalScore()));
                }
            });

            privateMsg.setOnAction(e -> controller.onSendPrivateMessage());
            mention.setOnAction(e -> controller.onMentionUser());
            ignore.setOnAction(e -> controller.onToggleIgnoreUser());
            report.setOnAction(e -> controller.onReportUser());
            closeButton.setOnAction(e -> controller.onClose());
        }

        @Override
        protected void onViewDetached() {
            nickName.textProperty().unbind();
            botId.textProperty().unbind();
            botId.getTooltip().textProperty().unbind();
            userProfileId.textProperty().unbind();
            userProfileId.getTooltip().textProperty().unbind();
            addressByTransport.textProperty().unbind();
            addressByTransport.getTooltip().textProperty().unbind();
            statement.textProperty().unbind();
            statementBox.visibleProperty().unbind();
            statementBox.managedProperty().unbind();
            terms.textProperty().unbind();
            terms.visibleProperty().unbind();
            terms.managedProperty().unbind();
            profileAge.textProperty().unbind();
            ignore.textProperty().unbind();
            optionsVBox.visibleProperty().unbind();
            optionsVBox.managedProperty().unbind();
            privateMsg.visibleProperty().unbind();
            privateMsg.managedProperty().unbind();

            roboHashNodeSubscription.unsubscribe();

            privateMsg.setOnAction(null);
            mention.setOnAction(null);
            ignore.setOnAction(null);
            report.setOnAction(null);
            closeButton.setOnAction(null);
        }

        private Triple<Label, Label, VBox> getInfoBox(String title) {
            Label headline = new Label(title.toUpperCase());
            headline.getStyleClass().add("chat-side-bar-user-profile-small-headline");
            Label value = new Label();
            value.setWrapText(true);
            value.getStyleClass().add("chat-side-bar-user-profile-small-value");
            VBox vBox = new VBox(2.5, headline, value);
            return new Triple<>(headline, value, vBox);
        }
    }
}