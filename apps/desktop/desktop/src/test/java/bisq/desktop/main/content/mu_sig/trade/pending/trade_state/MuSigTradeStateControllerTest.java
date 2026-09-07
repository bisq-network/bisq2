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

import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.MediationResultReason;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.trade.MuSigDisputeState;
import bisq.trade.mu_sig.protocol.MuSigTradeState;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MuSigTradeStateControllerTest {
    @Test
    void acceptanceStartsUnavailable() {
        MuSigTradeStateModel model = new MuSigTradeStateModel();

        MuSigTradeStateController.updateMediationResultControls(model);

        assertThat(model.getMediationResultAcceptanceAvailable().get()).isFalse();
        assertThat(model.getShowMediationResultDecisionButtons().get()).isFalse();
    }

    @Test
    void acceptanceIsAvailableOnlyInTheDepositConfirmedSigningWindow() {
        MuSigTradeStateModel model = createEligibleModel();
        Set<MuSigTradeState> signingStates = Set.of(MuSigTradeState.DEPOSIT_TX_CONFIRMED,
                MuSigTradeState.BUYER_INITIATED_PAYMENT,
                MuSigTradeState.SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE);

        for (MuSigTradeState state : MuSigTradeState.values()) {
            model.getTradeState().set(state);
            MuSigTradeStateController.updateMediationResultControls(model);

            assertThat(model.getMediationResultAcceptanceAvailable().get())
                    .as("Acceptance availability in %s", state)
                    .isEqualTo(signingStates.contains(state));
        }
    }

    @Test
    void acceptanceRequiresClosedMediation() {
        MuSigTradeStateModel model = createEligibleModel();

        for (MuSigDisputeState state : MuSigDisputeState.values()) {
            model.getDisputeState().set(state);
            MuSigTradeStateController.updateMediationResultControls(model);

            assertThat(model.getMediationResultAcceptanceAvailable().get())
                    .as("Acceptance availability in %s", state)
                    .isEqualTo(state == MuSigDisputeState.MEDIATION_CLOSED);
        }
    }

    @Test
    void acceptanceRequiresAResultOtherThanNoPayout() {
        MuSigTradeStateModel model = createEligibleModel();
        model.getMediationResult().set(null);
        MuSigTradeStateController.updateMediationResultControls(model);

        assertThat(model.getMediationResultAcceptanceAvailable().get()).isFalse();

        model.getMediationResult().set(createResult(MediationPayoutDistributionType.NO_PAYOUT));
        MuSigTradeStateController.updateMediationResultControls(model);

        assertThat(model.getMediationResultAcceptanceAvailable().get()).isFalse();

        model.getMediationResult().set(createResult(MediationPayoutDistributionType.CUSTOM_PAYOUT));
        MuSigTradeStateController.updateMediationResultControls(model);

        assertThat(model.getMediationResultAcceptanceAvailable().get()).isTrue();
    }

    @Test
    void localDecisionPreventsAcceptance() {
        MuSigTradeStateModel model = createEligibleModel();
        model.getMyMediationResultDecisionMade().set(true);

        MuSigTradeStateController.updateMediationResultControls(model);

        assertThat(model.getMediationResultAcceptanceAvailable().get()).isFalse();
    }

    @Test
    void peerRejectionPreventsAcceptance() {
        MuSigTradeStateModel model = createEligibleModel();
        model.getPeerMediationResultRejected().set(true);

        MuSigTradeStateController.updateMediationResultControls(model);

        assertThat(model.getMediationResultAcceptanceAvailable().get()).isFalse();
    }

    private static MuSigTradeStateModel createEligibleModel() {
        MuSigTradeStateModel model = new MuSigTradeStateModel();
        model.getTradeState().set(MuSigTradeState.DEPOSIT_TX_CONFIRMED);
        model.getDisputeState().set(MuSigDisputeState.MEDIATION_CLOSED);
        model.getMediationResult().set(createResult(MediationPayoutDistributionType.CUSTOM_PAYOUT));
        MuSigTradeStateController.updateMediationResultControls(model);
        assertThat(model.getMediationResultAcceptanceAvailable().get()).isTrue();
        return model;
    }

    private static MuSigMediationResult createResult(MediationPayoutDistributionType distributionType) {
        boolean noPayout = distributionType == MediationPayoutDistributionType.NO_PAYOUT;
        return new MuSigMediationResult(HexFormat.of().parseHex("ab".repeat(20)), MediationResultReason.OTHER,
                distributionType, noPayout ? Optional.empty() : Optional.of(90_000L),
                noPayout ? Optional.empty() : Optional.of(60_000L), Optional.empty(), Optional.empty());
    }
}
