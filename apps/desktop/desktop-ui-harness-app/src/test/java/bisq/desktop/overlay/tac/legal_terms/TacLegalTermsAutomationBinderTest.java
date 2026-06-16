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

package bisq.desktop.overlay.tac.legal_terms;

import bisq.desktop_ui_harness_app.DesktopAutomationBinderTestSupport;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class TacLegalTermsAutomationBinderTest extends DesktopAutomationBinderTestSupport {
    @Test
    void bindsLegalTermsSelectorsOutsideProductionView() {
        TacLegalTermsView view = new TacLegalTermsView(new TacLegalTermsModel(), mock(TacLegalTermsController.class));

        assertNoScope(view.getRoot());
        assertNoId(view.confirmationToggle());
        assertNoId(view.acceptAction());
        assertNoId(view.rejectAction());
        assertNoId(view.backAction());
        assertNoId(view.closeAction());

        new TacLegalTermsAutomationBinder().bind(view);

        assertScope(view.getRoot(), "tac-legal-terms");
        assertId(view.confirmationToggle(), "confirm");
        assertId(view.acceptAction(), "accept");
        assertId(view.rejectAction(), "reject");
        assertId(view.backAction(), "back");
        assertId(view.closeAction(), "close");
    }
}
