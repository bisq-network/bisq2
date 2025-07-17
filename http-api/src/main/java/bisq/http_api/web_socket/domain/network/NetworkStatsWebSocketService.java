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

package bisq.http_api.web_socket.domain.network;


import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.http_api.rest_api.domain.network.NetworkStatsResponse;
import bisq.http_api.web_socket.domain.SimpleObservableWebSocketService;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import bisq.http_api.web_socket.subscription.Topic;
import bisq.user.UserService;
import bisq.user.profile.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;



@Slf4j
public class NetworkStatsWebSocketService extends SimpleObservableWebSocketService<Observable<Integer>, NetworkStatsResponse> {
    private final UserProfileService userProfileService;

    public NetworkStatsWebSocketService(ObjectMapper objectMapper,
                                        SubscriberRepository subscriberRepository,
                                        UserService userService) {
        super(objectMapper, subscriberRepository, Topic.NETWORK_STATS);
        this.userProfileService = userService.getUserProfileService();
    }

    @Override
    protected Observable<Integer> getObservable() {
        return userProfileService.getNumUserProfiles();
    }

    @Override
    protected NetworkStatsResponse toPayload(Observable<Integer> observable) {
        try {
            int totalPublishedProfiles = observable.get();
            return new NetworkStatsResponse(totalPublishedProfiles);
        } catch (Exception e) {
            log.error("Error creating network stats payload", e);
            return new NetworkStatsResponse(0);
        }
    }

    @Override
    protected Pin setupObserver() {
        // Primary observer for user profiles count changes
        return userProfileService.getNumUserProfiles().addObserver(numProfiles -> {
            log.debug("User profiles count changed to: {}", numProfiles);
            onChange();
        });
    }


}
