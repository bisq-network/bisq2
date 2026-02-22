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

package bisq.api.rest_api.endpoints.explorer;

import bisq.bonded_roles.explorer.ExplorerService;
import bisq.bonded_roles.explorer.dto.Tx;
import bisq.common.json.JsonMapperProvider;
import bisq.common.util.ExceptionUtil;
import bisq.api.rest_api.endpoints.RestApiBase;
import bisq.api.rest_api.endpoints.market_price.QuotesResponse;
import bisq.network.http.HttpRequestUrlProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Path("/explorer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Blockchain Explorer API", description = "Provides endpoints to interact with the blockchain explorer service.")
public class ExplorerRestApi extends RestApiBase {
    private final ExplorerService explorerService;

    public ExplorerRestApi(ExplorerService explorerService) {
        this.explorerService = explorerService;
    }

    /**
     * Retrieves the currently selected blockchain explorer provider.
     *
     * @return Response containing the base URL of the selected provider or a not found error.
     */
    @GET
    @Path("/selected")
    @Operation(
            summary = "Get Selected Explorer Provider",
            description = "Returns the base URL of the currently selected blockchain explorer provider.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved the selected provider's base URL.",
                            content = @Content(schema = @Schema(implementation = QuotesResponse.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "No provider found."),
                    @ApiResponse(responseCode = "500", description = "Internal server error occurred while processing the request.")
            }
    )
    public Response getSelected() {
        try {
            HttpRequestUrlProvider provider = explorerService.getSelectedProvider().get();
            if (provider == null) {
                return buildNotFoundResponse("No provider found.");
            }
            return buildOkResponse(Map.of("provider", provider.getBaseUrl()));
        } catch (Exception e) {
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Retrieves transaction details from the blockchain explorer.
     *
     * @param txId The transaction ID to fetch details for.
     * @param asyncResponse The asynchronous response to be returned to the client.
     */
    @GET
    @Path("/tx/{txId}")
    @Operation(
            summary = "Get Transaction Details",
            description = "Fetches details of a specific transaction from the blockchain explorer using the given transaction ID.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Transaction details retrieved successfully.",
                            content = @Content(schema = @Schema(implementation = ExplorerTxDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error occurred while processing the request."
                    )
            }
    )
    public void getTx(@PathParam("txId") String txId, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(30, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            Tx tx = explorerService.requestTx(txId).get();
            List<ExplorerOutputDto> outputs = tx.getOutputs().stream().map(o -> new ExplorerOutputDto(o.getAddress(), o.getValue())).collect(Collectors.toList());
            ExplorerTxDto explorerTxDto = new ExplorerTxDto(txId, tx.getStatus().isConfirmed(), outputs);
            log.info("Explorer request result: {}. json={}", explorerTxDto, JsonMapperProvider.get().writeValueAsString(explorerTxDto));
            asyncResponse.resume(buildResponse(Response.Status.OK, explorerTxDto));
        } catch (InterruptedException e) {
            log.warn("Thread got interrupted at getTx method", e);
            Thread.currentThread().interrupt(); // Restore interrupted state
            asyncResponse.resume(buildErrorResponse("Thread was interrupted."));
        } catch (Exception e) {
            Throwable rootCause = ExceptionUtil.getRootCause(e);
            String errorMessage = ExceptionUtil.getRootCauseMessage(rootCause);
            log.info("Explorer request transaction id {} failed with {}", txId, errorMessage);
            asyncResponse.resume(buildErrorResponse(errorMessage));
        }
    }
}
