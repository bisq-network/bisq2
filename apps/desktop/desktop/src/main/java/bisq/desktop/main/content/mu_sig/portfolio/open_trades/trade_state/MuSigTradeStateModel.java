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

package bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.desktop.common.view.Model;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.trade.mu_sig.MuSigTrade;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class MuSigTradeStateModel implements Model {
    private final ObjectProperty<MuSigOpenTradeChannel> channel = new SimpleObjectProperty<>();
    private final ObjectProperty<MuSigTrade> trade = new SimpleObjectProperty<>();
    private final ObjectProperty<VBox> stateInfoVBox = new SimpleObjectProperty<>();
    private final BooleanProperty error = new SimpleBooleanProperty();
    private final BooleanProperty phaseAndInfoVisible = new SimpleBooleanProperty();
    private final BooleanProperty isInMediation = new SimpleBooleanProperty();
    private final BooleanProperty showReportToMediatorButton = new SimpleBooleanProperty();
    private final StringProperty errorMessage = new SimpleStringProperty();
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
        trade.set(null);
        stateInfoVBox.set(null);
        error.set(false);
        phaseAndInfoVisible.set(false);
        isInMediation.set(false);
        showReportToMediatorButton.set(false);
        errorMessage.set(null);
        buyerPriceDescriptionApprovalOverlay.set(null);
        sellerPriceDescriptionApprovalOverlay.set(null);
        isTradeCompleted.set(false);
        requestMediationDeliveryStatus.set(null);
        shouldShowTryRequestMediationAgain.set(false);
    }
}
