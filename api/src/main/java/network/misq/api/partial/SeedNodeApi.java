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

package network.misq.api.partial;

import lombok.Getter;
import network.misq.api.DefaultApplicationFactory;
import network.misq.network.NetworkService;
import network.misq.security.KeyPairRepository;

/**
 * Api for seed node with limited feature set
 */
@Getter
public class SeedNodeApi {
    private final KeyPairRepository keyPairRepository;
    private final NetworkService networkService;

    public SeedNodeApi(DefaultApplicationFactory applicationFactory) {
        keyPairRepository = applicationFactory.getKeyPairRepository();
        networkService = applicationFactory.getNetworkService();
    }
}
