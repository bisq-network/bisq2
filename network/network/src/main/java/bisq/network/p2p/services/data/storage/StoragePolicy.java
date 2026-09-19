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
 *
 * <h2>Choosing a value the enums do not offer</h2>
 * Add the constant to {@link Ttl} or {@link MaxMapSize}. The sets are closed so that a value is chosen once and
 * shared, not invented per call site: every node applies these to the same data, and a renamed or changed constant
 * shows at once which types it affects.
 *
 * <h2>Properties which depend on more than the type</h2>
 * A type whose storage properties cannot be a constant, because they follow from the payload itself, declares no
 * policy and overrides {@code getMetaData()} instead. The wrappers do this already: AuthenticatedData passes on the
 * policy of the payload it holds, and MailboxData the one it received over the wire.
 * <p>
 * Such an override must be a deterministic function of data every node already has, because each receiver resolves
 * the properties on its own. Two nodes which compute a different ttl for the same entry expire it at different
 * times, and nothing in a running network reports the disagreement. In particular it must not depend on anything a
 * peer chooses, since that would let the sender pick its own retention.
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
