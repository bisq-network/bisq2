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

package bisq.api.web_socket.domain.trade_restricting_alert;

import bisq.api.dto.alert.AuthorizedAlertDataDto;
import bisq.api.dto.mappings.alert.AuthorizedAlertDataDtoMapping;
import bisq.api.util.AppTypeParser;
import bisq.api.web_socket.domain.SimpleObservableWebSocketService;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionRequest;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertDataUtils;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableSet;

import java.util.Optional;

import static bisq.api.web_socket.subscription.Topic.TRADE_RESTRICTING_ALERT;

public class TradeRestrictingAlertWebSocketService extends SimpleObservableWebSocketService<ObservableSet<AuthorizedAlertData>, Optional<AuthorizedAlertDataDto>> {
    private final AlertService alertService;

    public TradeRestrictingAlertWebSocketService(SubscriberRepository subscriberRepository,
                                                 AlertService alertService) {
        super(subscriberRepository, TRADE_RESTRICTING_ALERT);
        this.alertService = alertService;
    }

    @Override
    protected Pin setupObserver() {
        return getObservable().addObserver(this::onChange);
    }

    @Override
    protected Optional<AuthorizedAlertDataDto> toPayload(ObservableSet<AuthorizedAlertData> observable) {
        throw new UnsupportedOperationException("appType parameter is required");
    }

    @Override
    public Optional<String> getJsonPayload() {
        throw new UnsupportedOperationException("appType parameter is required");
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
        return getPayload(AppTypeParser.parse(parameter)).flatMap(this::toJson);
    }

    @Override
    protected ObservableSet<AuthorizedAlertData> getObservable() {
        return alertService.getAuthorizedAlertDataSet();
    }

    private Optional<AuthorizedAlertDataDto> getPayload(AppType appType) {
        return AuthorizedAlertDataUtils
                .findMostRecentTradeRestrictingAlert(alertService.getAuthorizedAlertDataSet().stream(), appType)
                .map(AuthorizedAlertDataDtoMapping::fromBisq2Model);
    }
}