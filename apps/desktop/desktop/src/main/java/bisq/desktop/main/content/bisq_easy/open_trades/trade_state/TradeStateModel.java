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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.desktop.common.view.Model;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.beans.property.*;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class TradeStateModel implements Model {
    enum TradeCloseType {
        REJECT,
        CANCEL,
        COMPLETED
    }

    @Setter
    private TradeCloseType tradeCloseType;
    private final ObjectProperty<BisqEasyOpenTradeChannel> channel = new SimpleObjectProperty<>();
    private final ObjectProperty<BisqEasyTrade> bisqEasyTrade = new SimpleObjectProperty<>();
    private final ObjectProperty<VBox> stateInfoVBox = new SimpleObjectProperty<>();
    private final StringProperty interruptTradeButtonText = new SimpleStringProperty();
    private final BooleanProperty interruptTradeButtonVisible = new SimpleBooleanProperty(true);
    private final BooleanProperty interruptedTradeInfo = new SimpleBooleanProperty();
    private final BooleanProperty error = new SimpleBooleanProperty();
    private final BooleanProperty phaseAndInfoVisible = new SimpleBooleanProperty();
    private final BooleanProperty isInMediation = new SimpleBooleanProperty();
    private final BooleanProperty showReportToMediatorButton = new SimpleBooleanProperty();
    private final StringProperty tradeInterruptedInfo = new SimpleStringProperty();
    private final StringProperty errorMessage = new SimpleStringProperty();
    private final BooleanProperty shouldShowSellerPriceApprovalOverlay = new SimpleBooleanProperty();
    private final BooleanProperty hasBuyerAcceptedSellersPriceSpec = new SimpleBooleanProperty();
    private final StringProperty buyerPriceDescriptionApprovalOverlay = new SimpleStringProperty();
    private final StringProperty sellerPriceDescriptionApprovalOverlay = new SimpleStringProperty();
    private final BooleanProperty isTradeCompleted = new SimpleBooleanProperty();
    private final ObjectProperty<MessageDeliveryStatus> requestMediationDeliveryStatus = new SimpleObjectProperty<>();
    private final BooleanProperty shouldShowTryRequestMediationAgain = new SimpleBooleanProperty();

    void resetAll() {
        reset();
        channel.set(null);
    }

    void reset() {
        tradeCloseType = null;
        bisqEasyTrade.set(null);
        stateInfoVBox.set(null);
        interruptTradeButtonText.set(null);
        interruptTradeButtonVisible.set(true);
        interruptedTradeInfo.set(false);
        error.set(false);
        phaseAndInfoVisible.set(false);
        isInMediation.set(false);
        showReportToMediatorButton.set(false);
        tradeInterruptedInfo.set(null);
        errorMessage.set(null);
        shouldShowSellerPriceApprovalOverlay.set(false);
        hasBuyerAcceptedSellersPriceSpec.set(false);
        buyerPriceDescriptionApprovalOverlay.set(null);
        sellerPriceDescriptionApprovalOverlay.set(null);
        isTradeCompleted.set(false);
        requestMediationDeliveryStatus.set(null);
        shouldShowTryRequestMediationAgain.set(false);
    }
}
