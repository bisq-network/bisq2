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

package bisq.desktop.main.content.user.profile_card.reputation;

import bisq.common.monetary.Coin;
import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.DateColumnUtil;
import bisq.desktop.components.table.DateTableItem;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.reputation.ReputationSource;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Optional;

@Slf4j
public class ProfileCardReputationView extends View<VBox, ProfileCardReputationModel, ProfileCardReputationController> {
    private final BisqTableView<ListItem> tableView;

    public ProfileCardReputationView(ProfileCardReputationModel model,
                                     ProfileCardReputationController controller) {
        super(new VBox(), model, controller);

        VBox vBox = new VBox();
        vBox.setFillWidth(true);
        vBox.getStyleClass().add("header");
        tableView = new BisqTableView<>(model.getListItems());
        tableView.getStyleClass().addAll("reputation-table", "rich-table-view");
        tableView.allowVerticalScrollbar();
        configTableView();
        root.getChildren().addAll(vBox, tableView);
        root.setPadding(new Insets(20, 0, 0, 0));
        root.getStyleClass().add("reputation");
    }

    @Override
    protected void onViewAttached() {
        tableView.initialize();
    }

    @Override
    protected void onViewDetached() {
        tableView.dispose();
    }

    private void configTableView() {
        BisqTableColumn<ListItem> dateColumn = DateColumnUtil.getDateColumn(tableView.getSortOrder());
        dateColumn.setMinWidth(100);
        tableView.getColumns().add(dateColumn);

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.details.table.columns.source"))
                .left()
                .comparator(Comparator.comparing(ListItem::getReputationSource))
                .valueSupplier(ListItem::getSourceString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.details.table.columns.score"))
                .comparator(Comparator.comparing(ListItem::getScore))
                .valueSupplier(ListItem::getScoreString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("temporal.age"))
                .comparator(Comparator.comparing(ListItem::getAge))
                .valueSupplier(ListItem::getAgeString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("offer.amount"))
                .comparator(Comparator.comparing(ListItem::getAmount))
                .valueSupplier(ListItem::getAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation.details.table.columns.lockTime"))
                .comparator(Comparator.comparing(ListItem::getLockTime))
                .valueSupplier(ListItem::getLockTimeString)
                .build());
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    static class ListItem implements DateTableItem {
        @EqualsAndHashCode.Include
        private final ReputationSource reputationSource;
        @EqualsAndHashCode.Include
        private final long date, score, amount, lockTime;

        private final long age;
        private final String dateString, timeString, sourceString, ageString, amountString, scoreString, lockTimeString;

        public ListItem(ReputationSource reputationSource, long blockTime, long score) {
            this(reputationSource, blockTime, score, Optional.empty(), Optional.empty());
        }

        public ListItem(ReputationSource reputationSource, long blockTime, long score, long amount) {
            this(reputationSource, blockTime, score, Optional.of(amount), Optional.empty());
        }

        public ListItem(ReputationSource reputationSource,
                        long blockTime,
                        long score,
                        Optional<Long> optionalAmount,
                        Optional<Long> optionalLockTime) {
            this.reputationSource = reputationSource;
            this.date = blockTime;
            this.score = score;
            this.amount = optionalAmount.orElse(0L);
            this.lockTime = optionalLockTime.orElse(0L);

            dateString = DateFormatter.formatDate(blockTime);
            timeString = DateFormatter.formatTime(blockTime);
            age = TimeFormatter.getAgeInDays(blockTime);
            ageString = TimeFormatter.formatAgeInDays(blockTime);
            sourceString = reputationSource.getDisplayString();
            amountString = optionalAmount.map(amount -> AmountFormatter.formatAmountWithCode(Coin.fromValue(amount, "BSQ"))).orElse("-");
            scoreString = String.valueOf(score);
            lockTimeString = optionalLockTime.map(String::valueOf).orElse("-");
        }
    }
}
