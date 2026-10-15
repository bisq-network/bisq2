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

package bisq.desktop.overlay.tac.legal_terms;

import bisq.desktop.common.view.Controller;
import bisq.desktop.overlay.OverlayController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TacLegalTermsController implements Controller {
    private final TacLegalTermsModel model;
    @Getter
    private final TacLegalTermsView view;
    private final Runnable acceptHandler;
    private final Runnable rejectHandler;
    private final Runnable closeHandler;
    private final Runnable backHandler;
    private final OverlayController overlayController;

    public TacLegalTermsController(Runnable acceptHandler, Runnable rejectHandler, Runnable closeHandler, Runnable backHandler) {
        this.acceptHandler = acceptHandler;
        this.rejectHandler = rejectHandler;
        this.closeHandler = closeHandler;
        this.backHandler = backHandler;
        overlayController = OverlayController.getInstance();
        model = new TacLegalTermsModel();
        view = new TacLegalTermsView(model, this);
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
        model.getConfirmed().set(confirmed);
        updateEnterKeyHandler();
    }

    public void setReadOnly(boolean readOnly) {
        model.getReadOnly().set(readOnly);
        updateEnterKeyHandler();
    }

    void onAccept() {
        if (!model.getReadOnly().get() && model.getConfirmed().get()) {
            acceptHandler.run();
        }
    }

    void onReject() {
        rejectHandler.run();
    }

    void onClose() {
        closeHandler.run();
    }

    void onBack() {
        backHandler.run();
    }

    private void updateEnterKeyHandler() {
        overlayController.setEnterKeyHandler(model.getReadOnly().get()
                ? this::onClose
                : model.getConfirmed().get() ? this::onAccept : null);
    }
}
