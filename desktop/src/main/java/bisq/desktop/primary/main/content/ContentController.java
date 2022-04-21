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
import bisq.desktop.primary.main.content.markets.MarketsController;
import bisq.desktop.primary.main.content.settings.SettingsController;
import bisq.desktop.primary.main.content.social.SocialController;
import bisq.desktop.primary.main.content.social.exchange.ExchangeController;
import bisq.desktop.primary.main.content.support.SupportController;
import bisq.desktop.primary.main.content.trade.TradeController;
import bisq.desktop.primary.main.content.trade.bsqSwap.BsqSwapController;
import bisq.desktop.primary.main.content.trade.liquid.LiquidSwapController;
import bisq.desktop.primary.main.content.trade.ln.LightningController;
import bisq.desktop.primary.main.content.trade.multiSig.MultiSigController;
import bisq.desktop.primary.main.content.trade.xmr.XmrSwapController;
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
            case SOCIAL -> {
                return Optional.of(new SocialController(applicationService));
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
            case WALLET -> {
                return Optional.of(new WalletController(applicationService));
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
