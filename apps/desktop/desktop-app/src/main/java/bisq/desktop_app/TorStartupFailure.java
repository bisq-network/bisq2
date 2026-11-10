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

package bisq.desktop_app;

import bisq.network.p2p.node.transport.TorTransportService;

import java.util.List;
import java.util.Optional;

/**
 * A failed start of Tor which a choice of the user fixes, which Bisq then writes into bisq.conf. The Tor library
 * decides both from its own checks and the refusals of sandy. It tells a refusal of sandy from an exit of Tor by the
 * status record of sandy, which Tor cannot write, not by what Tor wrote. A missing record also counts as a refusal,
 * so a compromised Tor which fills the disk can cause one at a later start (tor-startup.md, Known gaps).
 */
sealed interface TorStartupFailure {
    // Tor cannot run in the sandbox on this system
    record SandboxUnavailable() implements TorStartupFailure {
    }

    // The sandbox does not let Tor start the pluggable transports of the bridges
    record PluggableTransportsNotAllowed(List<String> transportNames) implements TorStartupFailure {
        public PluggableTransportsNotAllowed {
            transportNames = List.copyOf(transportNames);
        }
    }

    static Optional<TorStartupFailure> find(Throwable startFailure) {
        Optional<List<String>> notAllowedTransports = TorTransportService.findPluggableTransportsNotAllowed(startFailure);
        if (notAllowedTransports.isPresent()) {
            return Optional.of(new PluggableTransportsNotAllowed(notAllowedTransports.get()));
        }
        return TorTransportService.isSandboxUnavailable(startFailure) ?
                Optional.of(new SandboxUnavailable()) :
                Optional.empty();
    }
}
