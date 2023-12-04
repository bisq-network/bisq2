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

import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.data.Triple;
import bisq.common.observable.Pin;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Layout;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.table.*;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.support.mediation.MediationCase;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MediatorView extends View<ScrollPane, MediatorModel, MediatorController> {
    private final Pane chatMessagesComponent;
    private final VBox centerVBox = new VBox();
    private final Switch showClosedCasesSwitch;
    private final VBox chatVBox;
    private final BisqTableView<ListItem> tableView;
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

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("authorizedRole.mediator.table.maker"))
                .minWidth(120)
                .left()
                .comparator(Comparator.comparing(item -> item.getMaker().getUserName()))
                .setCellFactory(getMakerCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .minWidth(95)
                .comparator(Comparator.comparing(ListItem::getDirection))
                .setCellFactory(getDirectionCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("authorizedRole.mediator.table.taker"))
                .minWidth(120)
                .left()
                .comparator(Comparator.comparing(item -> item.getTaker().getUserName()))
                .setCellFactory(getTakerCellFactory())
                .build());

        tableView.getColumns().add(BisqTableColumns.getDateColumn(tableView.getSortOrder()));

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.tradeId"))
                .minWidth(85)
                .comparator(Comparator.comparing(ListItem::getTradeId))
                .valueSupplier(ListItem::getShortTradeId)
                .tooltipSupplier(ListItem::getTradeId)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.quoteAmount"))
                .fixWidth(95)
                .comparator(Comparator.comparing(ListItem::getQuoteAmount))
                .valueSupplier(ListItem::getQuoteAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.baseAmount"))
                .fixWidth(120)
                .comparator(Comparator.comparing(ListItem::getBaseAmount))
                .valueSupplier(ListItem::getBaseAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.price"))
                .fixWidth(135)
                .comparator(Comparator.comparing(ListItem::getPrice))
                .valueSupplier(ListItem::getPriceString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.paymentMethod"))
                .minWidth(130)
                .right()
                .comparator(Comparator.comparing(ListItem::getPaymentMethod))
                .valueSupplier(ListItem::getPaymentMethod)
                .build());
    }

    private Callback<TableColumn<ListItem, ListItem>,
            TableCell<ListItem, ListItem>> getDirectionCellFactory() {
        return column -> new TableCell<>() {

            private final Label label = new Label();

            @Override
            public void updateItem(final ListItem item, boolean empty) {
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

    private Callback<TableColumn<ListItem, ListItem>,
            TableCell<ListItem, ListItem>> getMakerCellFactory() {
        return column -> new TableCell<>() {

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    applyTraderToTableCell(this, item, item.isMakerRequester(), item.getMaker());
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ListItem, ListItem>,
            TableCell<ListItem, ListItem>> getTakerCellFactory() {
        return column -> new TableCell<>() {

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    applyTraderToTableCell(this, item, !item.isMakerRequester(), item.getTaker());
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private static void applyTraderToTableCell(TableCell<ListItem, ListItem> tableCell,
                                               ListItem item,
                                               boolean isRequester,
                                               ListItem.Trader trader) {
        UserProfileDisplay userProfileDisplay = new UserProfileDisplay(trader.getUserProfile());
        userProfileDisplay.setReputationScore(trader.getReputationScore());
        if (isRequester) {
            userProfileDisplay.getStyleClass().add("mediator-table-requester");
        }
        userProfileDisplay.getTooltip().setText(Res.get("authorizedRole.mediator.hasRequested",
                userProfileDisplay.getTooltipText(),
                isRequester ? Res.get("confirmation.yes") : Res.get("confirmation.no")
        ));
        Badge badge = trader.equals(item.getMaker()) ? item.getMakersBadge() : item.getTakersBadge();
        badge.setControl(userProfileDisplay);
        badge.getStyleClass().add("open-trades-badge");
        badge.setPosition(Pos.BOTTOM_LEFT);
        badge.setBadgeInsets(new Insets(0, 0, 7.5, 20));
        // Label color does not get applied from badge style when in a list cell even we use '!important' in the css.
        badge.getLabel().setStyle("-fx-text-fill: black !important;");
        tableCell.setGraphic(badge);
    }

    @Slf4j
    @Getter
    @ToString
    @EqualsAndHashCode
    static class ListItem implements ActivatableTableItem, DateTableItem {
        private final ChatNotificationService chatNotificationService;
        private final ReputationService reputationService;

        private final BisqEasyOpenTradeChannel channel;
        private final MediationCase mediationCase;
        private final Trader maker, taker;
        private final long date, price, baseAmount, quoteAmount;
        private final String dateString, timeString, tradeId, shortTradeId, offerId, direction, market,
                priceString, baseAmountString, quoteAmountString, paymentMethod;
        private final boolean isMakerRequester;
        private final Badge makersBadge = new Badge();
        private final Badge takersBadge = new Badge();
        private Pin changedChatNotificationPin;

        ListItem(ServiceProvider serviceProvider,
                 MediationCase mediationCase,
                 BisqEasyOpenTradeChannel channel) {
            reputationService = serviceProvider.getUserService().getReputationService();
            chatNotificationService = serviceProvider.getChatService().getChatNotificationService();

            this.channel = channel;
            this.mediationCase = mediationCase;
            BisqEasyContract contract = mediationCase.getMediationRequest().getContract();
            BisqEasyOffer offer = contract.getOffer();
            List<UserProfile> traders = new ArrayList<>(channel.getTraders());
            offer.getMakerNetworkId().getId();

            Trader trader1 = new Trader(traders.get(0), reputationService);
            Trader trader2 = new Trader(traders.get(1), reputationService);
            if (offer.getMakerNetworkId().getId().equals(trader1.getUserProfile().getId())) {
                maker = trader1;
                taker = trader2;
            } else {
                maker = trader2;
                taker = trader1;
            }
            isMakerRequester = mediationCase.getMediationRequest().getRequester().equals(maker.userProfile);

            tradeId = channel.getTradeId();
            shortTradeId = tradeId.substring(0, 8);
            offerId = offer.getId();
            direction = BisqEasyTradeFormatter.getDirection(offer.getDirection());
            date = contract.getTakeOfferDate();
            dateString = DateFormatter.formatDate(date);
            timeString = DateFormatter.formatTime(date);
            market = offer.getMarket().toString();
            price = BisqEasyTradeUtils.getPriceQuote(contract).getValue();
            priceString = BisqEasyTradeFormatter.formatPriceWithCode(contract);
            baseAmount = contract.getBaseSideAmount();
            baseAmountString = BisqEasyTradeFormatter.formatBaseSideAmount(contract);
            quoteAmount = contract.getQuoteSideAmount();
            quoteAmountString = BisqEasyTradeFormatter.formatQuoteSideAmountWithCode(contract);
            paymentMethod = contract.getQuoteSidePaymentMethodSpec().getShortDisplayString();
        }

        @Override
        public void onActivate() {
            changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(notification -> {
                if (notification == null) {
                    return;
                }
                UIThread.run(() -> {
                    long numNotificationsFromMaker = getNumNotifications(maker.getUserProfile());
                    makersBadge.setText(numNotificationsFromMaker > 0 ?
                            String.valueOf(numNotificationsFromMaker) :
                            "");
                    long numNotificationsFromTaker = getNumNotifications(taker.getUserProfile());
                    takersBadge.setText(numNotificationsFromTaker > 0 ?
                            String.valueOf(numNotificationsFromTaker) :
                            "");
                });
            });
        }

        @Override
        public void onDeactivate() {
            changedChatNotificationPin.unbind();
            makersBadge.dispose();
        }

        private long getNumNotifications(UserProfile userProfile) {
            return chatNotificationService.getNotConsumedNotifications(channel.getId())
                    .filter(n -> n.getSenderUserProfile().isPresent())
                    .filter(n -> n.getSenderUserProfile().get().equals(userProfile))
                    .count();
        }

        @Getter
        @EqualsAndHashCode
        static class Trader {
            private final String userName;
            private final UserProfile userProfile;
            private final String totalReputationScoreString;
            private final String profileAgeString;
            private final ReputationScore reputationScore;
            private final long totalReputationScore, profileAge;

            Trader(UserProfile userProfile, ReputationService reputationService) {
                this.userProfile = userProfile;
                userName = userProfile.getUserName();

                reputationScore = reputationService.getReputationScore(userProfile);
                totalReputationScore = reputationScore.getTotalScore();
                totalReputationScoreString = String.valueOf(reputationScore);

                Optional<Long> optionalProfileAge = reputationService.getProfileAgeService().getProfileAge(userProfile);
                profileAge = optionalProfileAge.orElse(0L);
                profileAgeString = optionalProfileAge
                        .map(TimeFormatter::formatAgeInDays)
                        .orElse(Res.get("data.na"));
            }
        }
    }
}
