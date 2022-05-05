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

package bisq.desktop.primary.main.content.trade.overview;

import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.view.TabViewChild;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class TradeOverviewListView extends View<VBox, TradeOverviewModel, TradeOverviewListController> implements TabViewChild {
    private final BisqTableView<ProtocolListItem> tableView;
    private Callback<TableColumn<ProtocolListItem, ProtocolListItem>, TableCell<ProtocolListItem, ProtocolListItem>> cellFactory;

    public TradeOverviewListView(TradeOverviewModel model, TradeOverviewListController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(30);

        tableView = new BisqTableView<>(model.getSortedItems());
        tableView.setMinHeight(200);
        VBox.setMargin(tableView, new Insets(-33, 0, 0, 0));
        configDataTableView();

        this.root.getChildren().addAll(tableView);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    private void configDataTableView() {
        BisqTableColumn<ProtocolListItem> column = new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("trade.protocols.table.header.protocol"))
                .minWidth(80)
                .isFirst()
                .valueSupplier(ProtocolListItem::getProtocolsName)
                .build();
        tableView.getColumns().add(column);
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("trade.protocols.table.header.markets"))
                .minWidth(80)
                .valueSupplier(ProtocolListItem::getMarkets)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("trade.protocols.table.header.security"))
                .minWidth(80)
                .comparator(Comparator.comparing(e -> e.getSwapProtocolType().getSecurity().ordinal()))
                .setCellFactory(getCellFactory(e -> e.getSwapProtocolType().getSecurity().ordinal(), ProtocolListItem::getSecurityInfo))
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("trade.protocols.table.header.privacy"))
                .minWidth(80)
                .comparator(Comparator.comparing(e -> e.getSwapProtocolType().getPrivacy().ordinal()))
                .setCellFactory(getCellFactory(e -> e.getSwapProtocolType().getPrivacy().ordinal(), ProtocolListItem::getPrivacyInfo))
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("trade.protocols.table.header.convenience"))
                .minWidth(80)
                .comparator(Comparator.comparing(e -> e.getSwapProtocolType().getConvenience().ordinal()))
                .setCellFactory(getCellFactory(e -> e.getSwapProtocolType().getConvenience().ordinal(), ProtocolListItem::getConvenienceInfo))
                .build());
    /*    tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("trade.protocols.table.header.costs"))
                .minWidth(80)
                .setCellFactory(getCellFactory(e -> e.getSwapProtocolType().getCost().ordinal(), ProtocolListItem::getCostInfo))
                .build());*/
     /*   tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("trade.protocols.table.header.speed"))
                .minWidth(80)
                .setCellFactory(getCellFactory(e -> e.getSwapProtocolType().getSpeed().ordinal(), ProtocolListItem::getSpeedInfo))
                .build());*/
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("trade.protocols.table.header.release"))
                .minWidth(80)
                .valueSupplier(ProtocolListItem::getReleaseDate)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .fixWidth(150)
                .value(Res.get("shared.select"))
                .cellFactory(BisqTableColumn.DefaultCellFactories.BUTTON)
                .buttonClass(BisqIconButton.class)
                .actionHandler(controller::onSelect)
                .isLast()
                .build());
    }

    public Callback<TableColumn<ProtocolListItem, ProtocolListItem>, TableCell<ProtocolListItem, ProtocolListItem>> getCellFactory(
            Function<ProtocolListItem, Integer> enumOrdinalSupplier,
            Function<ProtocolListItem, String> toolTipSupplier) {
        return new Callback<>() {
            @Override
            public TableCell<ProtocolListItem, ProtocolListItem> call(TableColumn<ProtocolListItem,
                    ProtocolListItem> column) {
                return new TableCell<>() {
                    final HBox hBox = new HBox();
                    final List<Label> stars;
                    final Tooltip tooltip = new Tooltip();

                    {
                        // Does not take default style
                        // Seems table level color setting overrides our default tooltip colors
                        tooltip.setStyle("-fx-text-fill: black; -fx-background-color: -bisq-grey-11;");
                        tooltip.setMaxWidth(300);
                        tooltip.setWrapText(true);

                        String fontSize = "0.9em";
                        stars = List.of(Icons.getIcon(AwesomeIcon.STAR, fontSize),
                                Icons.getIcon(AwesomeIcon.STAR, fontSize),
                                Icons.getIcon(AwesomeIcon.STAR, fontSize));
                        for (Label label : stars) {
                            label.setMouseTransparent(false);
                        }
                        hBox.setSpacing(5);
                        hBox.setAlignment(Pos.CENTER);
                        hBox.getChildren().addAll(stars);
                    }

                    @Override
                    public void updateItem(final ProtocolListItem item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item != null && !empty) {
                            int index = enumOrdinalSupplier.apply(item);
                            for (int i = 0; i < stars.size(); i++) {
                                stars.get(i).setOpacity(i <= index ? 1 : 0.2);
                            }
                            tooltip.setText(toolTipSupplier.apply(item));
                            Tooltip.install(this, tooltip);
                            setGraphic(hBox);
                        } else {
                            setGraphic(null);
                            Tooltip.uninstall(this, tooltip);
                        }
                    }
                };
            }
        };
    }

}
