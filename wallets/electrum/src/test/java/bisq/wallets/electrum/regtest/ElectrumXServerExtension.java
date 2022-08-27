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

public class ElectrumXServerExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private final ElectrumXServerRegtestSetup regtestSetup;

    public ElectrumXServerExtension() throws IOException {
        regtestSetup = new ElectrumXServerRegtestSetup();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        regtestSetup.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        regtestSetup.shutdown();
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
