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

package bisq.desktop.main.content.bisq_easy.private_chats;

import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.common.data.Triple;
import bisq.desktop.common.Layout;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.chat.ChatView;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.Optional;

@Slf4j
public class BisqEasyPrivateChatsView extends ChatView {
    private BisqTableView<ListItem> tableView;
    private VBox tableViewVBox, chatVBox;
    private Subscription noOpenChatsPin, selectedTableViewItemPin, selectedModelItemPin, peersUserProfilePin;
    private Label chatHeadline;


    public BisqEasyPrivateChatsView(BisqEasyPrivateChatsModel model,
                                    BisqEasyPrivateChatsController controller,
                                    VBox chatMessagesComponent,
                                    Pane channelSidebar) {
        super(model,
                controller,
                chatMessagesComponent,
                channelSidebar);

        root.setPadding(new Insets(0, 0, -67, 0));
    }

    @Override
    protected void configTitleHBox() {
    }

    @Override
    protected void configCenterVBox() {
        addTableBox();
        addChatBox();
    }

    private void addTableBox() {
        tableView = new BisqTableView<>(getModel().getSortedList());
        configTableView();

        VBox.setMargin(tableView, new Insets(10, 0, 0, 0));
        Triple<Label, HBox, VBox> triple = BisqEasyViewUtils.getContainer(Res.get("bisqEasy.privateChats.table.headline"), tableView);
        tableViewVBox = triple.getThird();
        VBox.setMargin(tableViewVBox, new Insets(0, 0, 10, 0));
        centerVBox.getChildren().add(tableViewVBox);
    }

    private void addChatBox() {
        chatMessagesComponent.setMinHeight(200);
        chatMessagesComponent.getStyleClass().add("bisq-easy-chat-messages-bg");
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        chatMessagesComponent.setPadding(new Insets(0, -30, -15, -30));
        Triple<Label, HBox, VBox> triple = BisqEasyViewUtils.getContainer("", chatMessagesComponent);
        chatHeadline = triple.getFirst();
        chatHeadline.setContentDisplay(ContentDisplay.RIGHT);
        chatHeadline.setGraphicTextGap(10);

        triple.getSecond().getChildren().addAll(Spacer.fillHBox());


        chatVBox = triple.getThird();
        VBox.setVgrow(chatVBox, Priority.ALWAYS);
        centerVBox.getChildren().add(chatVBox);
    }

    @Override
    protected void configSideBarVBox() {
        sideBar.getChildren().add(channelSidebar);
        sideBar.getStyleClass().add("bisq-easy-chat-sidebar-bg");
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setFillWidth(true);
    }

    @Override
    protected void configContainerHBox() {
        containerHBox.setSpacing(10);
        containerHBox.setFillHeight(true);
        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(centerVBox, sideBar);

        Layout.pinToAnchorPane(containerHBox, 30, 0, 0, 0);
        root.getChildren().add(containerHBox);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        BisqEasyPrivateChatsModel model = getModel();

        selectedModelItemPin = EasyBind.subscribe(model.getSelectedItem(), selected ->
                tableView.getSelectionModel().select(selected));
        selectedTableViewItemPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                item -> {
                    if (item != null) {
                        getController().onSelectItem(item);
                    }
                });

        noOpenChatsPin = EasyBind.subscribe(model.getNoOpenChats(),
                noOpenTrades -> {
                    if (noOpenTrades) {
                        tableView.removeListeners();
                        tableView.setPlaceholderText(Res.get("bisqEasy.privateChats.noChats"));
                        tableView.allowVerticalScrollbar();
                        tableView.setFixHeight(150);
                    } else {
                        tableView.setPlaceholder(null);
                        tableView.adjustHeightToNumRows();
                        tableView.hideVerticalScrollbar();
                    }
                    chatVBox.setVisible(!noOpenTrades);
                    chatVBox.setManaged(!noOpenTrades);
                });
        peersUserProfilePin = EasyBind.subscribe(model.getPeersUserProfile(),
                peersUserProfile -> {
                    if (peersUserProfile != null) {
                        UserProfileDisplay userProfileDisplay = new UserProfileDisplay(peersUserProfile);
                        userProfileDisplay.applyReputationScore(model.getPeersReputationScore());
                        chatHeadline.setGraphic(userProfileDisplay);
                    }
                });

    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        tableView.removeListeners();

        selectedModelItemPin.unsubscribe();
        selectedTableViewItemPin.unsubscribe();
        noOpenChatsPin.unsubscribe();
        peersUserProfilePin.unsubscribe();
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.privateChats.table.peer"))
                .minWidth(100)
                .left()
                .comparator(Comparator.comparing(ListItem::getPeersUserName))
                .setCellFactory(getTradePeerCellFactory())
                .build());

        //bisqEasy.privateChats.table.reputation=Chat peer
        //bisqEasy.privateChats.table.reputationScore=Reputation score
        //bisqEasy.privateChats.table.age=Profile age
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.privateChats.table.reputation"))
                .minWidth(100)
                .comparator(Comparator.comparing(ListItem::getTotalReputationScore))
                .setCellFactory(getReputationScoreCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.privateChats.table.totalReputationScore"))
                .minWidth(100)
                .comparator(Comparator.comparing(ListItem::getTotalReputationScore))
                .valueSupplier(ListItem::getTotalReputationScoreString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.privateChats.table.age"))
                .minWidth(100)
                .right()
                .comparator(Comparator.comparing(ListItem::getProfileAge))
                .valueSupplier(ListItem::getProfileAgeString)
                .build());
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getReputationScoreCellFactory() {
        return column -> new TableCell<>() {

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    setGraphic(new ReputationScoreDisplay(item.getReputationScore()));
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getTradePeerCellFactory() {
        return column -> new TableCell<>() {

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    UserProfileDisplay userProfileDisplay = new UserProfileDisplay(item.getChannel().getPeer());
                    userProfileDisplay.applyReputationScore(item.getReputationScore());
                    setGraphic(userProfileDisplay);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private BisqEasyPrivateChatsModel getModel() {
        return (BisqEasyPrivateChatsModel) model;
    }

    private BisqEasyPrivateChatsController getController() {
        return (BisqEasyPrivateChatsController) controller;
    }

    @Getter
    @EqualsAndHashCode
    static class ListItem implements TableItem {
        private final TwoPartyPrivateChatChannel channel;
        private final String peersUserName;
        private final long totalReputationScore, profileAge;
        private final String totalReputationScoreString, profileAgeString;
        private final ReputationScore reputationScore;

        public ListItem(TwoPartyPrivateChatChannel channel, ReputationService reputationService) {
            this.channel = channel;

            UserProfile userProfile = channel.getPeer();
            peersUserName = userProfile.getUserName();

            reputationScore = reputationService.getReputationScore(userProfile);
            totalReputationScore = reputationScore.getTotalScore();
            totalReputationScoreString = String.valueOf(totalReputationScore);

            Optional<Long> optionalProfileAge = reputationService.getProfileAgeService().getProfileAge(userProfile);
            profileAge = optionalProfileAge.orElse(0L);
            profileAgeString = optionalProfileAge
                    .map(TimeFormatter::formatAgeInDays)
                    .orElse(Res.get("data.na"));
        }
    }
}
