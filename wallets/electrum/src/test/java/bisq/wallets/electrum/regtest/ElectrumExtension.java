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
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import org.junit.jupiter.api.extension.*;

import java.io.IOException;

public class ElectrumExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
    private final ElectrumRegtestSetup electrumRegtestSetup;

    public ElectrumExtension() throws IOException {
        electrumRegtestSetup = new ElectrumRegtestSetup(true);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        electrumRegtestSetup.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        electrumRegtestSetup.shutdown();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type.equals(BitcoindRegtestSetup.class) || type.equals(ElectrumRegtestSetup.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        if (type.equals(BitcoindRegtestSetup.class)) {
            return electrumRegtestSetup.getBitcoindRegtestSetup();
        } else if (type.equals(ElectrumRegtestSetup.class)) {
            return electrumRegtestSetup;
        }

        throw new IllegalStateException("Unknown parameter type");
    }
}
