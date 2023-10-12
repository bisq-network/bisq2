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

package bisq.desktop.main.content.bisq_easy.wallet_guide;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.bisq_easy.wallet_guide.create_wallet.WalletGuideCreateWalletController;
import bisq.desktop.main.content.bisq_easy.wallet_guide.download.WalletGuideDownloadController;
import bisq.desktop.main.content.bisq_easy.wallet_guide.intro.WalletGuideIntroController;
import bisq.desktop.main.content.bisq_easy.wallet_guide.receive.WalletGuideReceiveController;
import bisq.desktop.overlay.OverlayController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class WalletGuideController extends TabController<WalletGuideModel> {
    @Getter
    private final WalletGuideView view;
    private final ServiceProvider serviceProvider;

    public WalletGuideController(ServiceProvider serviceProvider) {
        super(new WalletGuideModel(), NavigationTarget.WALLET_GUIDE);

        this.serviceProvider = serviceProvider;
        view = new WalletGuideView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case WALLET_GUIDE_INTRO: {
                return Optional.of(new WalletGuideIntroController(serviceProvider));
            }
            case WALLET_GUIDE_DOWNLOAD: {
                return Optional.of(new WalletGuideDownloadController(serviceProvider));
            }
            case WALLET_GUIDE_CREATE_WALLET: {
                return Optional.of(new WalletGuideCreateWalletController(serviceProvider));
            }
            case WALLET_GUIDE_RECEIVE: {
                return Optional.of(new WalletGuideReceiveController(serviceProvider));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onClose() {
        OverlayController.hide();
    }

    public void onQuit() {
        serviceProvider.getShutDownHandler().shutdown();
    }
}
