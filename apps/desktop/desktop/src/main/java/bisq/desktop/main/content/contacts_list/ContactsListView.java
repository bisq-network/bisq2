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

import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.IndexColumnUtil;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
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
    private Subscription userProfileIdOfScoreUpdatePin;

    public ContactsListView(ContactsListModel model,
                            ContactsListController controller) {
        super(new VBox(), model, controller);

        richTableView = new RichTableView<>(model.getSortedList(),
                Res.get("network.contactList.table.headline"),
                controller::applySearchPredicate);
        richTableView.getExportButton().setVisible(false);
        richTableView.getExportButton().setManaged(false);

        configTableView();

        root.getChildren().addAll(richTableView);
        root.setPadding(new Insets(SIDE_PADDING, SIDE_PADDING, 0 , SIDE_PADDING));
    }

    @Override
    protected void onViewAttached() {
        richTableView.initialize();
        richTableView.resetSearch();
        userProfileIdOfScoreUpdatePin = EasyBind.subscribe(model.getScoreChangeTrigger(), trigger -> {
            if (trigger != null) {
                richTableView.refresh();
                richTableView.sort();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        richTableView.dispose();
        userProfileIdOfScoreUpdatePin.unsubscribe();
    }

    private void configTableView() {
        richTableView.getColumns().add(richTableView.getTableView().getSelectionMarkerColumn());

        richTableView.getColumns().add(IndexColumnUtil.getIndexColumn(model.getSortedList()));
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.userProfile"))
                .left()
                .comparator(Comparator.comparing(ListItem::getUserName))
                .setCellFactory(getUserProfileCellFactory())
                .valueSupplier(ListItem::getUserName)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("network.contactList.table.columns.tag"))
                .comparator(Comparator.comparing(ListItem::getTag))
                .valueSupplier(ListItem::getTag)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("network.contactList.table.columns.trustScore"))
                .comparator(Comparator.comparing(ListItem::getTrustScore))
                .valueSupplier(ListItem::getTrustScore)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.profileAge"))
                .comparator(Comparator.comparing(ListItem::getProfileAge).reversed())
                .valueSupplier(ListItem::getProfileAgeString)
                .includeForCsv(false)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.livenessState"))
                .comparator(Comparator.comparing(ListItem::getPublishDate).reversed())
                .valueSupplier(ListItem::getLastUserActivity)
                .build());
        BisqTableColumn<ListItem> reputationScoreColumn = new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("network.contactList.table.columns.reputation"))
                .comparator(Comparator.comparing(ListItem::getTotalScore))
                .sortType(TableColumn.SortType.DESCENDING)
                .valueSupplier(ListItem::getTotalScoreString)
                .build();
        richTableView.getColumns().add(reputationScoreColumn);

        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("network.contactList.table.columns.added"))
                .comparator(Comparator.comparing(ListItem::getContactReasonString))
                .valueSupplier(ListItem::getContactReasonString)
                .build());
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getUserProfileCellFactory() {
        return column -> new TableCell<>() {
            private final Label userName = new Label();
            private final UserProfileIcon userProfileIcon = new UserProfileIcon(40);
            private final HBox hBox = new HBox(10, userProfileIcon, userName);

            {
                userName.setId("chat-user-name");
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userName.setText(item.getUserName());
                    // The update at the second tick would trigger a updateItem on all items not only on the visible
                    // ones, which cause a performance peak as creating lots of user profile icons is expensive
                    // (about 13ms on a fast machine) and need to be done on the UI thread.
                    // Therefor we deactivate the update of the last activity.
                    userProfileIcon.setUseSecondTick(false);
                    userProfileIcon.setUserProfile(item.getUserProfile());
                    userProfileIcon.getStyleClass().add("hand-cursor");
                    userName.setOnMouseClicked(e -> controller.openProfileCard(item.getUserProfile()));
                    setGraphic(hBox);
                } else {
                    userProfileIcon.dispose();
                    userName.setOnMouseClicked(null);
                    setGraphic(null);
                }
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

        private final String userName, profileAgeString, trustScore, tag, notes, contactReasonString;
        private ReputationScore reputationScore;
        private final long profileAge;
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
            tag = contactListEntry.getTag().orElse(Res.get("data.na"));
            //todo use custom trust display
            trustScore = contactListEntry.getTrustScore().map(String::valueOf).orElse(Res.get("data.na"));
            notes = contactListEntry.getNotes().orElse(Res.get("data.na"));
            Optional<Long> optionalProfileAge = reputationService.getProfileAgeService().getProfileAge(userProfile);
            profileAge = optionalProfileAge.orElse(0L);
            profileAgeString = optionalProfileAge
                    .map(TimeFormatter::formatAgeInDaysAndYears)
                    .orElse(Res.get("data.na"));

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
