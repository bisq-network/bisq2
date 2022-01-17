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

import bisq.persistence.Persistable;
import lombok.Getter;

public class UserModel implements Persistable<UserModel> {
    @Getter
    private final Cookie cookie;

    public UserModel() {
        cookie = new Cookie();
    }

    public UserModel(Cookie cookie) {
        this.cookie = cookie;
    }

    @Override
    public UserModel getClone() {
        return new UserModel(cookie);
    }

    @Override
    public void applyPersisted(UserModel userModel) {
        cookie.putAll(userModel.getCookie());
    }
}