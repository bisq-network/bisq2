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

package network.misq.user;

import network.misq.account.Account;
import network.misq.id.IdentityRepository;
import network.misq.offer.OpenOffer;
import network.misq.persistence.Persistence;
import network.misq.protocol.Deal;
import network.misq.support.Dispute;

import java.util.Collection;

public class User {
    // expected dependencies
    Collection<IdentityRepository> identities; // A user can manage multiple identities and assign it to offers or other interactions
    Collection<Account> accounts;
    Collection<OpenOffer> openOffers;
    Collection<Deal> deals;
    Collection<Dispute> disputes;
    Persistence persistence;
}