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

package bisq.wallets.regtest.process;

import bisq.wallets.core.exceptions.WalletStartupFailedException;
import bisq.wallets.process.BisqProcess;
import com.google.common.collect.Lists;

import java.util.List;

public class MultiProcessCoordinator implements BisqProcess {
    protected final List<BisqProcess> daemonProcesses;

    public MultiProcessCoordinator(List<BisqProcess> daemonProcesses) {
        this.daemonProcesses = daemonProcesses;
    }

    @Override
    public void start() {
        daemonProcesses.forEach(bisqProcess -> {
            try {
                bisqProcess.start();
            } catch (InterruptedException e) {
                throw new WalletStartupFailedException("Cannot start process.", e);
            }
        });
    }

    @Override
    public void shutdown() {
        // Need to shut down processes in correct order!
        Lists.reverse(daemonProcesses)
                .forEach(BisqProcess::shutdown);
    }
}
