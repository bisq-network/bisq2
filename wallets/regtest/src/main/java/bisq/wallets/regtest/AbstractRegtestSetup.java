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

package bisq.wallets.regtest;

import bisq.common.file.FileUtils;
import bisq.wallets.json_rpc.RpcConfig;
import bisq.wallets.regtest.process.BisqProcess;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public abstract class AbstractRegtestSetup<T extends BisqProcess> implements BisqProcess {
    public static final String WALLET_PASSPHRASE = "My super secret passphrase that nobody can guess.";

    protected T daemonProcess;
    protected final Path tmpDirPath;

    public AbstractRegtestSetup() throws IOException {
        this.tmpDirPath = FileUtils.createTempDir();
    }

    protected abstract T createProcess();

    public void start() throws InterruptedException {
        daemonProcess = createProcess();
        daemonProcess.start();
    }

    public void shutdown() {
        daemonProcess.shutdown();
    }

    public abstract List<String> mineOneBlock() throws InterruptedException;

    public abstract RpcConfig getRpcConfig();
}
