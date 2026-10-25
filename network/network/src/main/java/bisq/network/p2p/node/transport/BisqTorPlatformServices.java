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
package bisq.network.p2p.node.transport;

import bisq.common.facades.FacadeProvider;
import bisq.common.file.FileMutatorUtils;
import bisq.network.tor.PlatformServices;

import java.util.stream.Stream;

/**
 * Passes the platform settings of the Bisq application to the Tor build. The settings are read at each call, as
 * TorService did before, because the application sets them at startup.
 */
class BisqTorPlatformServices implements PlatformServices {
    @Override
    public String getMyPid() {
        return FacadeProvider.getJdkFacade().getMyPid();
    }

    @Override
    public Stream<String> getProcessCommandStream() {
        return FacadeProvider.getJdkFacade().getProcessCommandStream();
    }

    @Override
    public boolean supportsPosixPermissions() {
        return FileMutatorUtils.supportsPosixPermissions();
    }
}
