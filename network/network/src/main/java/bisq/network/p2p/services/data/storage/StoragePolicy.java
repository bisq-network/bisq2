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

package bisq.network.p2p.services.data.storage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares how the distributed storage treats a payload type: how long an entry is kept, how it is prioritised when
 * an inventory response is truncated, and how many entries its store holds. The policy is constant per class, so it
 * is declared on the type instead of being carried by each instance.
 * <p>
 * {@link MetaData#from(Class)} resolves the declared policy into a {@link MetaData}, adding the className derived
 * from the annotated class. The policy is declared here and never travels: a peer receives the payload and resolves
 * the policy from its own copy of this annotation. See {@link MetaData} for the cases where a peer cannot do that
 * and the resolved values are sent instead.
 * <p>
 * The annotation is {@link Inherited} so that an abstract payload base class can declare the policy for all its
 * subclasses, while each subclass still resolves its own className and therefore its own storage file.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StoragePolicy {
    Ttl ttl();

    Priority priority() default Priority.DEFAULT;

    MaxMapSize maxMapSize() default MaxMapSize.SIZE_1000;
}
