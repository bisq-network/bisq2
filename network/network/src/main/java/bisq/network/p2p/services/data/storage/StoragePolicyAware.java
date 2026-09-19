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

/**
 * Implemented by types which the distributed storage handles under a storage policy. Payload types declare theirs
 * with {@link StoragePolicy} and use the default here. The wrappers override instead: AuthenticatedData and its
 * subclasses pass on the policy of the payload they hold, MailboxData carries the one it received over the wire,
 * and ConfidentialMessage has none it can know, since its content is encrypted.
 * <p>
 * It exists as a separate interface so that {@link DistributedData}, {@link StorageData} and
 * {@link bisq.network.p2p.services.data.storage.mailbox.MailboxMessage} share one declaration of
 * {@code getMetaData()}. A type implementing two of them would otherwise inherit the method from unrelated
 * interfaces, which Java rejects.
 */
public interface StoragePolicyAware {
    default MetaData getMetaData() {
        return MetaData.from(getClass());
    }
}
