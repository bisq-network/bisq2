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

package network.misq.protocol;

import network.misq.account.Account;
import network.misq.contract.Contract;
import network.misq.id.IdentityRepository;
import network.misq.network.NetworkService;
import network.misq.offer.Offer;
import network.misq.persistence.Persistence;
import network.misq.wallets.Wallet;


public class Deal {
    // expected dependencies
    IdentityRepository identityRepository;
    Account account;
    Contract contract;
    Offer offer;
    NetworkService networkService;
    Persistence persistence;
    Wallet wallet;
}