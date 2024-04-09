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
/**
 * The peergroup package holds classes responsible for bootstrapping the network and maintaining a
 * healthy network of neighbor peers.
 * <p>
 * PeerGroupService: Manages the peer group.
 * <p>
 * PeerExchangeService: Responsible for the PeerExchange protocol.
 * <p>
 * PeerGroup: Our shared model holding the peers in different categories and self-manages connected peers using the
 * ConnectionListener
 * <p>
 * PeerConfig: Holds configuration properties like target number of connections
 * <p>
 * Peer: Encapsulate another node with metadata useful for our context.
 * <p>
 * We have 4 categories of peers:
 * - Seed nodes (provided to application as hard coded list or application configuration, using PeerExchangeSelection)
 * - Persisted peers (persisted from previous sessions, selected by PeerExchangeSelection)
 * - Reported peers (connected and reported peers delivered from other peers, using PeerExchangeSelection)
 * - Connected peers (actual current connections)
 */
package bisq.network.p2p.services.peer_group;
