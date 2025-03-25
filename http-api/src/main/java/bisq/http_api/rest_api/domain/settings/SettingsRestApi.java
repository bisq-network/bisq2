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

package bisq.http_api.rest_api.domain.settings;

import bisq.common.application.ApplicationVersion;
import bisq.common.observable.collection.ObservableSet;
import bisq.dto.DtoMappings;
import bisq.dto.settings.SettingsApiVersionDto;
import bisq.dto.settings.SettingsDto;
import bisq.http_api.rest_api.domain.RestApiBase;
import bisq.settings.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Settings API", description = "API for managing user settings")
public class SettingsRestApi extends RestApiBase {
    private final SettingsService settingsService;

    public SettingsRestApi(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GET
    @Operation(
            summary = "Get user settings",
            description = "Retrieve the current user settings",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User settings retrieved successfully",
                            content = @Content(schema = @Schema(implementation = SettingsDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getSettings() {
        try {
            SettingsDto settingsDto = DtoMappings.SettingsMapping.fromBisq2Model(settingsService);
            return buildOkResponse(settingsDto);
        } catch (Exception e) {
            log.error("Failed to retrieve user settings", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GET
    @Path("/version")
    @Operation(description = "Get the current Bisq2 version")
    public Response getVersion() {
        try {
            String version = ApplicationVersion.getVersion().toString();
            return Response.ok(new SettingsApiVersionDto(version)).build();
        } catch (Exception e) {
            log.error("Error getting version", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @PATCH
    @Operation(
            summary = "Update a specific setting",
            description = "Allows partial updates to user settings, such as confirming trade rules or accepting terms and conditions.",
            requestBody = @RequestBody(
                    description = "The setting key and value to be updated",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SettingsChangeRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Setting updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response updateSetting(@Valid SettingsChangeRequest request) {
        try {
            if (request.isTacAccepted() != null) {
                settingsService.setIsTacAccepted(request.isTacAccepted());
            } else if (request.tradeRulesConfirmed() != null) {
                settingsService.setTradeRulesConfirmed(request.tradeRulesConfirmed());
            } else if (request.closeMyOfferWhenTaken() != null) {
                settingsService.setCloseMyOfferWhenTaken(request.closeMyOfferWhenTaken());
            } else if (request.languageCode() != null) {
                settingsService.setLanguageCode(request.languageCode());
            } else if (request.supportedLanguageCodes() != null) {
                ObservableSet<String> supportedLanguageCodes = settingsService.getSupportedLanguageCodes();
                supportedLanguageCodes.clear();
                supportedLanguageCodes.addAll(request.supportedLanguageCodes());
            } else if (request.maxTradePriceDeviation() != null) {
                settingsService.setMaxTradePriceDeviation(request.maxTradePriceDeviation());
            } else if (request.selectedMarket() != null) {
                settingsService.setSelectedMarket(DtoMappings.MarketMapping.toBisq2Model(request.selectedMarket()));
            } else if (request.numDaysAfterRedactingTradeData() != null) {
                settingsService.setNumDaysAfterRedactingTradeData(request.numDaysAfterRedactingTradeData());
            } else if (request.useAnimations() != null) {
                settingsService.setUseAnimations(request.useAnimations());
            } else {
                return buildErrorResponse(Response.Status.BAD_REQUEST, "Invalid request: " + request);
            }

            log.info("Updated setting from request: {}", request);
            return Response.noContent().build();
        } catch (Exception e) {
            log.error("Error updating setting", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }
}