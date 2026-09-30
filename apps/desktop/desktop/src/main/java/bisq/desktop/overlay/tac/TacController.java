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

package bisq.desktop.overlay.tac;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.overlay.tac.legal_terms.TacLegalTermsController;
import bisq.desktop.overlay.tac.risk_ack.TacRiskAckController;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TacController extends NavigationController implements InitWithDataController<TacController.InitData> {
    public enum Mode {
        ACCEPTANCE,
        READ_ONLY
    }

    @Getter
    public static final class InitData {
        private final Mode mode;
        private final Runnable completeHandler;

        public InitData(Runnable completeHandler) {
            this(Mode.ACCEPTANCE, completeHandler);
        }

        public InitData(Mode mode) {
            this(mode, null);
        }

        private InitData(Mode mode, Runnable completeHandler) {
            this.mode = mode;
            this.completeHandler = completeHandler;
        }
    }

    @Getter
    private final TacModel model;
    @Getter
    private final TacView view;
    private final ServiceProvider serviceProvider;
    private final SettingsService settingsService;
    private final OverlayController overlayController;
    private final TacRiskAckController riskAckController;
    private final TacLegalTermsController legalTermsController;
    private Mode mode = Mode.ACCEPTANCE;
    private Runnable completeHandler;

    public TacController(ServiceProvider serviceProvider) {
        super(NavigationTarget.TAC);

        this.serviceProvider = serviceProvider;
        settingsService = serviceProvider.getSettingsService();
        overlayController = OverlayController.getInstance();
        model = new TacModel();
        view = new TacView(model, this);
        riskAckController = new TacRiskAckController(this::onRiskAckCompleted, this::onReject, this::onClose);
        legalTermsController = new TacLegalTermsController(this::onAccept, this::onReject, this::onClose, this::onBack);
    }

    @Override
    public void initWithData(TacController.InitData data) {
        mode = data.getMode();
        completeHandler = data.getCompleteHandler();
    }

    @Override
    public void onActivate() {
        boolean readOnly = mode == Mode.READ_ONLY;
        boolean currentTacAccepted = settingsService.isCurrentTacAccepted();
        riskAckController.setReadOnly(readOnly);
        legalTermsController.setReadOnly(readOnly);
        riskAckController.setConfirmed(currentTacAccepted);
        legalTermsController.setConfirmed(currentTacAccepted);

        overlayController.setEnterKeyHandler(null);
        overlayController.setUseEscapeKeyHandler(readOnly);
    }

    @Override
    public void onDeactivate() {
        overlayController.setEnterKeyHandler(null);
        completeHandler = null;
        mode = Mode.ACCEPTANCE;
        resetSelectedChildTarget();
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case TAC_RISK_ACK -> Optional.of(riskAckController);
            case TAC_LEGAL_TERMS -> Optional.of(legalTermsController);
            default -> Optional.empty();
        };
    }

    void onQuit() {
        serviceProvider.getShutDownHandler().shutdown();
    }

    void onRiskAckCompleted() {
        Navigation.navigateTo(NavigationTarget.TAC_LEGAL_TERMS);
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.TAC_RISK_ACK);
    }

    void onAccept() {
        if (mode == Mode.READ_ONLY) {
            onClose();
            return;
        }

        Runnable handler = completeHandler;
        settingsService.acceptCurrentTac();
        OverlayController.hide(() -> {
            if (handler != null) {
                handler.run();
            }
        });
        completeHandler = null;
    }

    void onReject() {
        onQuit();
    }

    void onClose() {
        OverlayController.hide();
    }
}
