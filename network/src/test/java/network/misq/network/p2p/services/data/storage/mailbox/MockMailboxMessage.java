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

package network.misq.network.p2p.services.data.storage.mailbox;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import network.misq.network.p2p.services.data.storage.MetaData;

import java.util.concurrent.TimeUnit;

@EqualsAndHashCode
@Getter
public class MockMailboxMessage implements MailboxMessage {
    private final String text;
    private final MetaData metaData;

    public MockMailboxMessage(String text) {
        this.text = text;
        metaData = new MetaData(TimeUnit.DAYS.toMillis(10), 100000, getClass().getSimpleName());
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }
}
