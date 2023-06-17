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

package bisq.protocol.bisq_easy.tasks;

import bisq.protocol.bisq_easy.BisqEasyProtocolModel;
import bisq.protocol.bisq_easy.ServiceProvider;
import bisq.protocol.bisq_easy.messages.BisqEasyTradeMessage;

public abstract class ProcessBisqEasyMessageTask<M extends BisqEasyTradeMessage> extends BisqEasyTask {
    protected ProcessBisqEasyMessageTask(ServiceProvider serviceProvider,
                                         BisqEasyProtocolModel model) {
        super(serviceProvider, model);
    }

    protected abstract void verifyMessage(M message);
}