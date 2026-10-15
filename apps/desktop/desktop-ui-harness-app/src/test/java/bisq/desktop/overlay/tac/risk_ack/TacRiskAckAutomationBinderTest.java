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

import bisq.desktop_ui_harness_app.DesktopAutomationBinderTestSupport;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class TacRiskAckAutomationBinderTest extends DesktopAutomationBinderTestSupport {
    @Test
    void bindsRiskAckSelectorsOutsideProductionView() {
        TacRiskAckView view = new TacRiskAckView(new TacRiskAckModel(), mock(TacRiskAckController.class));

        assertNoScope(view.getRoot());
        assertNoId(view.lossAcknowledgementToggle());
        assertNoId(view.noRecoveryAcknowledgementToggle());
        assertNoId(view.nextAction());
        assertNoId(view.rejectAction());
        assertNoId(view.closeAction());

        new TacRiskAckAutomationBinder().bind(view);

        assertScope(view.getRoot(), "tac-risk-ack");
        assertId(view.lossAcknowledgementToggle(), "loss");
        assertId(view.noRecoveryAcknowledgementToggle(), "no-recovery");
        assertId(view.nextAction(), "next");
        assertId(view.rejectAction(), "reject");
        assertId(view.closeAction(), "close");
    }
}
