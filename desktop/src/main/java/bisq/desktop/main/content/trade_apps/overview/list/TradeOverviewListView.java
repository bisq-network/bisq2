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

package bisq.desktop.main.content.trade_apps.overview.list;

import bisq.desktop.common.Icons;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.trade_apps.overview.ProtocolListItem;
import bisq.desktop.main.content.trade_apps.overview.TradeOverviewModel;
import bisq.desktop.main.content.trade_apps.overview.TradeOverviewView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class TradeOverviewListView extends TradeOverviewView<VBox, TradeOverviewModel, TradeOverviewListController> {
    private final BisqTableView<ProtocolListItem> tableView;

    public TradeOverviewListView(TradeOverviewModel model, TradeOverviewListController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(30);

        tableView = new BisqTableView<>(model.getMainProtocols());
        tableView.getStyleClass().add("trade-overview-table-view");
        tableView.setMinHeight(700);
        configDataTableView();
        root.getChildren().add(tableView);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    private void configDataTableView() {
        BisqTableColumn<ProtocolListItem> column = new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("tradeApps.protocol"))
                .minWidth(180)
                .left()
                .comparator(Comparator.comparing(ProtocolListItem::getProtocolsName))
                .setCellFactory(getNameCellFactory())
                .build();
        tableView.getColumns().add(column);
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("tradeApps.markets"))
                .minWidth(80)
                .left()
                .valueSupplier(ProtocolListItem::getMarkets)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("tradeApps.security"))
                .minWidth(80)
                .left()
                .comparator(Comparator.comparing(e -> e.getTradeAppsAttributesType().getSecurity().ordinal()))
                .setCellFactory(getCellFactory(e -> e.getTradeAppsAttributesType().getSecurity().ordinal(), ProtocolListItem::getSecurityInfo))
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("tradeApps.privacy"))
                .minWidth(80)
                .left()
                .comparator(Comparator.comparing(e -> e.getTradeAppsAttributesType().getPrivacy().ordinal()))
                .setCellFactory(getCellFactory(e -> e.getTradeAppsAttributesType().getPrivacy().ordinal(), ProtocolListItem::getPrivacyInfo))
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("tradeApps.convenience"))
                .minWidth(80)
                .left()
                .comparator(Comparator.comparing(e -> e.getTradeAppsAttributesType().getConvenience().ordinal()))
                .setCellFactory(getCellFactory(e -> e.getTradeAppsAttributesType().getConvenience().ordinal(), ProtocolListItem::getConvenienceInfo))
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title(Res.get("tradeApps.release"))
                .minWidth(80)
                .left()
                .valueSupplier(ProtocolListItem::getReleaseDate)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .fixWidth(150)
                .value(Res.get("tradeApps.select"))
                .defaultCellFactory(BisqTableColumn.DefaultCellFactory.BUTTON)
                .actionHandler(controller::onSelect)
                .build());
    }

    private Callback<TableColumn<ProtocolListItem, ProtocolListItem>, TableCell<ProtocolListItem, ProtocolListItem>> getNameCellFactory() {
        return column -> new TableCell<>() {
            final Label label = new Label();

            {
                label.setGraphicTextGap(10);
                label.getStyleClass().add("bisq-text-5");
            }

            @Override
            public void updateItem(final ProtocolListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    label.setGraphic(ImageUtil.getImageViewById(item.getIconId()));
                    label.setText(item.getProtocolsName());
                    setGraphic(label);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ProtocolListItem, ProtocolListItem>, TableCell<ProtocolListItem, ProtocolListItem>> getCellFactory(
            Function<ProtocolListItem, Integer> enumOrdinalSupplier,
            Function<ProtocolListItem, String> toolTipSupplier) {
        return new Callback<>() {
            @Override
            public TableCell<ProtocolListItem, ProtocolListItem> call(TableColumn<ProtocolListItem,
                    ProtocolListItem> column) {
                return new TableCell<>() {
                    final HBox hBox = new HBox();
                    final List<Label> stars = new ArrayList<>();
                    final Tooltip tooltip = new BisqTooltip();

                    {
                        // Does not take default style
                        // Seems table level color setting overrides our default tooltip colors
                        tooltip.setStyle("-fx-text-fill: black; -fx-background-color: -bisq-grey-11;");
                        tooltip.setMaxWidth(300);
                        tooltip.setWrapText(true);

                        for (int i = 0; i < 3; i++) {
                            Label label = Icons.getIcon(AwesomeIcon.STAR, "1.2em");
                            label.setMouseTransparent(false);
                            stars.add(label);
                        }

                        hBox.setSpacing(5);
                        hBox.setAlignment(Pos.CENTER_LEFT);
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
