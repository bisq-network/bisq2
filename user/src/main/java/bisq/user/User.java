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

package bisq.user;

import bisq.account.Account;
import bisq.identity.IdentityService;
import bisq.offer.OpenOffer;
import bisq.persistence.Persistence;
import bisq.protocol.Deal;
import bisq.support.Dispute;

import java.util.Collection;

public class User {
    // expected dependencies
    Collection<IdentityService> identities; // A user can manage multiple identities and assign it to offers or other interactions
    Collection<Account> accounts;
    Collection<OpenOffer> openOffers;
    Collection<Deal> deals;
    Collection<Dispute> disputes;
    Persistence persistence;
}