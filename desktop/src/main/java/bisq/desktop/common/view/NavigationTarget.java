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

package bisq.desktop.common.view;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public enum NavigationTarget {
    NONE(),
    ROOT(),

    PRIMARY_STAGE(ROOT, false),

    SPLASH(PRIMARY_STAGE, false),

    ONBOARDING(PRIMARY_STAGE, false),

    INIT_USER_PROFILE(ONBOARDING, false),
    SELECT_USER_TYPE(ONBOARDING),
    ONBOARD_NEWBIE(ONBOARDING),
    ONBOARD_PRO_TRADER(ONBOARDING),

    MAIN(PRIMARY_STAGE, false),

    CONTENT(MAIN, false),

    SOCIAL(CONTENT, false),
    GETTING_STARTED(SOCIAL),
    DISCUSS(SOCIAL),
    LEARN(SOCIAL),
    CONNECT(SOCIAL),

    TRADE(CONTENT, false),
    TRADE_OVERVIEW(TRADE),
    TRADE_OVERVIEW_GRID(TRADE),
    OFFERBOOK(TRADE),
    OPEN_OFFERS(TRADE),
    PENDING_TRADES(TRADE),
    CLOSED_TRADES(TRADE),

    SATOSHI_SQUARE(CONTENT, false),
    EXCHANGE(SATOSHI_SQUARE),

    LIQUID_SWAP(CONTENT),

    BISQ_MULTI_SIG(CONTENT),
    MULTI_SIG_OPEN_OFFERS(BISQ_MULTI_SIG),
    MULTI_SIG_PENDING_TRADES(BISQ_MULTI_SIG),
    MULTI_SIG_CLOSED_TRADES(BISQ_MULTI_SIG),
    MULTI_SIG_OFFERBOOK(BISQ_MULTI_SIG),
    MULTI_SIG_CREATE_OFFER(BISQ_MULTI_SIG),
    MULTI_SIG_TAKE_OFFER(BISQ_MULTI_SIG, false),

    ATOMIC_CROSS_CHAIN_SWAP(CONTENT),
    BSQ_SWAP(CONTENT),
    LN_3_PARTY(CONTENT),


    PORTFOLIO(CONTENT),


    SETTINGS(CONTENT, false),
    PREFERENCES(SETTINGS),
    NETWORK_INFO(SETTINGS),
    ABOUT(SETTINGS),
    USER_PROFILE(SETTINGS),

    MARKETS(CONTENT),
    SUPPORT(CONTENT),

    WALLET(CONTENT, false),
    WALLET_TRANSACTIONS(WALLET),
    WALLET_SEND(WALLET),
    WALLET_RECEIVE(WALLET),
    WALLET_UTXOS(WALLET);

    @Getter
    private final Optional<NavigationTarget> parent;
    @Getter
    private final List<NavigationTarget> path;
    @Getter
    private final boolean allowPersistence;

    NavigationTarget() {
        parent = Optional.empty();
        path = new ArrayList<>();
        allowPersistence = true;
    }

    NavigationTarget(NavigationTarget parent) {
        this(parent, true);
    }

    NavigationTarget(NavigationTarget parent, boolean allowPersistence) {
        this.parent = Optional.of(parent);
        List<NavigationTarget> temp = new ArrayList<>();
        Optional<NavigationTarget> candidate = Optional.of(parent);
        while (candidate.isPresent()) {
            temp.add(0, candidate.get());
            candidate = candidate.get().getParent();
        }
        this.path = temp;
        this.allowPersistence = allowPersistence;
    }
}