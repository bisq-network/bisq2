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
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqMenuItem;
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
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
        private UIScheduler livenessUpateScheduler;

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
            model.nym.set(userProfile.getNym());
            model.userProfileIdString.set(userProfile.getId());
            model.setCatHashImage(CatHash.getImage(userProfile));

            model.addressByTransport.set(userProfile.getAddressByTransportDisplayString(26));

            model.statement.set(userProfile.getStatement());
            model.terms.set(userProfile.getTerms());

            model.reputationScore.set(reputationService.getReputationScore(userProfile));

            model.profileAge.set(reputationService.getProfileAgeService().getProfileAge(userProfile)
                    .map(TimeFormatter::formatAgeInDays)
                    .orElse(Res.get("data.na")));

            if (livenessUpateScheduler != null) {
                livenessUpateScheduler.stop();
                livenessUpateScheduler = null;
            }
            livenessUpateScheduler = UIScheduler.run(() -> {
                        long publishDate = userProfile.getPublishDate();
                        if (publishDate == 0) {
                            model.getLivenessState().set(Res.get("data.na"));
                        } else {
                            long age = Math.max(0, System.currentTimeMillis() - publishDate);
                            String formattedAge = TimeFormatter.formatAge(age);
                            model.getLivenessState().set(Res.get("user.userProfile.livenessState.ageDisplay", formattedAge));
                        }
                    })
                    .periodically(0, 1, TimeUnit.SECONDS);

            String version = userProfile.getApplicationVersion();
            if (version.isEmpty()) {
                version = Res.get("data.na");
            }
            model.version.set(version);

            // If we selected our own user we don't show certain features
            model.isPeer.set(!userIdentityService.isUserIdentityPresent(userProfile.getId()));
        }

        @Override
        public void onDeactivate() {
            if (livenessUpateScheduler != null) {
                livenessUpateScheduler.stop();
                livenessUpateScheduler = null;
            }
            model.setCatHashImage(null);
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
            } else {
                userProfileService.undoIgnoreUserProfile(model.userProfile);
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
        @Setter
        private Image catHashImage;
        private final StringProperty nickName = new SimpleStringProperty();
        private final StringProperty nym = new SimpleStringProperty();
        private final StringProperty addressByTransport = new SimpleStringProperty();
        private final StringProperty userProfileIdString = new SimpleStringProperty();
        private final StringProperty statement = new SimpleStringProperty();
        private final StringProperty terms = new SimpleStringProperty();
        private final ObjectProperty<ReputationScore> reputationScore = new SimpleObjectProperty<>();
        private final StringProperty profileAge = new SimpleStringProperty();
        private final StringProperty livenessState = new SimpleStringProperty();
        private final StringProperty version = new SimpleStringProperty();
        private final BooleanProperty ignoreUserSelected = new SimpleBooleanProperty();
        private final BooleanProperty isPeer = new SimpleBooleanProperty();

        private Model(ChatService chatService,
                      UserProfile userProfile,
                      ChatChannel<? extends ChatMessage> selectedChannel) {
            this.chatService = chatService;
            this.userProfile = userProfile;
            this.selectedChannel = selectedChannel;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView catIconImageView;
        private final Label nickName, botId, userId, addressByTransport, statement, totalReputationScore,
                profileAge, livenessState, terms, version;
        private final BisqMenuItem privateMsg, mention, ignore, undoIgnore, report;
        private final VBox botIdBox, userIdBox, addressByTransportBox, statementBox, termsBox, optionsVBox;
        private final ReputationScoreDisplay reputationScoreDisplay;
        private final BisqIconButton botIdCopyButton, userIdCopyButton, addressByTransportCopyButton;
        private final Button closeButton;
        private Subscription reputationScorePin;

        private View(Model model, Controller controller) {
            super(new VBox(15), model, controller);

            double width = 260;
            root.setPadding(new Insets(0, 20, 0, 20));
            root.setAlignment(Pos.TOP_CENTER);
            root.setMinWidth(width);
            root.setMaxWidth(width);
            VBox.setVgrow(root, Priority.ALWAYS);

            // Header
            Label headline = new Label(Res.get("chat.sideBar.userProfile.headline"));
            headline.setId("chat-sidebar-headline");

            closeButton = BisqIconButton.createIconButton("close");
            HBox.setMargin(headline, new Insets(15.5, 0, 0, 0));
            HBox.setMargin(closeButton, new Insets(10, 10, 0, 0));
            HBox header = new HBox(headline, Spacer.fillHBox(), closeButton);

            // Nickname, name and reputation
            nickName = new Label();
            nickName.getStyleClass().add("chat-side-bar-user-profile-nickname");
            if (controller.isUserProfileBanned()) {
                nickName.getStyleClass().add("error");
            }

            catIconImageView = new ImageView();
            catIconImageView.setFitWidth(100);
            catIconImageView.setFitHeight(100);

            reputationScoreDisplay = new ReputationScoreDisplay();
            reputationScoreDisplay.setAlignment(Pos.CENTER);
            VBox.setMargin(reputationScoreDisplay, new Insets(0, 0, 5, 0));

            // User details
            Triple<Label, BisqIconButton, VBox> botIdTriple = getInfoBoxWithCopyButton(Res.get("chat.sideBar.userProfile.nym"));
            botIdBox = botIdTriple.getThird();
            botIdCopyButton = botIdTriple.getSecond();
            botId = botIdTriple.getFirst();

            Triple<Label, BisqIconButton, VBox> userIdTriple = getInfoBoxWithCopyButton(Res.get("chat.sideBar.userProfile.id"));
            userIdBox = userIdTriple.getThird();
            userIdCopyButton = userIdTriple.getSecond();
            userId = userIdTriple.getFirst();

            Triple<Label, BisqIconButton, VBox> addressByTransportTriple = getInfoBoxWithCopyButton(Res.get("chat.sideBar.userProfile.transportAddress"));
            addressByTransportBox = addressByTransportTriple.getThird();
            addressByTransportCopyButton = addressByTransportTriple.getSecond();
            addressByTransport = addressByTransportTriple.getFirst();

            Triple<Label, Label, VBox> totalReputationScoreTriple = getInfoBox(Res.get("chat.sideBar.userProfile.totalReputationScore"));
            VBox totalReputationScoreBox = totalReputationScoreTriple.getThird();
            totalReputationScore = totalReputationScoreTriple.getSecond();

            Triple<Label, Label, VBox> profileAgeTriple = getInfoBox(Res.get("chat.sideBar.userProfile.profileAge"));
            VBox profileAgeBox = profileAgeTriple.getThird();
            profileAge = profileAgeTriple.getSecond();

            Triple<Label, Label, VBox> livenessStateTriple = getInfoBox(Res.get("chat.sideBar.userProfile.livenessState"));
            VBox livenessStateBox = livenessStateTriple.getThird();
            livenessState = livenessStateTriple.getSecond();

            Triple<Label, Label, VBox> versionTriple = getInfoBox(Res.get("chat.sideBar.userProfile.version"));
            VBox versionBox = versionTriple.getThird();
            version = versionTriple.getSecond();

            Triple<Label, Label, VBox> statementTriple = getInfoBox(Res.get("chat.sideBar.userProfile.statement"));
            statementBox = statementTriple.getThird();
            statement = statementTriple.getSecond();

            Triple<Label, Label, VBox> termsTriple = getInfoBox(Res.get("chat.sideBar.userProfile.terms"));
            termsBox = termsTriple.getThird();
            terms = termsTriple.getSecond();

            VBox content = new VBox(15, botIdBox, userIdBox, addressByTransportBox,
                    totalReputationScoreBox, profileAgeBox, livenessStateBox, versionBox, statementBox, termsBox);
            content.setMaxWidth(width - 15); // Remove the scrollbar
            content.setPadding(new Insets(0, 10, 20, 20));
            ScrollPane scrollPane = new ScrollPane(content);
            scrollPane.setMinWidth(width);
            VBox.setVgrow(content, Priority.ALWAYS);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            // Options
            privateMsg = new BisqMenuItem("private-chat-grey", "private-chat-white", Res.get("chat.sideBar.userProfile.sendPrivateMessage"));
            privateMsg.setPrefWidth(width);
            mention = new BisqMenuItem("mention-grey", "mention-white", Res.get("chat.sideBar.userProfile.mention"));
            mention.setPrefWidth(width);
            ignore = new BisqMenuItem("ignore-grey", "ignore-white", Res.get("chat.sideBar.userProfile.ignore"));
            ignore.setPrefWidth(width);
            undoIgnore = new BisqMenuItem("undo-ignore-grey", "undo-ignore-white", Res.get("chat.sideBar.userProfile.undoIgnore"));
            undoIgnore.setPrefWidth(width);
            report = new BisqMenuItem("report-grey", "report-white", Res.get("chat.sideBar.userProfile.report"));
            report.setPrefWidth(width);

            Region separator = Layout.hLine();
            VBox.setMargin(separator, new Insets(-15, -20, 10, -20));
            optionsVBox = new VBox(10, separator, privateMsg, mention, ignore, undoIgnore, report);
            optionsVBox.setAlignment(Pos.CENTER_LEFT);
            optionsVBox.setPadding(new Insets(0, 0, 20, 0));

            VBox.setMargin(header, new Insets(0, -20, 0, 0));
            VBox.setMargin(nickName, new Insets(10, 0, 0, 0));
            root.getChildren().addAll(header, nickName, catIconImageView, reputationScoreDisplay, scrollPane, optionsVBox);
        }

        @Override
        protected void onViewAttached() {
            catIconImageView.setImage(model.getCatHashImage());

            nickName.textProperty().bind(model.nickName);
            botId.textProperty().bind(model.nym);
            userId.textProperty().bind(model.userProfileIdString);
            addressByTransport.textProperty().bind(model.addressByTransport);
            statement.textProperty().bind(model.statement);
            statementBox.visibleProperty().bind(model.statement.isEmpty().not());
            statementBox.managedProperty().bind(model.statement.isEmpty().not());
            terms.textProperty().bind(model.terms);
            termsBox.visibleProperty().bind(model.terms.isEmpty().not());
            termsBox.managedProperty().bind(model.terms.isEmpty().not());
            profileAge.textProperty().bind(model.profileAge);
            livenessState.textProperty().bind(model.livenessState);
            version.textProperty().bind(model.version);
            ignore.visibleProperty().bind(model.ignoreUserSelected.not());
            ignore.managedProperty().bind(model.ignoreUserSelected.not());
            undoIgnore.visibleProperty().bind(model.ignoreUserSelected);
            undoIgnore.managedProperty().bind(model.ignoreUserSelected);
            optionsVBox.visibleProperty().bind(model.isPeer);
            optionsVBox.managedProperty().bind(model.isPeer);
            privateMsg.visibleProperty().bind(model.isPeer);
            privateMsg.managedProperty().bind(model.isPeer);

            reputationScorePin = EasyBind.subscribe(model.reputationScore, reputationScore -> {
                if (reputationScore != null) {
                    reputationScoreDisplay.setReputationScore(reputationScore);
                    totalReputationScore.setText(String.valueOf(reputationScore.getTotalScore()));
                }
            });

            botIdBox.setOnMouseEntered(e -> botIdCopyButton.setVisible(true));
            botIdBox.setOnMouseExited(e -> botIdCopyButton.setVisible(false));
            botIdCopyButton.setOnMouseClicked(e -> ClipboardUtil.copyToClipboard(model.getUserProfile().getNym()));
            userIdBox.setOnMouseEntered(e -> userIdCopyButton.setVisible(true));
            userIdBox.setOnMouseExited(e -> userIdCopyButton.setVisible(false));
            userIdCopyButton.setOnMouseClicked(e -> ClipboardUtil.copyToClipboard(model.getUserProfile().getId()));
            addressByTransportBox.setOnMouseEntered(e -> addressByTransportCopyButton.setVisible(true));
            addressByTransportBox.setOnMouseExited(e -> addressByTransportCopyButton.setVisible(false));
            addressByTransportCopyButton.setOnMouseClicked(e ->
                    ClipboardUtil.copyToClipboard(model.getUserProfile().getAddressByTransportDisplayString()));

            privateMsg.setOnAction(e -> controller.onSendPrivateMessage());
            mention.setOnAction(e -> controller.onMentionUser());
            ignore.setOnAction(e -> controller.onToggleIgnoreUser());
            undoIgnore.setOnAction(e -> controller.onToggleIgnoreUser());
            report.setOnAction(e -> controller.onReportUser());
            closeButton.setOnAction(e -> controller.onClose());
        }

        @Override
        protected void onViewDetached() {
            nickName.textProperty().unbind();
            botId.textProperty().unbind();
            userId.textProperty().unbind();
            addressByTransport.textProperty().unbind();
            statement.textProperty().unbind();
            statementBox.visibleProperty().unbind();
            statementBox.managedProperty().unbind();
            terms.textProperty().unbind();
            terms.visibleProperty().unbind();
            terms.managedProperty().unbind();
            profileAge.textProperty().unbind();
            livenessState.textProperty().unbind();
            version.textProperty().unbind();
            ignore.visibleProperty().unbind();
            ignore.managedProperty().unbind();
            undoIgnore.visibleProperty().unbind();
            undoIgnore.managedProperty().unbind();
            optionsVBox.visibleProperty().unbind();
            optionsVBox.managedProperty().unbind();
            privateMsg.visibleProperty().unbind();
            privateMsg.managedProperty().unbind();

            reputationScorePin.unsubscribe();

            botIdBox.setOnMouseEntered(null);
            botIdBox.setOnMouseExited(null);
            botIdCopyButton.setOnMouseClicked(null);
            userIdBox.setOnMouseEntered(null);
            userIdBox.setOnMouseExited(null);
            userIdCopyButton.setOnMouseClicked(null);
            addressByTransportBox.setOnMouseEntered(null);
            addressByTransportBox.setOnMouseExited(null);
            addressByTransportCopyButton.setOnMouseClicked(null);

            privateMsg.setOnAction(null);
            mention.setOnAction(null);
            ignore.setOnAction(null);
            undoIgnore.setOnAction(null);
            report.setOnAction(null);
            closeButton.setOnAction(null);
        }

        private static Triple<Label, Label, VBox> getInfoBox(String title) {
            Label headline = new Label(title.toUpperCase());
            headline.getStyleClass().add("chat-side-bar-user-profile-small-headline");

            Label value = new Label();
            value.setWrapText(true);
            value.getStyleClass().add("chat-side-bar-user-profile-small-value");

            VBox vBox = new VBox(2.5, headline, value);
            return new Triple<>(headline, value, vBox);
        }

        private static Triple<Label, BisqIconButton, VBox> getInfoBoxWithCopyButton(String title) {
            Label headline = new Label(title.toUpperCase());
            headline.getStyleClass().add("chat-side-bar-user-profile-small-headline");

            Label value = new Label();
            value.setWrapText(true);
            value.getStyleClass().add("chat-side-bar-user-profile-small-value");
            value.setContentDisplay(ContentDisplay.RIGHT);

            BisqIconButton copyButton = new BisqIconButton();
            copyButton.setIcon(AwesomeIcon.COPY);
            copyButton.setVisible(false);
            copyButton.setMinWidth(30);
            copyButton.setAlignment(Pos.BOTTOM_RIGHT);
            HBox.setMargin(copyButton, new Insets(0, 0, 5, 0));

            HBox hBox = new HBox(value, Spacer.fillHBox(), copyButton);
            hBox.setAlignment(Pos.BOTTOM_LEFT);
            VBox vBox = new VBox(2.5, headline, hBox);
            return new Triple<>(value, copyButton, vBox);
        }
    }
}
