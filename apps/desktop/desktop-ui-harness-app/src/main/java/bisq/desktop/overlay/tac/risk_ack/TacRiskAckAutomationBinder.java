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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.overlay.tac.risk_ack;

import bisq.desktop_ui_harness_app.AbstractDesktopAutomationViewBinder;

public final class TacRiskAckAutomationBinder extends AbstractDesktopAutomationViewBinder<TacRiskAckView> {
    @Override
    public Class<TacRiskAckView> viewType() {
        return TacRiskAckView.class;
    }

    @Override
    public void bind(TacRiskAckView view) {
        scope(view.getRoot(), "tac-risk-ack");
        id(view.lossAcknowledgementToggle(), "loss");
        id(view.noRecoveryAcknowledgementToggle(), "no-recovery");
        id(view.nextAction(), "next");
        id(view.rejectAction(), "reject");
        id(view.closeAction(), "close");
    }
}
