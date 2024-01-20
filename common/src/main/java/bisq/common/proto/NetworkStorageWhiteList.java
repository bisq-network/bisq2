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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains the classNames of all objects which might get persisted by the StorageService. Those files are inside
 * the `network` directory.
 */
@Slf4j
public class NetworkStorageWhiteList {
    @Getter
    private static final Set<String> classNames = new HashSet<>();

    public static void add(Class<?> clazz) {
        classNames.add(clazz.getSimpleName());
    }

    public static <T extends NetworkProto> void add(String protoTypeName, ProtoResolver<T> resolver) {
        try {
            String[] resolverTokens = resolver.getClass().getSimpleName().split("\\$\\$");
            String[] protoTypeNameTokens = protoTypeName.split("\\.");
            String className = resolverTokens[0];
            if (className.equals(protoTypeNameTokens[1])) {
                classNames.add(className);
            } else {
                log.warn("resolver and protoTypeName seems to not match. protoTypeName={} resolver={}",
                        protoTypeName, resolver.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Error at checking if resolver and protoTypeName match. protoTypeName={} resolver={}",
                    protoTypeName, resolver.getClass().getSimpleName());
        }
    }
}
