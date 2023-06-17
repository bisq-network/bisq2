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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_state;

import bisq.account.accounts.Account;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.common.view.Model;
import bisq.offer.bisq_easy.BisqEasyOffer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class TradeStateModel implements Model {
    public enum Phase {
        BUYER_PHASE_1,
        SELLER_PHASE_1,
        BUYER_PHASE_2,
        SELLER_PHASE_2,
        BUYER_PHASE_3,
        SELLER_PHASE_3,
        BUYER_PHASE_4,
        SELLER_PHASE_4,
        BUYER_PHASE_5,
        SELLER_PHASE_5
    }

    @Setter
    private BisqEasyPrivateTradeChatChannel selectedChannel;
    @Setter
    private BisqEasyOffer bisqEasyOffer;
    private final StringProperty quoteCode = new SimpleStringProperty();
    private final StringProperty quoteAmount = new SimpleStringProperty();
    private final StringProperty baseAmount = new SimpleStringProperty();
    private final StringProperty buyersBtcAddress = new SimpleStringProperty();
    private final StringProperty sellersPaymentAccountData = new SimpleStringProperty();
    private final BooleanProperty isCollapsed = new SimpleBooleanProperty();
    private final StringProperty tradeInfo = new SimpleStringProperty();
    private final StringProperty phaseInfo = new SimpleStringProperty();
    private final StringProperty phase1Info = new SimpleStringProperty();
    private final StringProperty phase2Info = new SimpleStringProperty();
    private final StringProperty phase3Info = new SimpleStringProperty();
    private final StringProperty phase4Info = new SimpleStringProperty();
    private final StringProperty phase5Info = new SimpleStringProperty();
    private final StringProperty actionButtonText = new SimpleStringProperty();
    private final BooleanProperty actionButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty openDisputeButtonVisible = new SimpleBooleanProperty();
    private final IntegerProperty phaseIndex = new SimpleIntegerProperty();
    private final ObservableList<Account<?, ?>> paymentAccounts = FXCollections.observableArrayList();
    private final ObjectProperty<Account<?, ?>> selectedAccount = new SimpleObjectProperty<>();
    private final ObjectProperty<Phase> phase = new SimpleObjectProperty<>();
}
