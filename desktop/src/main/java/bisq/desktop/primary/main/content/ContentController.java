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
import bisq.desktop.primary.main.content.bsqSwap.BsqSwapController;
import bisq.desktop.primary.main.content.dashboard.DashboardController;
import bisq.desktop.primary.main.content.discussion.DiscussionsController;
import bisq.desktop.primary.main.content.education.EducationController;
import bisq.desktop.primary.main.content.education.bisq.BisqAcademyController;
import bisq.desktop.primary.main.content.education.bitcoin.BitcoinAcademyController;
import bisq.desktop.primary.main.content.education.openSource.OpenSourceAcademyController;
import bisq.desktop.primary.main.content.education.privacy.PrivacyAcademyController;
import bisq.desktop.primary.main.content.education.security.SecurityAcademyController;
import bisq.desktop.primary.main.content.education.wallets.WalletsAcademyController;
import bisq.desktop.primary.main.content.events.EventsController;
import bisq.desktop.primary.main.content.lightning.LightningController;
import bisq.desktop.primary.main.content.liquid.LiquidSwapController;
import bisq.desktop.primary.main.content.markets.MarketsController;
import bisq.desktop.primary.main.content.multiSig.MultiSigController;
import bisq.desktop.primary.main.content.exchange.ExchangeController;
import bisq.desktop.primary.main.content.settings.SettingsController;
import bisq.desktop.primary.main.content.support.SupportController;
import bisq.desktop.primary.main.content.trade.TradeController;
import bisq.desktop.primary.main.content.wallet.BitcoinWalletController;
import bisq.desktop.primary.main.content.wallet.LBtcWalletController;
import bisq.desktop.primary.main.content.xmr.XmrSwapController;
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
            case DASHBOARD -> {
                return Optional.of(new DashboardController(applicationService));
            }
            case DISCUSS -> {
                return Optional.of(new DiscussionsController(applicationService));
            }
            case LEARN -> {
                return Optional.of(new EducationController(applicationService));
            }
            case BISQ_ACADEMY -> {
                return Optional.of(new BisqAcademyController(applicationService));
            }
            case BITCOIN_ACADEMY -> {
                return Optional.of(new BitcoinAcademyController(applicationService));
            }
            case SECURITY_ACADEMY -> {
                return Optional.of(new SecurityAcademyController(applicationService));
            }
            case PRIVACY_ACADEMY -> {
                return Optional.of(new PrivacyAcademyController(applicationService));
            }
            case WALLETS_ACADEMY -> {
                return Optional.of(new WalletsAcademyController(applicationService));
            }
            case OPEN_SOURCE_ACADEMY -> {
                return Optional.of(new OpenSourceAcademyController(applicationService));
            }
            case CONNECT -> {
                return Optional.of(new EventsController(applicationService));
            }
            case TRADE -> {
                return Optional.of(new TradeController(applicationService));
            }
            case SATOSHI_SQUARE -> {
                return Optional.of(new ExchangeController(applicationService));
            }
            case LIQUID_SWAP -> {
                return Optional.of(new LiquidSwapController(applicationService));
            }
            case BISQ_MULTI_SIG -> {
                return Optional.of(new MultiSigController(applicationService));
            }
            case ATOMIC_CROSS_CHAIN_SWAP -> {
                return Optional.of(new XmrSwapController(applicationService));
            }
            case BSQ_SWAP -> {
                return Optional.of(new BsqSwapController(applicationService));
            }
            case LN_3_PARTY -> {
                return Optional.of(new LightningController(applicationService));
            }
            case MARKETS -> {
                return Optional.of(new MarketsController(applicationService));
            }
            case WALLET_BITCOIN -> {
                return Optional.of(new BitcoinWalletController(applicationService));
            }
            case WALLET_LBTC -> {
                return Optional.of(new LBtcWalletController(applicationService));
            }
            case SUPPORT -> {
                return Optional.of(new SupportController(applicationService));
            }
            case SETTINGS -> {
                return Optional.of(new SettingsController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
