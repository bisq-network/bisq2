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

    //////////////////////////////////////////////////////////////////////
    // OVERLAY
    //////////////////////////////////////////////////////////////////////

    OVERLAY(PRIMARY_STAGE, false),

    ONBOARDING(OVERLAY, false),
    ONBOARDING_BISQ_2_INTRO(ONBOARDING, false),
    ONBOARDING_GENERATE_NYM(ONBOARDING, false),
    ONBOARDING_BISQ_EASY_OLD(ONBOARDING, false),

    CREATE_OFFER(OVERLAY, false),
    CREATE_OFFER_DIRECTION(CREATE_OFFER, false),
    CREATE_OFFER_MARKET(CREATE_OFFER, false),
    CREATE_OFFER_AMOUNT(CREATE_OFFER, false),
    CREATE_OFFER_PAYMENT_METHOD(CREATE_OFFER, false),
    CREATE_OFFER_OFFER_COMPLETED(CREATE_OFFER, false),

    CREATE_PROFILE(OVERLAY, false),
    CREATE_PROFILE_STEP1(CREATE_PROFILE, false),
    CREATE_PROFILE_STEP2(CREATE_PROFILE, false),

    BISQ_EASY_HELP(OVERLAY, false),
    BISQ_EASY_HELP_TAB_1(BISQ_EASY_HELP, false),
    BISQ_EASY_HELP_TAB_2(BISQ_EASY_HELP, false),
    BISQ_EASY_HELP_TAB_3(BISQ_EASY_HELP, false),

    EDIT_PROFILE(OVERLAY, false),

    BURN_BSQ(OVERLAY, false),
    BURN_BSQ_TAB_1(BURN_BSQ, false),
    BURN_BSQ_TAB_2(BURN_BSQ, false),
    BURN_BSQ_TAB_3(BURN_BSQ, false),

    BSQ_BOND(OVERLAY, false),
    BSQ_BOND_TAB_1(BSQ_BOND, false),
    BSQ_BOND_TAB_2(BSQ_BOND, false),
    BSQ_BOND_TAB_3(BSQ_BOND, false),


    ACCOUNT_AGE(OVERLAY, false),
    ACCOUNT_AGE_TAB_1(ACCOUNT_AGE, false),
    ACCOUNT_AGE_TAB_2(ACCOUNT_AGE, false),
    ACCOUNT_AGE_TAB_3(ACCOUNT_AGE, false),


    SINGED_ACCOUNT(OVERLAY, false),
    SINGED_ACCOUNT_TAB_1(SINGED_ACCOUNT, false),
    SINGED_ACCOUNT_TAB_2(SINGED_ACCOUNT, false),
    SINGED_ACCOUNT_TAB_3(SINGED_ACCOUNT, false),


    //////////////////////////////////////////////////////////////////////
    // MAIN
    //////////////////////////////////////////////////////////////////////

    MAIN(PRIMARY_STAGE, false),

    CONTENT(MAIN, false),

    DASHBOARD(CONTENT),
    DISCUSS(CONTENT),

    ACADEMY(CONTENT),
    BISQ_ACADEMY(CONTENT),
    BITCOIN_ACADEMY(CONTENT),
    SECURITY_ACADEMY(CONTENT),
    PRIVACY_ACADEMY(CONTENT),
    WALLETS_ACADEMY(CONTENT),
    FOSS_ACADEMY(CONTENT),

    EVENTS(CONTENT),

    SUPPORT(CONTENT),

    TRADE_OVERVIEW(CONTENT),
    TRADE_OVERVIEW_LIST(TRADE_OVERVIEW),
    TRADE_OVERVIEW_GRID(TRADE_OVERVIEW),

    BISQ_EASY(CONTENT),
    BISQ_EASY_INTRO(BISQ_EASY),
    BISQ_EASY_CHAT(BISQ_EASY),

    TRADE_GUIDE(BISQ_EASY_CHAT, false),
    TRADE_GUIDE_TAB_1(TRADE_GUIDE, false),
    TRADE_GUIDE_TAB_2(TRADE_GUIDE, false),
    TRADE_GUIDE_TAB_3(TRADE_GUIDE, false),

    LIQUID_SWAP(CONTENT),

    BISQ_MULTI_SIG(CONTENT),
    MULTI_SIG_OPEN_OFFERS(BISQ_MULTI_SIG),
    MULTI_SIG_PENDING_TRADES(BISQ_MULTI_SIG),
    MULTI_SIG_CLOSED_TRADES(BISQ_MULTI_SIG),
    MULTI_SIG_OFFER_BOOK(BISQ_MULTI_SIG),
    MULTI_SIG_CREATE_OFFER(BISQ_MULTI_SIG),
    MULTI_SIG_TAKE_OFFER(BISQ_MULTI_SIG, false),

    ATOMIC_CROSS_CHAIN_SWAP(CONTENT),
    BSQ_SWAP(CONTENT),
    LN_3_PARTY(CONTENT),

    SETTINGS(CONTENT),
    PREFERENCES(SETTINGS),
    NETWORK_INFO(SETTINGS),
    ABOUT(SETTINGS),
    USER_PROFILE(SETTINGS),
    REPUTATION(SETTINGS),

    WALLET_BITCOIN(CONTENT),
    WALLET_BITCOIN_TRANSACTIONS(WALLET_BITCOIN),
    WALLET_BITCOIN_SEND(WALLET_BITCOIN),
    WALLET_BITCOIN_RECEIVE(WALLET_BITCOIN),
    WALLET_BITCOIN_UTXOS(WALLET_BITCOIN),


    WALLET_LBTC(CONTENT, false),
    WALLET_LBTC_TRANSACTIONS(WALLET_LBTC),
    WALLET_LBTC_SEND(WALLET_LBTC),
    WALLET_LBTC_RECEIVE(WALLET_LBTC),
    WALLET_LBTC_UTXOS(WALLET_LBTC);

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