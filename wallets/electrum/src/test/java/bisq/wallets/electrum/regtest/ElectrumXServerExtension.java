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

import bisq.wallets.electrum.regtest.electrumx.ElectrumXServerConfig;
import bisq.wallets.electrum.regtest.electrumx.ElectrumXServerRegtestSetup;
import org.junit.jupiter.api.extension.*;

import java.io.IOException;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class ElectrumXServerExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource, ParameterResolver {

    private static boolean isRunning;
    private static final ElectrumXServerRegtestSetup regtestSetup;

    static {
        try {
            regtestSetup = new ElectrumXServerRegtestSetup();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ElectrumXServerExtension() {
    }

    @Override
    public synchronized void beforeAll(ExtensionContext context) throws Exception {
        if (!isRunning) {
            regtestSetup.start();
            isRunning = true;

            // Register close hook
            context.getRoot()
                    .getStore(GLOBAL)
                    .put("register_electrumx_close_hook", this);
        }
    }

    @Override
    public synchronized void close() {
        if (isRunning) {
            regtestSetup.shutdown();
            isRunning = false;
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type.equals(ElectrumXServerConfig.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        if (type.equals(ElectrumXServerConfig.class)) {
            return regtestSetup.getServerConfig();
        }
        throw new IllegalStateException("Unknown parameter type");
    }
}
