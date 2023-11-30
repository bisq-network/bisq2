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

package bisq.desktop.main.content.authorized_role.mediator;

import bisq.common.data.Triple;
import bisq.desktop.common.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableColumns;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class MediatorView extends View<ScrollPane, MediatorModel, MediatorController> {
    private final Pane chatMessagesComponent;
    private final VBox centerVBox = new VBox();
    private final Switch showClosedCasesSwitch;
    private final VBox chatVBox;
    private final BisqTableView<MediationCaseListItem> tableView;
    private Subscription noOpenCasesPin, tableViewSelectionPin, selectedModelItemPin, showClosedCasesPin;

    public MediatorView(MediatorModel model,
                        MediatorController controller,
                        Pane mediationCaseHeader,
                        VBox chatMessagesComponent) {

        super(new ScrollPane(), model, controller);

        this.chatMessagesComponent = chatMessagesComponent;

        tableView = new BisqTableView<>(model.getListItems().getSortedList());
        tableView.getStyleClass().addAll("bisq-easy-open-trades", "hide-horizontal-scrollbar");
        configTableView();

        VBox.setMargin(tableView, new Insets(10, 0, 0, 0));
        Triple<Label, HBox, VBox> triple = BisqEasyViewUtils.getContainer(Res.get("authorizedRole.mediator.table.headline"), tableView);

        HBox header = triple.getSecond();
        showClosedCasesSwitch = new Switch(Res.get("authorizedRole.mediator.showClosedCases"));
        header.getChildren().addAll(Spacer.fillHBox(), showClosedCasesSwitch);

        VBox container = triple.getThird();

        VBox.setMargin(container, new Insets(0, 0, 10, 0));
        centerVBox.getChildren().add(container);

        chatMessagesComponent.setMinHeight(200);
        chatMessagesComponent.setPadding(new Insets(0, -30, -15, -30));

        VBox.setMargin(chatMessagesComponent, new Insets(0, 30, 15, 30));
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        chatVBox = new VBox(mediationCaseHeader, Layout.hLine(), chatMessagesComponent);
        chatVBox.getStyleClass().add("bisq-easy-container");

        VBox.setVgrow(chatVBox, Priority.ALWAYS);
        centerVBox.getChildren().add(chatVBox);

        centerVBox.setPadding(new Insets(0, 40, 0, 40));

        VBox.setVgrow(centerVBox, Priority.ALWAYS);
        root.setContent(centerVBox);

        root.setFitToWidth(true);
        root.setFitToHeight(true);
    }

    @Override
    protected void onViewAttached() {
        selectedModelItemPin = EasyBind.subscribe(model.getSelectedItem(),
                selected -> tableView.getSelectionModel().select(selected));

        tableViewSelectionPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                item -> {
                    if (item != null) {
                        controller.onSelectItem(item);
                    }
                });

        noOpenCasesPin = EasyBind.subscribe(model.getNoOpenCases(), noOpenCases -> {
            if (noOpenCases) {
                tableView.removeListeners();
                tableView.allowVerticalScrollbar();
                tableView.setFixHeight(150);
                tableView.getStyleClass().add("empty-table");
                tableView.setPlaceholderText(model.getShowClosedCases().get() ?
                        Res.get("authorizedRole.mediator.noClosedCases") :
                        Res.get("authorizedRole.mediator.noOpenCases"));
            } else {
                tableView.setPlaceholder(null);
                tableView.adjustHeightToNumRows();
                tableView.hideVerticalScrollbar();
                tableView.getStyleClass().remove("empty-table");
            }
            chatVBox.setVisible(!noOpenCases);
            chatVBox.setManaged(!noOpenCases);
        });

        showClosedCasesPin = EasyBind.subscribe(model.getShowClosedCases(), showClosedCases -> {
            showClosedCasesSwitch.setSelected(showClosedCases);

            tableView.setPlaceholderText(showClosedCases ?
                    Res.get("authorizedRole.mediator.noClosedCases") :
                    Res.get("authorizedRole.mediator.noOpenCases"));
        });
        showClosedCasesSwitch.setOnAction(e -> controller.onToggleClosedCases());
    }

    @Override
    protected void onViewDetached() {
        tableView.removeListeners();

        selectedModelItemPin.unsubscribe();
        tableViewSelectionPin.unsubscribe();
        noOpenCasesPin.unsubscribe();
        showClosedCasesPin.unsubscribe();
        showClosedCasesSwitch.setOnAction(null);
    }

    private void configTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("authorizedRole.mediator.table.maker"))
                .minWidth(120)
                .left()
                .comparator(Comparator.comparing(item -> item.getMaker().getUserName()))
                .setCellFactory(getMakerCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .minWidth(95)
                .comparator(Comparator.comparing(MediationCaseListItem::getDirection))
                .setCellFactory(getDirectionCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("authorizedRole.mediator.table.taker"))
                .minWidth(120)
                .left()
                .comparator(Comparator.comparing(item -> item.getTaker().getUserName()))
                .setCellFactory(getTakerCellFactory())
                .build());

        tableView.getColumns().add(BisqTableColumns.getDateColumn(tableView.getSortOrder()));

        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.tradeId"))
                .minWidth(85)
                .comparator(Comparator.comparing(MediationCaseListItem::getTradeId))
                .valueSupplier(MediationCaseListItem::getShortTradeId)
                .tooltipSupplier(MediationCaseListItem::getTradeId)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.quoteAmount"))
                .fixWidth(95)
                .comparator(Comparator.comparing(MediationCaseListItem::getQuoteAmount))
                .valueSupplier(MediationCaseListItem::getQuoteAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.baseAmount"))
                .fixWidth(120)
                .comparator(Comparator.comparing(MediationCaseListItem::getBaseAmount))
                .valueSupplier(MediationCaseListItem::getBaseAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.price"))
                .fixWidth(135)
                .comparator(Comparator.comparing(MediationCaseListItem::getPrice))
                .valueSupplier(MediationCaseListItem::getPriceString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.paymentMethod"))
                .minWidth(130)
                .right()
                .comparator(Comparator.comparing(MediationCaseListItem::getPaymentMethod))
                .valueSupplier(MediationCaseListItem::getPaymentMethod)
                .build());
    }

    private Callback<TableColumn<MediationCaseListItem, MediationCaseListItem>,
            TableCell<MediationCaseListItem, MediationCaseListItem>> getDirectionCellFactory() {
        return column -> new TableCell<>() {

            private final Label label = new Label();

            @Override
            public void updateItem(final MediationCaseListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    label.setText(item.getDirection());
                    label.setPadding(new Insets(-9, -20, 0, -20));
                    setGraphic(label);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<MediationCaseListItem, MediationCaseListItem>,
            TableCell<MediationCaseListItem, MediationCaseListItem>> getMakerCellFactory() {
        return column -> new TableCell<>() {

            @Override
            public void updateItem(final MediationCaseListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    applyTraderToTableCell(this, item.isMakerRequester(), item.getMaker());
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<MediationCaseListItem, MediationCaseListItem>,
            TableCell<MediationCaseListItem, MediationCaseListItem>> getTakerCellFactory() {
        return column -> new TableCell<>() {

            @Override
            public void updateItem(final MediationCaseListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    applyTraderToTableCell(this, !item.isMakerRequester(), item.getTaker());
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private static void applyTraderToTableCell(TableCell<MediationCaseListItem, MediationCaseListItem> tableCell,
                                               boolean isRequester,
                                               MediationCaseListItem.Trader trader) {
        UserProfileDisplay userProfileDisplay = new UserProfileDisplay(trader.getUserProfile());
        userProfileDisplay.setReputationScore(trader.getReputationScore());
        if (isRequester) {
            userProfileDisplay.getStyleClass().add("mediator-table-requester");
        }
        userProfileDisplay.getTooltip().setText(Res.get("authorizedRole.mediator.hasRequested",
                userProfileDisplay.getTooltipText(),
                isRequester ? Res.get("confirmation.yes") : Res.get("confirmation.no")
        ));
        tableCell.setGraphic(userProfileDisplay);
    }
}
