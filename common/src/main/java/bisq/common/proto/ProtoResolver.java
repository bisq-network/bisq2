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

package bisq.common.proto;

import com.google.protobuf.Any;

public interface ProtoResolver<T extends Proto> {
    T fromAny(Any any);

    /**
     * Derives the proto type name from the class which gets packed into the Any, matching the type URL suffix from
     * {@link ProtobufUtils#getProtoType(Any)}, e.g. bisq.user.identity.UserIdentityStore maps to user.UserIdentityStore.
     * Must be a source declared class, never a compiler generated one like a lambda, as those names are unspecified
     * and get merged by optimizers like R8.
     */
    static String getProtoType(Class<?> clazz) {
        String[] tokens = clazz.getName().split("\\.");
        if (tokens.length < 2) {
            throw new IllegalArgumentException("Cannot derive the proto type name from " + clazz.getName()
                    + ". The java package is part of the name, so the class must not be repackaged or obfuscated.");
        }
        return tokens[1] + "." + clazz.getSimpleName();
    }
}
