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

package bisq.api.rest_api.endpoints.alert_notifications;

import bisq.api.dto.alert.AuthorizedAlertDataDto;
import bisq.api.dto.mappings.alert.AuthorizedAlertDataDtoMapping;
import bisq.api.rest_api.endpoints.RestApiBase;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertDataUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Path("/alert-notifications")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Alert Notifications API", description = "API for retrieving and dismissing visible alert notifications")
public class AlertNotificationsRestApi extends RestApiBase {
    private static final AppType DEFAULT_APP_TYPE = AppType.MOBILE_CLIENT;

    private final AlertNotificationsService alertNotificationsService;

    public AlertNotificationsRestApi(AlertNotificationsService alertNotificationsService) {
        this.alertNotificationsService = alertNotificationsService;
    }

    @GET
    @Operation(
            summary = "Get visible alert notifications",
            description = "Returns the currently visible, undismissed alert notifications in display order.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Alert notifications retrieved successfully",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuthorizedAlertDataDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Bad request - invalid appType"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getAlertNotifications(@QueryParam("appType") @DefaultValue("MOBILE_CLIENT") String appTypeParam) {
        try {
            AppType appType = parseAppType(appTypeParam);
            return buildOkResponse(getSortedAlertNotifications(appType));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(Response.Status.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error retrieving alert notifications", e);
            return buildErrorResponse("An unexpected error occurred");
        }
    }

    @DELETE
    @Path("/{alertId}")
    @Operation(
            summary = "Dismiss a visible alert notification",
            description = "Dismisses a currently visible alert notification for the local client state.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Alert notification dismissed successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad request - invalid appType"),
                    @ApiResponse(responseCode = "404", description = "Alert notification not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response dismissAlert(@PathParam("alertId") String alertId,
                                 @QueryParam("appType") @DefaultValue("MOBILE_CLIENT") String appTypeParam) {
        try {
            AppType appType = parseAppType(appTypeParam);
            Optional<AuthorizedAlertData> authorizedAlertData = alertNotificationsService.getUnconsumedAlertsByAppType(appType)
                    .filter(AuthorizedAlertDataDtoMapping::canRepresent)
                    .filter(alert -> alert.getId().equals(alertId))
                    .findFirst();
            if (authorizedAlertData.isEmpty()) {
                return buildNotFoundResponse("Alert notification not found with ID: " + alertId);
            }

            alertNotificationsService.dismissAlert(authorizedAlertData.get());
            return buildNoContentResponse();
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(Response.Status.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error dismissing alert notification {}", alertId, e);
            return buildErrorResponse("An unexpected error occurred");
        }
    }

    private List<AuthorizedAlertDataDto> getSortedAlertNotifications(AppType appType) {
        return alertNotificationsService.getUnconsumedAlertsByAppType(appType)
                .sorted(AuthorizedAlertDataUtils.RELEVANCE_COMPARATOR.reversed())
                .filter(AuthorizedAlertDataDtoMapping::canRepresent)
                .map(AuthorizedAlertDataDtoMapping::fromBisq2Model)
                .toList();
    }

    private AppType parseAppType(String appTypeParam) {
        String normalizedValue = appTypeParam == null || appTypeParam.isBlank()
                ? DEFAULT_APP_TYPE.name()
                : appTypeParam.trim().toUpperCase(Locale.ROOT);
        try {
            return AppType.valueOf(normalizedValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid appType: " + appTypeParam);
        }
    }
}