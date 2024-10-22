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

package bisq.bisq_easy;

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

    UNLOCK(OVERLAY, false),
    TAC(OVERLAY, false),
    UPDATER(OVERLAY, false),

    ONBOARDING(OVERLAY, false),
    ONBOARDING_WELCOME(ONBOARDING, false),
    ONBOARDING_GENERATE_NYM(ONBOARDING, false),
    ONBOARDING_PASSWORD(ONBOARDING, false),

    REPORT_TO_MODERATOR(OVERLAY, false),

    BISQ_EASY_VIDEO(OVERLAY, false),

    TRADE_WIZARD(OVERLAY, false),
    TRADE_WIZARD_DIRECTION(TRADE_WIZARD, false),
    TRADE_WIZARD_MARKET(TRADE_WIZARD, false),
    TRADE_WIZARD_PRICE(TRADE_WIZARD, false),
    TRADE_WIZARD_AMOUNT(TRADE_WIZARD, false),
    TRADE_WIZARD_PAYMENT_METHODS(TRADE_WIZARD, false),
    TRADE_WIZARD_TAKE_OFFER_OFFER(TRADE_WIZARD, false),
    TRADE_WIZARD_REVIEW_OFFER(TRADE_WIZARD, false),

    TAKE_OFFER(OVERLAY, false),
    TAKE_OFFER_PRICE(TAKE_OFFER, false),
    TAKE_OFFER_AMOUNT(TAKE_OFFER, false),
    TAKE_OFFER_PAYMENT(TAKE_OFFER, false),
    TAKE_OFFER_REVIEW(TAKE_OFFER, false),

    CREATE_PROFILE(OVERLAY, false),
    CREATE_PROFILE_STEP1(CREATE_PROFILE, false),
    CREATE_PROFILE_STEP2(CREATE_PROFILE, false),

    CREATE_BISQ_EASY_PAYMENT_ACCOUNT(OVERLAY, false),

    BISQ_EASY_GUIDE(OVERLAY, false),
    BISQ_EASY_GUIDE_WELCOME(BISQ_EASY_GUIDE, false),
    BISQ_EASY_GUIDE_SECURITY(BISQ_EASY_GUIDE, false),
    BISQ_EASY_GUIDE_PROCESS(BISQ_EASY_GUIDE, false),
    BISQ_EASY_GUIDE_RULES(BISQ_EASY_GUIDE, false),

    WALLET_GUIDE(OVERLAY, false),
    WALLET_GUIDE_INTRO(WALLET_GUIDE, false),
    WALLET_GUIDE_DOWNLOAD(WALLET_GUIDE, false),
    WALLET_GUIDE_CREATE_WALLET(WALLET_GUIDE, false),
    WALLET_GUIDE_RECEIVE(WALLET_GUIDE, false),

    BISQ_EASY_OFFER_DETAILS(OVERLAY, false),

    CHAT_RULES(OVERLAY, false),

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


    SIGNED_WITNESS(OVERLAY, false),
    SIGNED_WITNESS_TAB_1(SIGNED_WITNESS, false),
    SIGNED_WITNESS_TAB_2(SIGNED_WITNESS, false),
    SIGNED_WITNESS_TAB_3(SIGNED_WITNESS, false),


    //////////////////////////////////////////////////////////////////////
    // MAIN
    //////////////////////////////////////////////////////////////////////

    MAIN(PRIMARY_STAGE, false),

    CONTENT(MAIN, false),

    DASHBOARD(CONTENT),

    // BISQ_EASY
    BISQ_EASY(CONTENT),
    BISQ_EASY_ONBOARDING(BISQ_EASY),
    BISQ_EASY_OFFERBOOK(BISQ_EASY),
    BISQ_EASY_OPEN_TRADES(BISQ_EASY),
    BISQ_EASY_PRIVATE_CHAT(BISQ_EASY),

    // REPUTATION
    REPUTATION(CONTENT),
    BUILD_REPUTATION(REPUTATION),
    REPUTATION_RANKING(REPUTATION),
    REPUTATION_SCORE(REPUTATION),

    // TRADE_PROTOCOLS
    TRADE_PROTOCOLS(CONTENT),
    TRADE_PROTOCOLS_OVERVIEW(TRADE_PROTOCOLS),
    BISQ_EASY_INFO(TRADE_PROTOCOLS),
    BISQ_MU_SIG(TRADE_PROTOCOLS),
    SUBMARINE(TRADE_PROTOCOLS),
    BISQ_LIGHTNING(TRADE_PROTOCOLS),
    MORE_TRADE_PROTOCOLS(TRADE_PROTOCOLS, false),

    // ACADEMY
    ACADEMY(CONTENT),
    OVERVIEW_ACADEMY(ACADEMY),
    BISQ_ACADEMY(ACADEMY),
    BITCOIN_ACADEMY(ACADEMY),
    SECURITY_ACADEMY(ACADEMY),
    PRIVACY_ACADEMY(ACADEMY),
    WALLETS_ACADEMY(ACADEMY),
    FOSS_ACADEMY(ACADEMY),

    // CHAT
    CHAT(CONTENT),
    CHAT_DISCUSSION(CHAT),
    CHAT_PRIVATE(CHAT),

    // SUPPORT
    SUPPORT(CONTENT),
    SUPPORT_ASSISTANCE(SUPPORT),
    SUPPORT_RESOURCES(SUPPORT),

    // USER
    USER(CONTENT),
    USER_PROFILE(USER),
    PASSWORD(USER),
    BISQ_EASY_PAYMENT_ACCOUNTS(USER),


    // NETWORK
    NETWORK(CONTENT),
    MY_NETWORK_NODE(NETWORK),
    P2P_NETWORK(NETWORK),
    ROLES(NETWORK),
    NODES(NETWORK),

    ROLES_TABS(ROLES),
    REGISTER_MEDIATOR(ROLES_TABS),
    REGISTER_ARBITRATOR(ROLES_TABS),
    REGISTER_MODERATOR(ROLES_TABS),
    REGISTER_SECURITY_MANAGER(ROLES_TABS),
    REGISTER_RELEASE_MANAGER(ROLES_TABS),

    NODES_TABS(NODES),
    REGISTER_SEED_NODE(NODES_TABS),
    REGISTER_ORACLE_NODE(NODES_TABS),
    REGISTER_EXPLORER_NODE(NODES_TABS),
    REGISTER_MARKET_PRICE_NODE(NODES_TABS),

    // SETTINGS
    SETTINGS(CONTENT),
    LANGUAGE_SETTINGS(SETTINGS),
    NOTIFICATION_SETTINGS(SETTINGS),
    DISPLAY_SETTINGS(SETTINGS),
    TRADE_SETTINGS(SETTINGS),
    MISC_SETTINGS(SETTINGS),

    // WALLET
    WALLET(CONTENT),
    WALLET_DASHBOARD(WALLET),
    WALLET_SEND(WALLET),
    WALLET_RECEIVE(WALLET),
    WALLET_TXS(WALLET),
    WALLET_SETTINGS(WALLET),

    // AUTHORIZED_ROLE
    AUTHORIZED_ROLE(CONTENT),
    MEDIATOR(AUTHORIZED_ROLE),
    ARBITRATOR(AUTHORIZED_ROLE),
    MODERATOR(AUTHORIZED_ROLE),
    SECURITY_MANAGER(AUTHORIZED_ROLE),
    RELEASE_MANAGER(AUTHORIZED_ROLE),
    SEED_NODE(AUTHORIZED_ROLE),
    ORACLE_NODE(AUTHORIZED_ROLE),
    EXPLORER_NODE(AUTHORIZED_ROLE),
    MARKET_PRICE_NODE(AUTHORIZED_ROLE);


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