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

package bisq.wallets.electrum.regtest;

import bisq.wallets.electrum.regtest.electrum.ElectrumRegtestSetup;
import bisq.wallets.electrum.regtest.electrum.MacLinuxElectrumRegtestSetup;
import bisq.wallets.electrum.regtest.electrum.WindowsElectrumRegtestSetup;
import bisq.wallets.regtest.bitcoind.RemoteBitcoind;
import org.junit.jupiter.api.extension.*;

import java.io.IOException;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class ElectrumExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource, ParameterResolver {
    private static boolean isRunning;
    private static final ElectrumRegtestSetup electrumRegtestSetup;

    static {
        try {
            electrumRegtestSetup = !WindowsElectrumRegtestSetup.isExternalBitcoindAndElectrumXEnvironment() ?
                    new MacLinuxElectrumRegtestSetup(true) : new WindowsElectrumRegtestSetup(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ElectrumExtension() {
    }

    @Override
    public synchronized void beforeAll(ExtensionContext context) throws Exception {
        if (!isRunning) {
            electrumRegtestSetup.start();
            isRunning = true;

            // Register close hook
            context.getRoot()
                    .getStore(GLOBAL)
                    .put("register_electrum_close_hook", this);
        }
    }

    @Override
    public synchronized void close() {
        if (isRunning) {
            electrumRegtestSetup.shutdown();
            isRunning = false;
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type.equals(RemoteBitcoind.class) || type.equals(ElectrumRegtestSetup.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        if (type.equals(RemoteBitcoind.class)) {
            return electrumRegtestSetup.getRemoteBitcoind();
        } else if (type.equals(ElectrumRegtestSetup.class)) {
            return electrumRegtestSetup;
        }

        throw new IllegalStateException("Unknown parameter type");
    }
}
