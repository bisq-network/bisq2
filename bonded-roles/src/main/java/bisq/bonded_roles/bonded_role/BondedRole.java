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

package bisq.bonded_roles.bonded_role;

import bisq.common.observable.Observable;
import lombok.EqualsAndHashCode;
import lombok.Getter;


@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BondedRole {
    @Getter
    @EqualsAndHashCode.Include
    private final AuthorizedBondedRole authorizedBondedRole;
    private final Observable<Boolean> isBannedObservable = new Observable<>(false);

    public BondedRole(AuthorizedBondedRole authorizedBondedRole) {
        this.authorizedBondedRole = authorizedBondedRole;
    }

    public boolean isNotBanned() {
        return !isBanned();
    }

    public boolean isBanned() {
        return isBannedObservable.get();
    }

    public void setIsBanned(boolean value) {
        isBannedObservable.set(value);
    }
}