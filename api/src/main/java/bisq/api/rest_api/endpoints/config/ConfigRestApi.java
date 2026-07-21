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

package bisq.api.rest_api.endpoints.config;

import bisq.api.dto.DtoMappings;
import bisq.api.dto.config.ApiCapabilitiesDto;
import bisq.api.dto.config.TradeAmountLimitsDto;
import bisq.api.rest_api.endpoints.RestApiBase;
import bisq.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.common.application.ApplicationVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * Serves static bisq2 configuration that clients would otherwise have to hardcode.
 * <p>
 * SCOPE CONTRACT — only expose values here that are ALL of:
 * <ul>
 *     <li>static: they change only between bisq2 core/api versions, never per user or at runtime;</li>
 *     <li>global: identical for every peer on the network;</li>
 *     <li>small: cheap enough for a client to fetch once and cache.</li>
 * </ul>
 * Anything user-specific, runtime-mutable, large, or that belongs to a single feature's flow does
 * NOT go here — give it its own feature endpoint. Keeping this endpoint honest avoids turning it
 * into a kitchen-sink blob that couples unrelated features' release cadence and cache invalidation.
 */
@Slf4j
@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Config API", description = "Static, version-global bisq2 configuration for clients")
public class ConfigRestApi extends RestApiBase {

    @GET
    @Path("/trade-amount-limits")
    @Operation(
            summary = "Get Bisq Easy trade-amount limits",
            description = "Static min/max USD trade amounts, tolerance and required reputation-per-USD, "
                    + "mirrored from bisq2 core. Fixed per core/api version.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Trade-amount limits retrieved successfully",
                            content = @Content(schema = @Schema(implementation = TradeAmountLimitsDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getTradeAmountLimits() {
        try {
            TradeAmountLimitsDto dto = new TradeAmountLimitsDto(
                    DtoMappings.FiatMapping.fromBisq2Model(BisqEasyTradeAmountLimits.DEFAULT_MIN_USD_TRADE_AMOUNT),
                    DtoMappings.FiatMapping.fromBisq2Model(BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT),
                    BisqEasyTradeAmountLimits.TOLERANCE,
                    BisqEasyTradeAmountLimits.getRequiredReputationScorePerUsd());
            return buildOkResponse(dto);
        } catch (Exception e) {
            log.error("Failed to retrieve trade-amount limits", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Returns the recent API features this node supports (see {@link ApiFeature}), so a client can
     * enable or hide feature UI for the node it is paired with.
     * <p>
     * How clients use it: fetch once, cache keyed by {@code apiVersion}, and gate each feature on its
     * key being present. The crucial property is the ABSENCE case — a node too old to have this
     * endpoint returns 404, which the client reads as "none of these recent features are supported"
     * and hides them all. So adding a feature to a client only needs a key here; no client-side
     * version checks.
     */
    @GET
    @Path("/capabilities")
    @Operation(
            summary = "Get supported API features",
            description = "The recent API features this node supports, as stable kebab-case keys, plus the "
                    + "node's API version. A node without this endpoint (older node / Bisq Desktop) is treated "
                    + "by clients as supporting none of them.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Supported features retrieved successfully",
                            content = @Content(schema = @Schema(implementation = ApiCapabilitiesDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getCapabilities() {
        try {
            String apiVersion = ApplicationVersion.getVersion().toString();
            return buildOkResponse(new ApiCapabilitiesDto(apiVersion, ApiFeature.allKeys()));
        } catch (Exception e) {
            log.error("Failed to retrieve API capabilities", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }
}
