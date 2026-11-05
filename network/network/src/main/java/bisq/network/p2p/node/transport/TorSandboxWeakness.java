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

/**
 * What the sandbox of the running Tor cannot do, because of the level which sandy selected for this host.
 */
public enum TorSandboxWeakness {
    // Tor uses the network of the host, because the pluggable transports of the bridges need it
    HOST_NETWORK_FOR_TRANSPORTS,
    // Tor uses the network of the host, because this system cannot give it one of its own
    HOST_NETWORK_OF_SYSTEM,
    // Tor can send signals to the processes of the user, because this system cannot isolate them
    SIGNALS_NOT_ISOLATED
}
