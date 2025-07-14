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

package bisq.http_api.web_socket.domain.reputation;

import bisq.common.observable.Pin;
import bisq.dto.DtoMappings;
import bisq.dto.user.reputation.ReputationScoreDto;
import bisq.http_api.web_socket.domain.SimpleObservableWebSocketService;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import bisq.http_api.web_socket.subscription.Topic;
import bisq.user.reputation.ReputationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ReputationWebSocketService extends SimpleObservableWebSocketService<Map<String, Long>, Map<String, ReputationScoreDto>> {
    private final ReputationService reputationService;

    public ReputationWebSocketService(ObjectMapper objectMapper,
                                      SubscriberRepository subscriberRepository,
                                      ReputationService _reputationService) {
        super(objectMapper, subscriberRepository, Topic.USER_REPUTATION);
        reputationService = _reputationService;
    }

    @Override
    protected Map<String, Long> getObservable() {
        return reputationService.getScoreByUserProfileId();
    }

    @Override
    protected Map<String, ReputationScoreDto> toPayload(Map<String, Long> observable) {
        return getObservable()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> DtoMappings.ReputationScoreMapping.fromBisq2Model(reputationService.getReputationScore(entry.getKey())),
                        (existing, replacement) -> existing,
                        HashMap::new
                ));
    }

    @Override
    protected Pin setupObserver() {
        return reputationService.getUserProfileIdWithScoreChange().addObserver(value -> onChange());
    }
}
