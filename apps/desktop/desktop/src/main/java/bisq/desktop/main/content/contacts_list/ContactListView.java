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

import bisq.common.data.Pair;
import bisq.common.monetary.Coin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.IndexColumnUtil;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import bisq.user.reputation.ReputationSource;
import bisq.user.reputation.data.AuthorizedAccountAgeData;
import bisq.user.reputation.data.AuthorizedBondedReputationData;
import bisq.user.reputation.data.AuthorizedProofOfBurnData;
import bisq.user.reputation.data.AuthorizedSignedWitnessData;
import bisq.user.reputation.data.AuthorizedTimestampData;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ContactListView extends View<VBox, ContactListModel, ContactListController> {
    private static final double SIDE_PADDING = 40;

    private final RichTableView<ListItem> richTableView;
    private BisqTableColumn<ListItem> scoreColumn, valueColumn;
    private Subscription userProfileIdOfScoreUpdatePin, selectedReputationSourcePin;

    public ContactListView(ContactListModel model,
                           ContactListController controller) {
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
        valueColumn.visibleProperty().bind(model.getValueColumnVisible());
        userProfileIdOfScoreUpdatePin = EasyBind.subscribe(model.getScoreChangeTrigger(), trigger -> {
            if (trigger != null) {
                richTableView.refresh();
                richTableView.sort();
            }
        });

        selectedReputationSourcePin = EasyBind.subscribe(model.getSelectedReputationSource(), selectedReputationSource -> UIThread.runOnNextRenderFrame(() -> {
            richTableView.getSortOrder().clear();
            if (selectedReputationSource == null) {
                richTableView.getSortOrder().add(scoreColumn);
            } else {

                switch (selectedReputationSource) {
                    case BURNED_BSQ:
                    case BSQ_BOND:
                        valueColumn.setSortType(TableColumn.SortType.DESCENDING);
                        break;
                    case PROFILE_AGE:
                    case BISQ1_ACCOUNT_AGE:
                    case BISQ1_SIGNED_ACCOUNT_AGE_WITNESS:
                        valueColumn.setSortType(TableColumn.SortType.ASCENDING);
                        break;
                }
                richTableView.getSortOrder().add(valueColumn);
            }
        }));
    }

    @Override
    protected void onViewDetached() {
        richTableView.dispose();
        valueColumn.visibleProperty().unbind();
        userProfileIdOfScoreUpdatePin.unsubscribe();
        selectedReputationSourcePin.unsubscribe();
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
                .title(Res.get("network.contactList.table.columns.contactReason"))
                .comparator(Comparator.comparing(ListItem::getContactReasonString))
                .valueSupplier(ListItem::getContactReasonString)
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
                .title(Res.get("network.contactList.table.columns.notes"))
                .comparator(Comparator.comparing(ListItem::getNotes))
                .valueSupplier(ListItem::getNotes)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.profileAge"))
                .comparator(Comparator.comparing(ListItem::getProfileAge).reversed())
                .valueSupplier(ListItem::getProfileAgeString)
                .includeForCsv(false)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.livenessState"))
                .right()
                .comparator(Comparator.comparing(ListItem::getPublishDate).reversed())
                .valueSupplier(ListItem::getLastUserActivity)
                .build());
        scoreColumn = new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.reputationScore"))
                .comparator(Comparator.comparing(ListItem::getTotalScore))
                .sortType(TableColumn.SortType.DESCENDING)
                .valueSupplier(ListItem::getTotalScoreString)
                .build();
        richTableView.getColumns().add(scoreColumn);

        valueColumn = new BisqTableColumn.Builder<ListItem>()
                .minWidth(150)
                .titleProperty(model.getFilteredValueTitle())
                .comparator(Comparator.comparing(ListItem::getValue))
                .valuePropertySupplier(ListItem::getValueAsStringProperty)
                .includeForCsv(false)
                .build();
        richTableView.getColumns().add(valueColumn);

        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.table.columns.reputation"))
                .comparator(Comparator.comparing(ListItem::getTotalScore))
                .sortType(TableColumn.SortType.DESCENDING)
                .setCellFactory(getStarsCellFactory())
                .includeForCsv(false)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .isSortable(false)
                .title(Res.get("reputation.table.columns.details"))
                .setCellFactory(getDetailsCellFactory())
                .includeForCsv(false)
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

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getStarsCellFactory() {
        return column -> new TableCell<>() {
            private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();

            {
                reputationScoreDisplay.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    reputationScoreDisplay.setReputationScore(item.getReputationScore());
                    setGraphic(reputationScoreDisplay);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getDetailsCellFactory() {
        return column -> new TableCell<>() {
            private final Hyperlink info = new Hyperlink(Res.get("reputation.table.columns.details.button"));

            @Override
            protected void updateItem(ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    info.setOnAction(e -> controller.onOpenProfileCard(item.getUserProfile()));
                    setGraphic(info);
                } else {
                    info.setOnAction(null);
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
        private final ContactListController controller;

        private final String userName, profileAgeString, trustScore, tag, notes, contactReasonString;
        private ReputationScore reputationScore;
        private final long profileAge;
        private long totalScore;
        private String totalScoreString;
        private final Map<ReputationSource, Pair<Long, String>> valuePairBySource = new HashMap<>();
        private long value;
        private final StringProperty valueAsStringProperty = new SimpleStringProperty();
        private final Set<ReputationSource> reputationSources = new HashSet<>();

        ListItem(ContactListEntry contactListEntry,
                 ReputationService reputationService,
                 ContactListController controller,
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
            value = totalScore;
            valueAsStringProperty.set(String.valueOf(totalScore));

            updateAmountBySource();
        }

        private void updateAmountBySource() {
            applyReputationSourceValue(ReputationSource.BURNED_BSQ,
                    Optional.ofNullable(reputationService.getProofOfBurnService().getDataSetByHash().get(userProfile.getProofOfBurnKey()))
                            .map(dataSet -> dataSet.stream().mapToLong(AuthorizedProofOfBurnData::getAmount).sum())
                            .orElse(0L));

            applyReputationSourceValue(ReputationSource.BSQ_BOND,
                    Optional.ofNullable(reputationService.getBondedReputationService().getDataSetByHash().get(userProfile.getBondedReputationKey()))
                            .map(dataSet -> dataSet.stream().mapToLong(AuthorizedBondedReputationData::getAmount).sum())
                            .orElse(0L));

            applyReputationSourceValue(ReputationSource.BISQ1_ACCOUNT_AGE,
                    Optional.ofNullable(reputationService.getAccountAgeService().getDataSetByHash().get(userProfile.getAccountAgeKey()))
                            .map(dataSet -> dataSet.stream().mapToLong(AuthorizedAccountAgeData::getDate).sum())
                            .orElse(0L));

            applyReputationSourceValue(ReputationSource.BISQ1_SIGNED_ACCOUNT_AGE_WITNESS,
                    Optional.ofNullable(reputationService.getSignedWitnessService().getDataSetByHash().get(userProfile.getSignedWitnessKey()))
                            .map(dataSet -> dataSet.stream().mapToLong(AuthorizedSignedWitnessData::getWitnessSignDate).sum())
                            .orElse(0L));

            applyReputationSourceValue(ReputationSource.PROFILE_AGE,
                    Optional.ofNullable(reputationService.getProfileAgeService().getDataSetByHash().get(userProfile.getProfileAgeKey()))
                            .map(dataSet -> dataSet.stream().mapToLong(AuthorizedTimestampData::getDate).sum())
                            .orElse(0L));
        }

        private void applyReputationSourceValue(ReputationSource reputationSource, long value) {
            valuePairBySource.put(reputationSource, new Pair<>(value, formatReputationSourceValue(reputationSource, value)));
        }

        private String formatReputationSourceValue(ReputationSource reputationSource, long value) {
            return switch (reputationSource) {
                case BURNED_BSQ, BSQ_BOND -> AmountFormatter.formatAmount(Coin.asBsqFromValue(value), false);
                case PROFILE_AGE, BISQ1_ACCOUNT_AGE, BISQ1_SIGNED_ACCOUNT_AGE_WITNESS ->
                        value > 0 ? TimeFormatter.formatAgeInDaysAndYears(value) : "";
            };
        }

        private Set<ReputationSource> getFilteredReputationSources(Optional<ReputationSource> selectedReputationSource) {
            return valuePairBySource.entrySet().stream()
                    .filter(e -> e.getValue().getFirst() > 0)
                    .filter(e -> selectedReputationSource.isEmpty() || e.getKey().equals(selectedReputationSource.get()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
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
