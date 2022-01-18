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

package bisq.account;

import lombok.Getter;

@Getter
public abstract class Settlement<T extends SettlementMethod> {
    public enum Type {
        AUTOMATIC, MANUAL
    }

    protected final T method;
    protected final String name;
    protected final Type type;

    Settlement(T method) {
        this(method, method.name());
    }

    Settlement(T method, String name) {
        this.method = method;
        this.name = name;
        type = getDefaultType();
    }

    Settlement(T method, String name, Type type) {
        this.method = method;
        this.name = name;
        this.type = type;
    }

    Settlement(String name) {
        this.method = getDefaultMethod();
        this.name = name;
        type = getDefaultType();
    }

    protected abstract T getDefaultMethod();

    protected Type getDefaultType() {
        return Type.MANUAL;
    }
}
