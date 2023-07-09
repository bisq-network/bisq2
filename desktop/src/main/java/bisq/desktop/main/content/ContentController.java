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

package bisq.desktop.main.content;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.chat.channel.ChatChannelDomain;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.academy.AcademyOverviewController;
import bisq.desktop.main.content.academy.bisq.BisqAcademyController;
import bisq.desktop.main.content.academy.bitcoin.BitcoinAcademyController;
import bisq.desktop.main.content.academy.foss.FossAcademyController;
import bisq.desktop.main.content.academy.privacy.PrivacyAcademyController;
import bisq.desktop.main.content.academy.security.SecurityAcademyController;
import bisq.desktop.main.content.academy.wallets.WalletsAcademyController;
import bisq.desktop.main.content.authorized_role.AuthorizedRoleController;
import bisq.desktop.main.content.common_chat.CommonChatController;
import bisq.desktop.main.content.dashboard.DashboardController;
import bisq.desktop.main.content.settings.SettingsController;
import bisq.desktop.main.content.trade_apps.ProtocolRoadmapController;
import bisq.desktop.main.content.trade_apps.TradeAppsController;
import bisq.desktop.main.content.trade_apps.bisqEasy.BisqEasyController;
import bisq.desktop.main.content.user.UserController;
import bisq.desktop.main.content.wallet.WalletController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ContentController extends NavigationController {
    private final ServiceProvider serviceProvider;
    @Getter
    private final ContentModel model;
    @Getter
    private final ContentView view;

    public ContentController(ServiceProvider serviceProvider) {
        super(NavigationTarget.CONTENT);

        this.serviceProvider = serviceProvider;
        model = new ContentModel(serviceProvider.getWalletService().isPresent());
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
        if (navigationTarget == NavigationTarget.WALLET && !model.isWalletEnabled()) {
            navigationTarget = NavigationTarget.DASHBOARD;
        }
        switch (navigationTarget) {
            case DASHBOARD: {
                return Optional.of(new DashboardController(serviceProvider));
            }
            case DISCUSSION: {
                return Optional.of(new CommonChatController(serviceProvider, ChatChannelDomain.DISCUSSION));
            }
            case ACADEMY_OVERVIEW: {
                return Optional.of(new AcademyOverviewController(serviceProvider));
            }
            case BISQ_ACADEMY: {
                return Optional.of(new BisqAcademyController(serviceProvider));
            }
            case BITCOIN_ACADEMY: {
                return Optional.of(new BitcoinAcademyController(serviceProvider));
            }
            case SECURITY_ACADEMY: {
                return Optional.of(new SecurityAcademyController(serviceProvider));
            }
            case PRIVACY_ACADEMY: {
                return Optional.of(new PrivacyAcademyController(serviceProvider));
            }
            case WALLETS_ACADEMY: {
                return Optional.of(new WalletsAcademyController(serviceProvider));
            }
            case FOSS_ACADEMY: {
                return Optional.of(new FossAcademyController(serviceProvider));
            }
            case EVENTS: {
                return Optional.of(new CommonChatController(serviceProvider, ChatChannelDomain.EVENTS));
            }
            case SUPPORT: {
                return Optional.of(new CommonChatController(serviceProvider, ChatChannelDomain.SUPPORT));
            }
            case TRADE_OVERVIEW: {
                return Optional.of(new TradeAppsController(serviceProvider));
            }
            case BISQ_EASY: {
                return Optional.of(new BisqEasyController(serviceProvider));
            }

            case MULTISIG: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.MULTISIG,
                        "protocol-multisig",
                        "https://bisq.network/"));
            }
            case SUBMARINE: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.SUBMARINE,
                        "protocol-submarine",
                        "https://docs.lightning.engineering/the-lightning-network/multihop-payments/understanding-submarine-swaps"));
            }
            case LIQUID_MULTISIG: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.LIQUID_MULTISIG,
                        "protocol-liquid-multisig",
                        "https://bisq.wiki/liquid-multisig"));
            }
            case LIGHTNING_FIAT: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.LIGHTNING_FIAT,
                        "protocol-ln-liquid",
                        "https://bisq.wiki/ln-liquid"));
            }
            case LIQUID_SWAP: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.LIQUID_SWAP,
                        "protocol-liquid-multisig",
                        "https://www.blockstream.com/liquid/"));
            }
            case BSQ_SWAP: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.BSQ_SWAP,
                        "protocol-bsq",
                        "https://bisq.wiki/BSQ"));
            }
            case LIGHTNING_ESCROW: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.LIGHTNING_ESCROW,
                        "protocol-lightning-escrow",
                        "https://github.com/bisq-network/proposals/issues/416"));
            }
            case MONERO_SWAP: {
                return Optional.of(new ProtocolRoadmapController(TradeProtocolType.MONERO_SWAP,
                        "protocol-monero",
                        "https://www.monerooutreach.org/stories/monero-atomic-swaps.html"));
            }

            case USER: {
                return Optional.of(new UserController(serviceProvider));
            }
            case SETTINGS: {
                return Optional.of(new SettingsController(serviceProvider));
            }

            case WALLET: {
                return Optional.of(new WalletController(serviceProvider));
            }

            case AUTHORIZED_ROLE: {
                return Optional.of(new AuthorizedRoleController(serviceProvider));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
