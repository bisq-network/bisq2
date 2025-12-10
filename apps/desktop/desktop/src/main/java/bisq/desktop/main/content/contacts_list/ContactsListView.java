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

package bisq.desktop.main.content.contacts_list;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.IndexColumnUtil;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.Optional;

@Slf4j
public class ContactsListView extends View<VBox, ContactsListModel, ContactsListController> {
    private static final double SIDE_PADDING = 40;

    private final RichTableView<ListItem> richTableView;
    private final Hyperlink learnMoreLink;
    private Subscription userProfileIdOfScoreUpdatePin;
    private UIScheduler uiScheduler;

    public ContactsListView(ContactsListModel model, ContactsListController controller) {
        super(new VBox(), model, controller);

        richTableView = new RichTableView<>(model.getSortedList(),
                Res.get("contactsList.table.headline"),
                controller::applySearchPredicate);
        learnMoreLink = new Hyperlink(Res.get("contactsList.table.placeholder.hyperlink"));
        richTableView.getExportButton().setVisible(false);
        richTableView.getExportButton().setManaged(false);
        richTableView.getHeadlineLabel().setGraphic(ImageUtil.getImageViewById("contacts-green"));
        richTableView.getHeadlineLabel().setGraphicTextGap(8);
        richTableView.getTableView().setPlaceholder(createAndGetPlaceholderContent());

        configTableView();

        root.getChildren().add(richTableView);
        root.setPadding(new Insets(SIDE_PADDING, SIDE_PADDING, 0 , SIDE_PADDING));
    }

    @Override
    protected void onViewAttached() {
        richTableView.initialize();
        richTableView.resetSearch();
        richTableView.setTableInfo(model.getTableInfoTitle(), model.getTableInfoContent());
        userProfileIdOfScoreUpdatePin = EasyBind.subscribe(model.getScoreChangeTrigger(), trigger -> {
            if (trigger != null) {
                richTableView.refresh();
                richTableView.sort();
            }
        });

        learnMoreLink.setOnAction(e -> richTableView.openLearnMorePopup());

        maybeShowLearnMorePopup();
    }

    @Override
    protected void onViewDetached() {
        richTableView.dispose();
        userProfileIdOfScoreUpdatePin.unsubscribe();

        learnMoreLink.setOnAction(null);

        if (uiScheduler != null) {
            uiScheduler.stop();
            uiScheduler = null;
        }
    }

    private void configTableView() {
        richTableView.getColumns().add(richTableView.getTableView().getSelectionMarkerColumn());

        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.userProfile"))
                .left()
                .minWidth(140)
                .comparator(Comparator.comparing(ListItem::getUserName))
                .setCellFactory(getUserProfileCellFactory())
                .valueSupplier(ListItem::getUserName)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("contactsList.table.columns.tag"))
                .minWidth(140)
                .comparator(Comparator.comparing(ListItem::getTag))
                .valueSupplier(ListItem::getTag)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("contactsList.table.columns.trustScore"))
                .minWidth(80)
                .comparator(Comparator.comparing(ListItem::getTrustScore))
                .valueSupplier(ListItem::getTrustScore)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.profileAge"))
                .minWidth(120)
                .comparator(Comparator.comparing(ListItem::getProfileAge).reversed())
                .valueSupplier(ListItem::getProfileAgeString)
                .includeForCsv(false)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.livenessState"))
                .minWidth(140)
                .comparator(Comparator.comparing(ListItem::getPublishDate).reversed())
                .valueSupplier(ListItem::getLastUserActivity)
                .build());
        BisqTableColumn<ListItem> reputationScoreColumn = new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("contactsList.table.columns.reputation"))
                .minWidth(100)
                .comparator(Comparator.comparing(ListItem::getTotalScore))
                .sortType(TableColumn.SortType.DESCENDING)
                .valueSupplier(ListItem::getTotalScoreString)
                .build();
        richTableView.getColumns().add(reputationScoreColumn);

        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("contactsList.table.columns.reason"))
                .minWidth(140)
                .comparator(Comparator.comparing(ListItem::getContactReasonString))
                .valueSupplier(ListItem::getContactReasonString)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("contactsList.table.columns.addedOn"))
                .minWidth(100)
                .comparator(Comparator.comparing(ListItem::getAddedDate))
                .valueSupplier(ListItem::getAddedDateString)
                .build());

        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .setCellFactory(getActionButtonsCellFactory())
                .minWidth(150)
                .includeForCsv(false)
                .build());
    }

    private void maybeShowLearnMorePopup() {
        UIThread.run(() -> {
            if (model.isShouldShowLearnMorePopup()) {
                uiScheduler = UIScheduler.run(richTableView::openLearnMorePopup).after(2000);
                controller.onShowLearnMorePopup();
            }
        });
    }

    private VBox createAndGetPlaceholderContent() {
        Label label = new Label(Res.get("contactsList.table.placeholder.title"));
        label.getStyleClass().addAll("thin-text", "very-large-text");
        VBox contentBox = new VBox(20);
        contentBox.getChildren().addAll(label, learnMoreLink);
        contentBox.setAlignment(Pos.CENTER);
        return contentBox;
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getUserProfileCellFactory() {
        return column -> new TableCell<>() {
            private UserProfileDisplay userProfileDisplay;

            @Override
            protected void updateItem(ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userProfileDisplay = new UserProfileDisplay(item.getUserProfile(), true, true);
                    userProfileDisplay.setReputationScore(item.getReputationScore());
                    setGraphic(userProfileDisplay);
                } else {
                    if (userProfileDisplay != null) {
                        userProfileDisplay.dispose();
                        userProfileDisplay = null;
                    }
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getActionButtonsCellFactory() {
        return column -> new TableCell<>() {
            private static final double PREF_WIDTH = 120;
            private static final double PREF_HEIGHT = 26;

            private final HBox mainBox = new HBox();
            private final HBox actionsMenuBox = new HBox(5);
            private final BisqMenuItem openPrivateChatMenuItem = new BisqMenuItem("private-chat-grey", "private-chat-white");
            private final BisqMenuItem showMoreInfoMenuItem = new BisqMenuItem("icon-info-grey", "icon-info-white");
            private final BisqMenuItem removeContactMenuItem = new BisqMenuItem("delete-t-grey", "delete-t-red");
            private final ChangeListener<Boolean> selectedListener = (observable, oldValue, newValue) -> {
                boolean shouldShow = newValue || getTableRow().isHover();
                actionsMenuBox.setVisible(shouldShow);
                actionsMenuBox.setManaged(shouldShow);
            };

            {
                mainBox.setMinWidth(PREF_WIDTH);
                mainBox.setPrefWidth(PREF_WIDTH);
                mainBox.setMaxWidth(PREF_WIDTH);
                mainBox.setMinHeight(PREF_HEIGHT);
                mainBox.setPrefHeight(PREF_HEIGHT);
                mainBox.setMaxHeight(PREF_HEIGHT);
                mainBox.getChildren().addAll(actionsMenuBox);

                actionsMenuBox.setMinWidth(PREF_WIDTH);
                actionsMenuBox.setPrefWidth(PREF_WIDTH);
                actionsMenuBox.setMaxWidth(PREF_WIDTH);
                actionsMenuBox.setMinHeight(PREF_HEIGHT);
                actionsMenuBox.setPrefHeight(PREF_HEIGHT);
                actionsMenuBox.setMaxHeight(PREF_HEIGHT);
                actionsMenuBox.getChildren().addAll(openPrivateChatMenuItem, showMoreInfoMenuItem, removeContactMenuItem);
                actionsMenuBox.setAlignment(Pos.CENTER);

                openPrivateChatMenuItem.useIconOnly();
                openPrivateChatMenuItem.setTooltip(
                        Res.get("contactsList.table.columns.actionsMenu.openPrivateChat.tooltip"));
                showMoreInfoMenuItem.useIconOnly();
                showMoreInfoMenuItem.setTooltip(
                        Res.get("contactsList.table.columns.actionsMenu.showMoreInfo.tooltip"));
                removeContactMenuItem.useIconOnly();
                removeContactMenuItem.setTooltip(
                        Res.get("contactsList.table.columns.actionsMenu.removeContact.tooltip"));
            }

            @Override
            protected void updateItem(ListItem item, boolean empty) {
                super.updateItem(item, empty);

                resetRowEventHandlersAndListeners();
                resetVisibilities();

                if (item != null && !empty) {
                    setUpRowEventHandlersAndListeners();
                    setGraphic(mainBox);
                    openPrivateChatMenuItem.setOnAction(
                            e -> controller.onOpenPrivateChat(item.getUserProfile().getId()));
                    showMoreInfoMenuItem.setOnAction(
                            e -> controller.onShowMoreInfo(item.getUserProfile()));
                    removeContactMenuItem.setOnAction(
                            e -> controller.onRemoveContact(item.getContactListEntry()));
                } else {
                    resetRowEventHandlersAndListeners();
                    resetVisibilities();
                    openPrivateChatMenuItem.setOnAction(null);
                    showMoreInfoMenuItem.setOnAction(null);
                    removeContactMenuItem.setOnAction(null);
                    setGraphic(null);
                }
            }

            private void setUpRowEventHandlersAndListeners() {
                TableRow<?> row = getTableRow();
                if (row != null) {
                    row.setOnMouseEntered(e -> {
                        boolean shouldShow = row.isSelected() || row.isHover();
                        actionsMenuBox.setVisible(shouldShow);
                        actionsMenuBox.setManaged(shouldShow);
                    });
                    row.setOnMouseExited(e -> {
                        boolean shouldShow = row.isSelected();
                        actionsMenuBox.setVisible(shouldShow);
                        actionsMenuBox.setManaged(shouldShow);
                    });
                    row.selectedProperty().addListener(selectedListener);
                }
            }

            private void resetRowEventHandlersAndListeners() {
                TableRow<?> row = getTableRow();
                if (row != null) {
                    row.setOnMouseEntered(null);
                    row.setOnMouseExited(null);
                    row.selectedProperty().removeListener(selectedListener);
                }
            }

            private void resetVisibilities() {
                actionsMenuBox.setVisible(false);
                actionsMenuBox.setManaged(false);
            }
        };
    }

    @Getter
    @ToString
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class ListItem {
        @EqualsAndHashCode.Include
        private final ContactListEntry contactListEntry;
        private final UserProfile userProfile;
        private final ReputationService reputationService;
        private final UserProfileService userProfileService;
        private final ContactsListController controller;
        private final String userName, profileAgeString, trustScore, tag, notes, contactReasonString, addedDateString;
        private final long profileAge;
        private final long addedDate;
        private ReputationScore reputationScore;
        private long totalScore;
        private String totalScoreString;

        ListItem(ContactListEntry contactListEntry,
                 ReputationService reputationService,
                 ContactsListController controller,
                 UserProfileService userProfileService) {
            this.contactListEntry = contactListEntry;
            this.userProfile = contactListEntry.getUserProfile();
            this.reputationService = reputationService;
            this.userProfileService = userProfileService;
            this.controller = controller;

            userName = userProfile.getUserName();
            contactReasonString = contactListEntry.getContactReason().getDisplayString();
            tag = contactListEntry.getTag().orElseGet(() -> Res.get("data.na"));
            //todo use custom trust display
            trustScore = contactListEntry.getTrustScore().map(PercentageFormatter::formatToPercentNoDecimalsWithSymbol).orElseGet(() -> Res.get("data.na"));
            notes = contactListEntry.getNotes().orElseGet(() -> Res.get("data.na"));
            Optional<Long> optionalProfileAge = reputationService.getProfileAgeService().getProfileAge(userProfile);
            profileAge = optionalProfileAge.orElse(0L);
            profileAgeString = optionalProfileAge
                    .map(TimeFormatter::formatAgeInDaysAndYears)
                    .orElseGet(() -> Res.get("data.na"));
            addedDate = contactListEntry.getDate();
            addedDateString = DateFormatter.formatDate(contactListEntry.getDate());

            applyReputationScore(userProfile.getId());
        }

        long getPublishDate() {
            return userProfile.getPublishDate();
        }

        void dispose() {
        }

        void applyReputationScore(String userProfileId) {
            reputationScore = reputationService.getReputationScore(userProfileId);
            totalScore = reputationScore.getTotalScore();
            totalScoreString = String.valueOf(totalScore);
        }

        public String getLastUserActivity() {
            long publishDate = userProfile.getPublishDate();
            if (publishDate == 0) {
                return Res.get("data.na");
            } else {
                return TimeFormatter.formatAge(Math.max(0, System.currentTimeMillis() - publishDate));
            }
        }
    }
}
