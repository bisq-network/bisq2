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

package bisq.desktop.primary.main.content;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.academy.AcademyOverviewController;
import bisq.desktop.primary.main.content.academy.bisq.BisqAcademyController;
import bisq.desktop.primary.main.content.academy.bitcoin.BitcoinAcademyController;
import bisq.desktop.primary.main.content.academy.foss.FossAcademyController;
import bisq.desktop.primary.main.content.academy.privacy.PrivacyAcademyController;
import bisq.desktop.primary.main.content.academy.security.SecurityAcademyController;
import bisq.desktop.primary.main.content.academy.wallets.WalletsAcademyController;
import bisq.desktop.primary.main.content.dashboard.DashboardController;
import bisq.desktop.primary.main.content.discussion.DiscussionsController;
import bisq.desktop.primary.main.content.events.EventsController;
import bisq.desktop.primary.main.content.settings.SettingsController;
import bisq.desktop.primary.main.content.support.SupportController;
import bisq.desktop.primary.main.content.trade.TradeController;
import bisq.desktop.primary.main.content.trade.bisqEasy.BisqEasyController;
import bisq.desktop.primary.main.content.trade.bsqSwap.BsqSwapController;
import bisq.desktop.primary.main.content.trade.lightning.LightningController;
import bisq.desktop.primary.main.content.trade.liquidSwap.LiquidSwapController;
import bisq.desktop.primary.main.content.trade.multiSig.MultiSigController;
import bisq.desktop.primary.main.content.trade.xmrSwap.XmrSwapController;
import bisq.desktop.primary.main.content.wallet.WalletController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ContentController extends NavigationController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final ContentModel model;
    @Getter
    private final ContentView view;

    public ContentController(DefaultApplicationService applicationService) {
        super(NavigationTarget.CONTENT);

        this.applicationService = applicationService;
        model = new ContentModel();
        view = new ContentView(model, this);
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
            case DASHBOARD: {
                return Optional.of(new DashboardController(applicationService));
            }
            case DISCUSS: {
                return Optional.of(new DiscussionsController(applicationService));
            }
            case ACADEMY_OVERVIEW: {
                return Optional.of(new AcademyOverviewController(applicationService));
            }
            case BISQ_ACADEMY: {
                return Optional.of(new BisqAcademyController(applicationService));
            }
            case BITCOIN_ACADEMY: {
                return Optional.of(new BitcoinAcademyController(applicationService));
            }
            case SECURITY_ACADEMY: {
                return Optional.of(new SecurityAcademyController(applicationService));
            }
            case PRIVACY_ACADEMY: {
                return Optional.of(new PrivacyAcademyController(applicationService));
            }
            case WALLETS_ACADEMY: {
                return Optional.of(new WalletsAcademyController(applicationService));
            }
            case FOSS_ACADEMY: {
                return Optional.of(new FossAcademyController(applicationService));
            }
            case EVENTS: {
                return Optional.of(new EventsController(applicationService));
            }
            case SUPPORT: {
                return Optional.of(new SupportController(applicationService));
            }
            case TRADE_OVERVIEW: {
                return Optional.of(new TradeController(applicationService));
            }
            case BISQ_EASY: {
                return Optional.of(new BisqEasyController(applicationService));
            }
            case LIQUID_SWAP: {
                return Optional.of(new LiquidSwapController(applicationService));
            }
            case BISQ_MULTISIG: {
                return Optional.of(new MultiSigController(applicationService));
            }
            case MONERO_SWAP: {
                return Optional.of(new XmrSwapController(applicationService));
            }
            case BSQ_SWAP: {
                return Optional.of(new BsqSwapController(applicationService));
            }
            case LIGHTNING_X: {
                return Optional.of(new LightningController(applicationService));
            }
            case SETTINGS: {
                return Optional.of(new SettingsController(applicationService));
            }
            case WALLET: {
                return Optional.of(new WalletController(applicationService));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
