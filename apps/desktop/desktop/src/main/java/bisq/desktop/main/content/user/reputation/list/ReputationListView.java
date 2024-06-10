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

import bisq.common.data.Pair;
import bisq.common.monetary.Coin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.StandardTable;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import bisq.user.reputation.ReputationSource;
import bisq.user.reputation.data.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import java.util.stream.Stream;

@Slf4j
public class ReputationListView extends View<VBox, ReputationListModel, ReputationListController> {
    private final BisqTableView<ListItem> tableView;
    private final StandardTable<ListItem> standardTable;
    private BisqTableColumn<ListItem> scoreColumn, valueColumn;
    private Subscription userProfileIdOfScoreUpdatePin, selectedReputationSourcePin;

    public ReputationListView(ReputationListModel model,
                              ReputationListController controller) {
        super(new VBox(20), model, controller);

        standardTable = new StandardTable<>(model.getSortedList(),
                Res.get("user.reputation.table.headline"),
                model.getFilterItems(),
                model.getFilterMenuItemToggleGroup(),
                this::applySearchPredicate);
        tableView = standardTable.getTableView();
        configTableView();

        root.getChildren().addAll(standardTable);
    }

    @Override
    protected void onViewAttached() {
        standardTable.initialize();
        standardTable.resetSearch();
        valueColumn.visibleProperty().bind(model.getValueColumnVisible());
        userProfileIdOfScoreUpdatePin = EasyBind.subscribe(model.getUserProfileIdOfScoreUpdate(), profileId -> {
            if (profileId != null) {
                tableView.refresh();
            }
        });

        selectedReputationSourcePin = EasyBind.subscribe(model.getSelectedReputationSource(), selectedReputationSource -> {
            UIThread.runOnNextRenderFrame(() -> {
                tableView.getSortOrder().clear();
                if (selectedReputationSource == null) {
                    tableView.getSortOrder().add(scoreColumn);
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
                    tableView.getSortOrder().add(valueColumn);
                }
            });
        });

        List<String> csvHeaders = standardTable.buildCsvHeaders();
        csvHeaders.add(Res.get("user.reputation.ranking").toUpperCase());
        csvHeaders.addAll(Stream.of(ReputationSource.values())
                .map(reputationSource -> reputationSource.getDisplayString().toUpperCase())
                .collect(Collectors.toList()));
        csvHeaders.add(Res.get("component.standardTable.csv.plainValue", Res.get("user.reputation.table.columns.lastSeen").toUpperCase()));
        csvHeaders.addAll(Stream.of(ReputationSource.values())
                .map(reputationSource -> Res.get("component.standardTable.csv.plainValue", reputationSource.getDisplayString().toUpperCase()))
                .collect(Collectors.toList()));
        standardTable.setCsvHeaders(Optional.of(csvHeaders));

        List<List<String>> csvData = tableView.getItems().stream()
                .map(item -> {
                    List<String> cellDataInRow = standardTable.getBisqTableColumnsForCsv()
                            .map(bisqTableColumn -> bisqTableColumn.resolveValueForCsv(item))
                            .collect(Collectors.toList());

                    // Add ranking
                    cellDataInRow.add(item.getReputationScore().getRankingAsString());

                    // Add formatted values
                    cellDataInRow.addAll(item.getValuePairBySource().entrySet().stream()
                            .sorted(Comparator.comparingLong(o -> o.getKey().ordinal()))
                            .map(Map.Entry::getValue)
                            .map(Pair::getSecond)
                            .collect(Collectors.toList()));

                    // Add lastSeen plain value
                    cellDataInRow.add(String.valueOf(item.getLastSeen()));

                    // Add plain values (for better filter/sorting)
                    cellDataInRow.addAll(item.getValuePairBySource().entrySet().stream()
                            .sorted(Comparator.comparingLong(o -> o.getKey().ordinal()))
                            .map(Map.Entry::getValue)
                            .map(e -> String.valueOf(e.getFirst()))
                            .collect(Collectors.toList()));
                    return cellDataInRow;
                })
                .collect(Collectors.toList());
        standardTable.setCsvData(Optional.of(csvData));
    }

    @Override
    protected void onViewDetached() {
        standardTable.dispose();
        valueColumn.visibleProperty().unbind();
        userProfileIdOfScoreUpdatePin.unsubscribe();
        selectedReputationSourcePin.unsubscribe();
    }

    private void applySearchPredicate(String searchText) {
        String string = searchText.toLowerCase();
        model.getFilteredList().setPredicate(item ->
                StringUtils.isEmpty(string) ||
                        item.getUserName().toLowerCase().contains(string) ||
                        item.getUserProfile().getNym().toLowerCase().contains(string) ||
                        item.getTotalScoreString().contains(string) ||
                        item.getProfileAgeString().contains(string) ||
                        item.getValueAsStringProperty().get().toLowerCase().contains(string));
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.reputation.table.columns.userProfile"))
                .left()
                .comparator(Comparator.comparing(ListItem::getUserName))
                .setCellFactory(getUserProfileCellFactory())
                .valueSupplier(ListItem::getUserName)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.reputation.table.columns.profileAge"))
                .left()
                .comparator(Comparator.comparing(ListItem::getProfileAge))
                .valueSupplier(ListItem::getProfileAgeString)
                .includeForCsv(false)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.reputation.table.columns.lastSeen"))
                .left()
                .comparator(Comparator.comparing(ListItem::getLastSeen))
                .valueSupplier(ListItem::getLastSeenAsString)
                .build());

        scoreColumn = new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.reputation.table.columns.reputationScore"))
                .comparator(Comparator.comparing(ListItem::getTotalScore))
                .sortType(TableColumn.SortType.DESCENDING)
                .valueSupplier(ListItem::getTotalScoreString)
                .build();
        tableView.getColumns().add(scoreColumn);

        valueColumn = new BisqTableColumn.Builder<ListItem>()
                .titleProperty(model.getFilteredValueTitle())
                .comparator(Comparator.comparing(ListItem::getValue))
                .valuePropertySupplier(ListItem::getValueAsStringProperty)
                .includeForCsv(false)
                .build();
        tableView.getColumns().add(valueColumn);

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.reputation.table.columns.reputation"))
                .comparator(Comparator.comparing(ListItem::getTotalScore))
                .sortType(TableColumn.SortType.DESCENDING)
                .setCellFactory(getStarsCellFactory())
                .includeForCsv(false)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .isSortable(false)
                .title(Res.get("user.reputation.table.columns.details"))
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
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userName.setText(item.getUserName());
                    userProfileIcon.applyData(item.getUserProfile(), item.getLastSeenAsString(), item.getLastSeen());
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
                    reputationScoreDisplay.setAlignment(Pos.CENTER);
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

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @ToString
    public static class ListItem {
        @EqualsAndHashCode.Include
        private final UserProfile userProfile;
        private final String userName;
        private ReputationScore reputationScore;
        private final String profileAgeString;
        private final long profileAge;
        private long totalScore;
        private String totalScoreString;
        private final Map<ReputationSource, Pair<Long, String>> valuePairBySource = new HashMap<>();
        private long value;
        private final StringProperty valueAsStringProperty = new SimpleStringProperty();
        private final Set<ReputationSource> reputationSources = new HashSet<>();
        private final long lastSeen;
        private final String lastSeenAsString;

        private final ToggleGroup toggleGroup;
        private final ReputationListController controller;
        private final ReputationService reputationService;
        private final UserProfileService userProfileService;

        private final Subscription selectedTogglePin;

        ListItem(UserProfile userProfile,
                 ReputationService reputationService,
                 ReputationListController controller,
                 ToggleGroup toggleGroup,
                 UserProfileService userProfileService) {
            this.reputationService = reputationService;
            this.userProfile = userProfile;
            userName = userProfile.getUserName();
            this.controller = controller;
            this.toggleGroup = toggleGroup;
            this.userProfileService = userProfileService;
            applyReputationScore(userProfile.getId());
            profileAge = reputationService.getProfileAgeService().getProfileAge(userProfile).orElse(0L);
            profileAgeString = reputationService.getProfileAgeService().getProfileAge(userProfile)
                    .map(TimeFormatter::formatAgeInDays)
                    .orElse(Res.get("data.na"));

            selectedTogglePin = EasyBind.subscribe(toggleGroup.selectedToggleProperty(), this::selectedToggleChanged);

            lastSeen = userProfileService.getLastSeen(userProfile);
            lastSeenAsString = TimeFormatter.formatAge(lastSeen);
        }

        public void dispose() {
            selectedTogglePin.unsubscribe();
        }

        void applyReputationScore(String userProfileId) {
            Optional<ReputationSource> selectedReputationSource = controller.resolveReputationSource(toggleGroup.getSelectedToggle());
            reputationScore = reputationService.getReputationScore(userProfileId);
            if (selectedReputationSource.isEmpty() || !valuePairBySource.containsKey(selectedReputationSource.get())) {
                totalScore = reputationScore.getTotalScore();
                totalScoreString = String.valueOf(totalScore);
                valueAsStringProperty.set(String.valueOf(totalScore));
            } else {
                Pair<Long, String> pair = valuePairBySource.get(selectedReputationSource.get());
                value = pair.getFirst();
                valueAsStringProperty.set(pair.getSecond());
            }

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
            valuePairBySource.putIfAbsent(reputationSource, new Pair<>(value, formatReputationSourceValue(reputationSource, value)));
        }

        private String formatReputationSourceValue(ReputationSource reputationSource, long value) {
            switch (reputationSource) {
                case BURNED_BSQ:
                case BSQ_BOND:
                    return AmountFormatter.formatAmount(Coin.asBsqFromValue(value));
                case PROFILE_AGE:
                case BISQ1_ACCOUNT_AGE:
                case BISQ1_SIGNED_ACCOUNT_AGE_WITNESS:
                    return value > 0 ? TimeFormatter.formatAgeInDays(value) : "";
                default:
                    return String.valueOf(value);
            }
        }

        private void selectedToggleChanged(Toggle selectedToggle) {
            Optional<ReputationSource> selectedReputationSource = controller.resolveReputationSource(selectedToggle);
            reputationSources.addAll(getFilteredReputationSources(selectedReputationSource));
            applyReputationScore(userProfile.getId());
        }

        private Set<ReputationSource> getFilteredReputationSources(Optional<ReputationSource> selectedReputationSource) {
            return valuePairBySource.entrySet().stream()
                    .filter(e -> e.getValue().getFirst() > 0)
                    .filter(e -> selectedReputationSource.isEmpty() || e.getKey().equals(selectedReputationSource.get()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }
    }
}
