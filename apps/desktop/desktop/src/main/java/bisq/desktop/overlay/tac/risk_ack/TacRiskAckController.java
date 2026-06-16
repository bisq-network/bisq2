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

package bisq.desktop.overlay.tac.risk_ack;

import bisq.desktop.common.view.Controller;
import bisq.desktop.overlay.OverlayController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TacRiskAckController implements Controller {
    private final TacRiskAckModel model;
    @Getter
    private final TacRiskAckView view;
    private final Runnable nextHandler;
    private final Runnable rejectHandler;
    private final Runnable closeHandler;
    private final OverlayController overlayController;

    public TacRiskAckController(Runnable nextHandler, Runnable rejectHandler, Runnable closeHandler) {
        this.nextHandler = nextHandler;
        this.rejectHandler = rejectHandler;
        this.closeHandler = closeHandler;
        overlayController = OverlayController.getInstance();
        model = new TacRiskAckModel();
        view = new TacRiskAckView(model, this);
    }

    @Override
    public void onActivate() {
        updateEnterKeyHandler();
    }

    @Override
    public void onDeactivate() {
        overlayController.setEnterKeyHandler(null);
    }

    public void setConfirmed(boolean confirmed) {
        model.getLossAcknowledged().set(confirmed);
        model.getNoRecoveryAcknowledged().set(confirmed);
        updateEnterKeyHandler();
    }

    public void setReadOnly(boolean readOnly) {
        model.getReadOnly().set(readOnly);
        updateEnterKeyHandler();
    }

    void onLossAcknowledged(boolean selected) {
        model.getLossAcknowledged().set(selected);
        updateEnterKeyHandler();
    }

    void onNoRecoveryAcknowledged(boolean selected) {
        model.getNoRecoveryAcknowledged().set(selected);
        updateEnterKeyHandler();
    }

    void onNext() {
        if (model.canContinue()) {
            nextHandler.run();
        }
    }

    void onReject() {
        rejectHandler.run();
    }

    void onClose() {
        closeHandler.run();
    }

    private void updateEnterKeyHandler() {
        overlayController.setEnterKeyHandler(model.canContinue() ? this::onNext : null);
    }
}
