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

package bisq.api.web_socket.domain.alert_notifications;

import bisq.api.dto.alert.AuthorizedAlertDataDto;
import bisq.api.dto.mappings.alert.AuthorizedAlertDataDtoMapping;
import bisq.api.util.AppTypeParser;
import bisq.api.web_socket.domain.SimpleObservableWebSocketService;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionRequest;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertDataUtils;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableSet;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static bisq.api.web_socket.subscription.Topic.ALERT_NOTIFICATIONS;

@Slf4j
public class AlertNotificationsWebSocketService extends SimpleObservableWebSocketService<ObservableSet<AuthorizedAlertData>, List<AuthorizedAlertDataDto>> {
    private final AlertNotificationsService alertNotificationsService;

    public AlertNotificationsWebSocketService(SubscriberRepository subscriberRepository,
                                              AlertNotificationsService alertNotificationsService) {
        super(subscriberRepository, ALERT_NOTIFICATIONS);
        this.alertNotificationsService = alertNotificationsService;
    }

    @Override
    protected Pin setupObserver() {
        return getObservable().addObserver(this::onChange);
    }

    @Override
    protected List<AuthorizedAlertDataDto> toPayload(ObservableSet<AuthorizedAlertData> observable) {
        return getPayload(AppType.UNSPECIFIED)
                .filter(AuthorizedAlertDataDtoMapping::canRepresent)
                .sorted(AuthorizedAlertDataUtils.RELEVANCE_COMPARATOR.reversed())
                .map(AuthorizedAlertDataDtoMapping::fromBisq2Model)
                .toList();
    }

    @Override
    public void validate(SubscriptionRequest request) {
        AppTypeParser.parse(request.getParameter());
    }

    @Override
    public Optional<String> canonicalizeParameter(Optional<String> parameter) {
        return Optional.of(AppTypeParser.parse(parameter).name());
    }

    @Override
    public Optional<String> getJsonPayload(Optional<String> parameter) {
        AppType appType = AppTypeParser.parse(parameter);
        return toJson(buildAlertList(appType));
    }

    private List<AuthorizedAlertDataDto> buildAlertList(AppType appType) {
        return getPayload(appType)
                .filter(AuthorizedAlertDataDtoMapping::canRepresent)
                .sorted(AuthorizedAlertDataUtils.RELEVANCE_COMPARATOR.reversed())
                .map(AuthorizedAlertDataDtoMapping::fromBisq2Model)
                .toList();
    }

    @Override
    protected ObservableSet<AuthorizedAlertData> getObservable() {
        return alertNotificationsService.getUnconsumedAlerts();
    }

    private Stream<AuthorizedAlertData> getPayload(AppType appType) {
        return alertNotificationsService.getUnconsumedAlertsByAppType(appType);
    }
}