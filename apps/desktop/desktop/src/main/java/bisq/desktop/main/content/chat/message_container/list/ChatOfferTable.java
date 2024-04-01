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

package bisq.desktop.main.content.chat.message_container.list;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.BisqEasyOfferMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.monetary.Monetary;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.offer.amount.spec.FixedAmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;

import java.util.Comparator;
import java.util.Optional;

public final class ChatOfferTable {
    private static final PseudoClass OWN_OFFER_PSEUDO_CLASS = PseudoClass.getPseudoClass("own-offer");

    private final ChatMessagesListController controller;
    @Getter
    BisqTableView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> tableView;

    public ChatOfferTable(ChatMessagesListController controller, ChatMessagesListModel model) {
        this.controller = controller;
        FilteredList<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> chatOffers = new FilteredList<>(model.getFilteredChatMessages());
        chatOffers.setPredicate(ChatMessageListItem::hasTradeChatOffer);
        this.tableView = new BisqTableView<>(chatOffers);
        tableView.bindSort();
        configTableView();
    }


    private void configTableView() {
        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableView.getStyleClass().add("bisq-easy-offer-book-table-view");

        tableView.getColumns().add(new BisqTableColumn.Builder<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>()
                .title(Res.get("chat.offerTable.userProfile"))
                .isSortable(false)
                .left()
                .minWidth(150)
                .setCellFactory(getUserProfileCellFactory())
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>()
                .title(Res.get("chat.offerTable.reputation"))
                .isSortable(true)
                .left()
                .minWidth(100)
                .comparator(Comparator.comparing(ChatMessageListItem::getReputationScore))
                .setCellFactory(getReputationCellFactory())
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>()
                .title(Res.get("chat.offerTable.intention"))
                .isSortable(true)
                .left()
                .minWidth(50)
                .comparator(Comparator.comparing(m -> getOffer(m).map(o -> o.getDirection().getDisplayString()).orElse("")))
                .valueSupplier(m -> getOffer(m).map(o -> o.getDirection().getDisplayString().toUpperCase()).orElse(""))
                .build());

//        tableView.getColumns().add(new BisqTableColumn.Builder<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>()
//                .title(Res.get("chat.offer.table.method"))
//                .isSortable(false)
//                .left()
//                .minWidth(100)
//                .comparator(Comparator.comparing(m -> getOffer(m).map(o -> o.getBaseSidePaymentMethodSpecs().toString()).orElse("")))
//                .valueSupplier(m -> getOffer(m).map(o -> o.getBaseSidePaymentMethodSpecs().stream()
//                        .map(spec -> spec.getPaymentMethod().getDisplayString())
//                        .collect(Collectors.joining(", "))).orElse(""))
//                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>()
                .title(Res.get("chat.offerTable.fiatAmount"))
                .isSortable(true)
                .left()
                .minWidth(200)
                .comparator(Comparator.comparing(m -> getOffer(m).map(this::getAmount).orElse("")))
                .valueSupplier(m -> getOffer(m).map(this::getAmount).orElse(""))
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>()
                .title(Res.get("chat.offerTable.price"))
                .isSortable(true)
                .right()
                .minWidth(100)
                .comparator(new PriceComparator())
                .valueSupplier(m -> getOffer(m).map(this::getPriceString).orElse(""))
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>()
                .title(Res.get("chat.offerTable.takeOffer"))
                .isSortable(false)
                .right()
                .minWidth(200)
                .setCellFactory(getTakeOfferCellFactory())
                .build());
    }

    private static Optional<BisqEasyOffer> getOffer(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> messageListItem) {
        return messageListItem.hasTradeChatOffer() ? ((BisqEasyOfferMessage) messageListItem.getChatMessage()).getBisqEasyOffer() : Optional.empty();
    }

    private String getAmount(BisqEasyOffer offer) {
        if (offer.hasAmountRange()) {
            RangeAmountSpec rangedSpec = (RangeAmountSpec) offer.getAmountSpec();
            String minAmount = AmountFormatter.formatAmount(Monetary.from(rangedSpec.getMinAmount(), offer.getMarket().getQuoteCurrencyCode()), true);
            String maxAmount = AmountFormatter.formatAmount(Monetary.from(rangedSpec.getMaxAmount(), offer.getMarket().getQuoteCurrencyCode()), true);
            return minAmount + " - " + maxAmount;
        }

        FixedAmountSpec fixSpec = (FixedAmountSpec) offer.getAmountSpec();
        return AmountFormatter.formatAmount(Monetary.from(fixSpec.getAmount(), offer.getMarket().getQuoteCurrencyCode()), true);
    }

    private static class PriceComparator implements Comparator<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> {
        @Override
        public int compare(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> msg1,
                           ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> msg2) {
            Optional<BisqEasyOffer> offer1 = getOffer(msg1);
            Optional<BisqEasyOffer> offer2 = getOffer(msg2);
            if (offer1.isEmpty() || offer2.isEmpty()) {
                return 0;
            }

            if (offer1.get().getDirection().isBuy()) {
                return -1;
            }

            if (offer2.get().getDirection().isBuy()) {
                return 1;
            }

            PriceSpec spec1 = offer1.get().getPriceSpec();
            PriceSpec spec2 = offer2.get().getPriceSpec();
            if (spec1 instanceof FixPriceSpec) {
                if (spec2 instanceof FixPriceSpec) {
                    return ((FixPriceSpec) spec1).getPriceQuote().compareTo(((FixPriceSpec) spec2).getPriceQuote());
                }
                return 1;
            } else if (spec1 instanceof FloatPriceSpec) {
                if (spec2 instanceof FixPriceSpec) {
                    return -1;
                } else if (spec2 instanceof FloatPriceSpec) {
                    return Double.compare(((FloatPriceSpec) spec1).getPercentage(), ((FloatPriceSpec) spec2).getPercentage());
                } else if (spec2 instanceof MarketPriceSpec) {
                    return Double.compare(((FloatPriceSpec) spec1).getPercentage(), 0d);
                }
                return 1;
            } else if (spec1 instanceof MarketPriceSpec) {
                if (spec2 instanceof FixPriceSpec) {
                    return -1;
                } else if (spec2 instanceof FloatPriceSpec) {
                    return Double.compare(0d, ((FloatPriceSpec) spec2).getPercentage());
                } else if (spec2 instanceof MarketPriceSpec) {
                    return 0;
                }
                return -1;
            }
            return 0;
        }
    }

    private String getPriceString(BisqEasyOffer offer) {
        if (offer.getDirection().isBuy()) {
            return "";
        }
        if (offer.getPriceSpec() instanceof FixPriceSpec) {
            FixPriceSpec fixPriceSpec = (FixPriceSpec) offer.getPriceSpec();
            return PriceFormatter.formatWithCode(fixPriceSpec.getPriceQuote());
        } else if (offer.getPriceSpec() instanceof FloatPriceSpec) {
            FloatPriceSpec floatPriceSpec = (FloatPriceSpec) offer.getPriceSpec();
            return PercentageFormatter.formatToPercentWithSymbol(floatPriceSpec.getPercentage());
        } else {
            return PercentageFormatter.formatToPercentWithSymbol(0d);
        }
    }

    private Callback<TableColumn<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>,
            ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>,
            TableCell<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>,
                    ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>> getUserProfileCellFactory() {
        return column -> new TableCell<>() {
            private final Label userName = new Label();
            private final UserProfileIcon userProfileIcon = new UserProfileIcon(30);
            private final HBox hBox = new HBox(10, userProfileIcon, userName);

            {
                userName.setId("chat-user-name");
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            public void updateItem(final ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    item.getSenderUserProfile().ifPresent(author -> {
                        userName.setText(author.getUserName());
                        userProfileIcon.setUserProfile(author);
                    });
                    setGraphic(hBox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>,
            ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>,
            TableCell<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>,
                    ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>> getReputationCellFactory() {
        return column -> new TableCell<>() {
            private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();
            private final HBox hBox = new HBox(reputationScoreDisplay);

            {
                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.getStyleClass().add("reputation");
            }

            @Override
            public void updateItem(final ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    reputationScoreDisplay.setReputationScore(item.getReputationScore());
                    setGraphic(hBox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>,
            ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>,
            TableCell<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>,
                    ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>> getTakeOfferCellFactory() {
        return column -> new TableCell<>() {
            private final Button takeOfferButton = new Button(Res.get("offer.takeOffer"));

            {
                takeOfferButton.setAlignment(Pos.CENTER_RIGHT);
                takeOfferButton.getStyleClass().add("take-offer-button");
            }

            @Override
            public void updateItem(final ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty && item.getChatMessage() instanceof BisqEasyOfferbookMessage) {
                    if (item.isMyMessage()) {
                        getTableRow().pseudoClassStateChanged(OWN_OFFER_PSEUDO_CLASS, true);
                        takeOfferButton.setOnAction(null);
                        setGraphic(null);
                        return;
                    }
                    getTableRow().pseudoClassStateChanged(OWN_OFFER_PSEUDO_CLASS, false);
                    BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) item.getChatMessage();
                    takeOfferButton.setOnAction(e -> controller.onTakeOffer(bisqEasyOfferbookMessage));
                    takeOfferButton.setDefaultButton(!item.isOfferAlreadyTaken());
                    setGraphic(takeOfferButton);
                } else {
                    takeOfferButton.setOnAction(null);
                    setGraphic(null);
                }
            }
        };
    }
}
