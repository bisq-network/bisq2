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

package bisq.api.web_socket.domain.release_notifications;

import bisq.api.dto.mappings.release.ReleaseNotificationDtoMapping;
import bisq.api.dto.release.ReleaseNotificationDto;
import bisq.api.util.AppTypeParser;
import bisq.api.web_socket.domain.SimpleObservableWebSocketService;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionRequest;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.release.ReleaseNotification;
import bisq.bonded_roles.release.ReleaseNotificationsService;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableSet;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static bisq.api.web_socket.subscription.Topic.RELEASE_NOTIFICATIONS;

/**
 * Pre-releases are not filtered here: the DTO carries {@code isPreRelease} and the client decides.
 */
@Slf4j
public class ReleaseNotificationsWebSocketService extends SimpleObservableWebSocketService<ObservableSet<ReleaseNotification>, List<ReleaseNotificationDto>> {
    private static final Comparator<ReleaseNotification> NEWEST_FIRST =
            Comparator.comparing(ReleaseNotification::getReleaseVersion).reversed();

    private final ReleaseNotificationsService releaseNotificationsService;

    public ReleaseNotificationsWebSocketService(SubscriberRepository subscriberRepository,
                                                ReleaseNotificationsService releaseNotificationsService) {
        super(subscriberRepository, RELEASE_NOTIFICATIONS);
        this.releaseNotificationsService = releaseNotificationsService;
    }

    @Override
    protected Pin setupObserver() {
        return getObservable().addObserver(this::onChange);
    }

    @Override
    protected List<ReleaseNotificationDto> toPayload(ObservableSet<ReleaseNotification> observable) {
        return toNewestFirstDtos(observable.stream());
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
        // Strict equality, the rule desktop's UpdaterService applies. Unlike alerts, UNSPECIFIED is
        // not a wildcard: a release built for one app type is no update for another.
        return toJson(toNewestFirstDtos(getObservable().stream()
                .filter(notification -> notification.getAppType() == appType)));
    }

    @Override
    protected ObservableSet<ReleaseNotification> getObservable() {
        return releaseNotificationsService.getReleaseNotifications();
    }

    private static List<ReleaseNotificationDto> toNewestFirstDtos(Stream<ReleaseNotification> notifications) {
        return notifications
                .sorted(NEWEST_FIRST)
                .map(ReleaseNotificationDtoMapping::fromBisq2Model)
                .toList();
    }
}
