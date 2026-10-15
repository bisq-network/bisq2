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

package bisq.desktop.main.content.mu_sig.trade.pending.trade_state;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.desktop.common.view.Model;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.trade.MuSigDisputeState;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.protocol.MuSigTradeState;
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
    private final BooleanProperty isInArbitration = new SimpleBooleanProperty();
    private final BooleanProperty showReportToMediatorButton = new SimpleBooleanProperty();
    private final StringProperty errorMessage = new SimpleStringProperty();
    private final BooleanProperty isTradeCompleted = new SimpleBooleanProperty();
    private final ObjectProperty<MessageDeliveryStatus> requestMediationDeliveryStatus = new SimpleObjectProperty<>();
    private final ObjectProperty<MessageDeliveryStatus> requestArbitrationDeliveryStatus = new SimpleObjectProperty<>();
    private final BooleanProperty shouldShowTryRequestMediationAgain = new SimpleBooleanProperty();
    private final ObjectProperty<MuSigTradeState> tradeState = new SimpleObjectProperty<>(MuSigTradeState.INIT);
    private final ObjectProperty<MuSigDisputeState> disputeState = new SimpleObjectProperty<>(MuSigDisputeState.NO_DISPUTE);
    private final ObjectProperty<MuSigMediationResult> mediationResult = new SimpleObjectProperty<>();
    private final BooleanProperty peerMediationResultRejected = new SimpleBooleanProperty();
    private final BooleanProperty myMediationResultDecisionMade = new SimpleBooleanProperty();
    private final BooleanProperty mediationResultAcceptanceAvailable = new SimpleBooleanProperty();
    private final BooleanProperty showMediationResultDecisionButtons = new SimpleBooleanProperty();
    private final StringProperty mediationBannerText = new SimpleStringProperty();
    private final StringProperty arbitrationBannerText = new SimpleStringProperty();

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
        isInArbitration.set(false);
        showReportToMediatorButton.set(false);
        errorMessage.set(null);
        isTradeCompleted.set(false);
        requestMediationDeliveryStatus.set(null);
        requestArbitrationDeliveryStatus.set(null);
        shouldShowTryRequestMediationAgain.set(false);
        tradeState.set(MuSigTradeState.INIT);
        disputeState.set(MuSigDisputeState.NO_DISPUTE);
        mediationResult.set(null);
        peerMediationResultRejected.set(false);
        myMediationResultDecisionMade.set(false);
        mediationResultAcceptanceAvailable.set(false);
        showMediationResultDecisionButtons.set(false);
        mediationBannerText.set(null);
        arbitrationBannerText.set(null);
    }
}
