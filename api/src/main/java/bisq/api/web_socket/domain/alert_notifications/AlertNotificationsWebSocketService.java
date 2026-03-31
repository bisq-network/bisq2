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
import bisq.api.web_socket.domain.SimpleObservableWebSocketService;
import bisq.api.web_socket.subscription.Subscriber;
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
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static bisq.api.web_socket.subscription.Topic.ALERT_NOTIFICATIONS;

@Slf4j
public class AlertNotificationsWebSocketService extends SimpleObservableWebSocketService<ObservableSet<AuthorizedAlertData>, List<AuthorizedAlertDataDto>> {
    private static final AppType DEFAULT_APP_TYPE = AppType.MOBILE_CLIENT;

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
        return getPayload(DEFAULT_APP_TYPE)
                .sorted(AuthorizedAlertDataUtils.RELEVANCE_COMPARATOR.reversed())
            .filter(AuthorizedAlertDataDtoMapping::canRepresent)
            .map(AuthorizedAlertDataDtoMapping::fromBisq2Model)
                .toList();
    }

    @Override
    public void validate(SubscriptionRequest request) {
        parseAppType(Optional.ofNullable(request.getParameter()));
    }

    @Override
    protected boolean usesSubscriberSpecificPayload() {
        return true;
    }

    @Override
    public Optional<String> getJsonPayload(Subscriber subscriber) {
        return getJsonPayloadForParameter(subscriber.getParameter());
    }

    @Override
    public Optional<String> getJsonPayload(SubscriptionRequest request) {
        return getJsonPayloadForParameter(Optional.ofNullable(request.getParameter()));
    }

    @Override
    protected ObservableSet<AuthorizedAlertData> getObservable() {
        return alertNotificationsService.getUnconsumedAlerts();
    }

    private Optional<String> getJsonPayloadForParameter(Optional<String> parameter) {
        AppType appType = parseAppType(parameter);
        return toJson(getPayload(appType)
                .sorted(AuthorizedAlertDataUtils.RELEVANCE_COMPARATOR.reversed())
            .filter(AuthorizedAlertDataDtoMapping::canRepresent)
            .map(AuthorizedAlertDataDtoMapping::fromBisq2Model)
                .toList());
    }

    private Stream<AuthorizedAlertData> getPayload(AppType appType) {
        return alertNotificationsService.getUnconsumedAlertsByAppType(appType);
    }

    private AppType parseAppType(Optional<String> appTypeParam) {
        String normalizedValue = appTypeParam
                .filter(value -> !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .orElse(DEFAULT_APP_TYPE.name());
        try {
            return AppType.valueOf(normalizedValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid appType: " + appTypeParam.orElse(null));
        }
    }
}