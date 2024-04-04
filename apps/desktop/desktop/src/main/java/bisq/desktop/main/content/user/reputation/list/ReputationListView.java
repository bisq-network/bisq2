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

package bisq.desktop.main.content.user.reputation.list;

import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.StandardTable;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import bisq.user.reputation.ReputationSource;
import bisq.user.reputation.data.*;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ReputationListView extends View<VBox, ReputationListModel, ReputationListController> {
    private final BisqTableView<ListItem> tableView;
    private final StandardTable<ListItem> standardTable;
    private BisqTableColumn<ListItem> valueColumn;
    private Subscription userProfileIdOfScoreUpdatePin;

    public ReputationListView(ReputationListModel model,
                              ReputationListController controller) {
        super(new VBox(20), model, controller);

        standardTable = new StandardTable<>(model.getSortedList(),
                Res.get("user.reputation.table.headline"),
                model.getFilterItems(),
                model.getFilterMenuItemToggleGroup());
        tableView = standardTable.getTableView();
        configTableView();

        root.getChildren().addAll(standardTable);
    }

    @Override
    protected void onViewAttached() {
        standardTable.initialize();
        valueColumn.visibleProperty().bind(model.getValueColumnVisible());
        userProfileIdOfScoreUpdatePin = EasyBind.subscribe(model.getUserProfileIdOfScoreUpdate(), profileId -> {
            if (profileId != null) {
                tableView.refresh();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        standardTable.dispose();
        valueColumn.visibleProperty().unbind();
        userProfileIdOfScoreUpdatePin.unsubscribe();
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.reputation.table.columns.userProfile"))
                .left()
                .comparator(Comparator.comparing(ListItem::getUserName))
                .setCellFactory(getUserProfileCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.reputation.table.columns.profileAge"))
                .left()
                .comparator(Comparator.comparing(ListItem::getProfileAge))
                .valueSupplier(ListItem::getProfileAgeString)
                .build());

        BisqTableColumn<ListItem> scoreColumn = new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.reputation.table.columns.reputationScore"))
                .comparator(Comparator.comparing(ListItem::getTotalScore))
                .sortType(TableColumn.SortType.DESCENDING)
                .valueSupplier(ListItem::getTotalScoreString)
                .build();
        tableView.getColumns().add(scoreColumn);
        tableView.getSortOrder().add(scoreColumn);

        valueColumn = new BisqTableColumn.Builder<ListItem>()
                .titleProperty(model.getFilteredValueTitle())
                .comparator(Comparator.comparing(ListItem::getValue))
                .sortType(TableColumn.SortType.DESCENDING)
                .valueSupplier(ListItem::getValueString)
                .build();
        tableView.getColumns().add(valueColumn);

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.reputation.table.columns.reputation"))
                .comparator(Comparator.comparing(ListItem::getTotalScore))
                .sortType(TableColumn.SortType.DESCENDING)
                .setCellFactory(getStarsCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .isSortable(false)
                .title(Res.get("user.reputation.table.columns.details"))
                .setCellFactory(getDetailsCellFactory())
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
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userName.setText(item.getUserName());
                    userProfileIcon.setUserProfile(item.getUserProfile());
                    setGraphic(hBox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getStarsCellFactory() {
        return column -> new TableCell<>() {
            private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();

            @Override
            public void updateItem(final ListItem item, boolean empty) {
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
            private final Hyperlink info = new Hyperlink(Res.get("user.reputation.table.columns.details.button"));

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    info.setOnAction(e -> controller.onShowDetails(item));
                    setGraphic(info);
                } else {
                    info.setOnAction(null);
                    setGraphic(null);
                }
            }
        };
    }

    @EqualsAndHashCode
    @Getter
    @ToString
    public static class ListItem {
        private final ReputationService reputationService;
        private final UserProfile userProfile;
        private final Map<ReputationSource, Long> amountBySource = new HashMap<>();
        private ReputationScore reputationScore;
        private final String userName;
        private final ReputationListController controller;
        private final ToggleGroup toggleGroup;
        private final String profileAgeString;
        private final long profileAge;
        private long totalScore;
        private String totalScoreString;
        private long value;
        private String valueString;
        private final Set<ReputationSource> reputationSources = new HashSet<>();
        private final Subscription selectedTogglePin;

        ListItem(UserProfile userProfile, ReputationService reputationService, ReputationListController controller, ToggleGroup toggleGroup) {
            this.reputationService = reputationService;
            this.userProfile = userProfile;
            userName = userProfile.getUserName();
            this.controller = controller;
            this.toggleGroup = toggleGroup;
            applyReputationScore(userProfile.getId());
            profileAge = reputationService.getProfileAgeService().getProfileAge(userProfile).orElse(0L);
            profileAgeString = reputationService.getProfileAgeService().getProfileAge(userProfile)
                    .map(TimeFormatter::formatAgeInDays)
                    .orElse(Res.get("data.na"));

            selectedTogglePin = EasyBind.subscribe(toggleGroup.selectedToggleProperty(), this::selectedToggleChanged);
        }

        public void dispose() {
            selectedTogglePin.unsubscribe();
        }

        void applyReputationScore(String userProfileId) {
            Optional<ReputationSource> selectedReputationSource = controller.resolveReputationSource(toggleGroup.getSelectedToggle());
            reputationScore = reputationService.findReputationScore(userProfileId).orElse(ReputationScore.NONE);
            if (selectedReputationSource.isEmpty() || !amountBySource.containsKey(selectedReputationSource.get())) {
                totalScore = reputationScore.getTotalScore();
                totalScoreString = String.valueOf(totalScore);
                valueString = String.valueOf(totalScore);
            } else {
                value = amountBySource.get(selectedReputationSource.get());
                switch (selectedReputationSource.get()) {
                    case BURNED_BSQ:
                    case BSQ_BOND:
                        valueString = String.valueOf(value);
                        break;
                    case PROFILE_AGE:
                    case BISQ1_ACCOUNT_AGE:
                    case BISQ1_SIGNED_ACCOUNT_AGE_WITNESS:
                        valueString = TimeFormatter.formatAgeInDays(value);
                        break;
                }
            }

            updateAmountBySource();
        }

        private void updateAmountBySource() {
            Optional.ofNullable(reputationService.getProofOfBurnService().getDataSetByHash().get(userProfile.getProofOfBurnKey()))
                    .ifPresent(dataSet -> amountBySource.putIfAbsent(ReputationSource.BURNED_BSQ,
                            dataSet.stream().mapToLong(AuthorizedProofOfBurnData::getAmount).sum()));

            Optional.ofNullable(reputationService.getBondedReputationService().getDataSetByHash().get(userProfile.getBondedReputationKey()))
                    .ifPresent(dataSet -> amountBySource.putIfAbsent(ReputationSource.BSQ_BOND,
                            dataSet.stream().mapToLong(AuthorizedBondedReputationData::getAmount).sum()));

            Optional.ofNullable(reputationService.getAccountAgeService().getDataSetByHash().get(userProfile.getAccountAgeKey()))
                    .ifPresent(dataSet -> amountBySource.putIfAbsent(ReputationSource.BISQ1_ACCOUNT_AGE,
                            dataSet.stream().mapToLong(AuthorizedAccountAgeData::getDate).sum()));

            Optional.ofNullable(reputationService.getSignedWitnessService().getDataSetByHash().get(userProfile.getSignedWitnessKey()))
                    .ifPresent(dataSet -> amountBySource.putIfAbsent(ReputationSource.BISQ1_SIGNED_ACCOUNT_AGE_WITNESS,
                            dataSet.stream().mapToLong(AuthorizedSignedWitnessData::getWitnessSignDate).sum()));

            Optional.ofNullable(reputationService.getProfileAgeService().getDataSetByHash().get(userProfile.getProfileAgeKey()))
                    .ifPresent(dataSet -> amountBySource.putIfAbsent(ReputationSource.PROFILE_AGE,
                            dataSet.stream().mapToLong(AuthorizedTimestampData::getDate).sum()));
        }

        private void selectedToggleChanged(Toggle selectedToggle) {
            Optional<ReputationSource> selectedReputationSource = controller.resolveReputationSource(selectedToggle);
            reputationSources.addAll(amountBySource.entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .filter(e -> selectedReputationSource.isEmpty() || e.getKey().equals(selectedReputationSource.get()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet()));

            applyReputationScore(userProfile.getId());
        }
    }
}
