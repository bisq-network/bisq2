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

package bisq.desktop.primary.overlay.onboarding.offer.market;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.primary.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class MarketView extends View<VBox, MarketModel, MarketController> {
    private final BisqTableView<MarketListItem> tableView;

    public MarketView(MarketModel model, MarketController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("bisq-content-bg");

        Label headLineLabel = new Label(Res.get("onboarding.market.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.market.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-10", "wrap-text");

        tableView = new BisqTableView<>(model.sortedList);
        tableView.getStyleClass().add("onboarding-market-selection-table-view");
        tableView.setMinHeight(240);
        tableView.setMaxWidth(720);
        configTableView();
        

        VBox.setMargin(headLineLabel, new Insets(38, 0, 4, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 20, 0));
        VBox.setMargin(tableView, new Insets(0, 0, 30, 0));

        root.getChildren().addAll(headLineLabel, subtitleLabel, tableView, Spacer.fillVBox());
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .title(Res.get("onboarding.market.columns.name"))
                .isFirst()
                .minWidth(100)
                .comparator(Comparator.comparing(MarketListItem::getName))
                .setCellFactory(getNameCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .title(Res.get("onboarding.market.columns.offers"))
                .minWidth(60)
                .valueSupplier(MarketListItem::getOffersCount)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .title(Res.get("onboarding.market.columns.peers"))
                .minWidth(60)
                .valueSupplier(MarketListItem::getOnlinePeersCount)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .fixWidth(140)
                .value(Res.get("shared.select"))
                .cellFactory(BisqTableColumn.DefaultCellFactories.BUTTON)
                .actionHandler(controller::onSelect)
                .build());
    }

    private Callback<TableColumn<MarketListItem, MarketListItem>, TableCell<MarketListItem, MarketListItem>> getNameCellFactory() {
        return column -> new TableCell<>() {
            final Label label = new Label();
            {
                label.setGraphicTextGap(16);
            }

            @Override
            public void updateItem(final MarketListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    label.setGraphic(
                            MarketImageComposition.imageBoxForMarket(
                                    item.getBaseCurrencyCode().toLowerCase(),
                                    item.getQuoteCurrencyCode().toLowerCase()
                            )
                    );
                    label.setText(item.getName());
                    setGraphic(label);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    @Override
    protected void onViewAttached() {
        model.fillObservableList();        
    }

    @Override
    protected void onViewDetached() {
    }
}
