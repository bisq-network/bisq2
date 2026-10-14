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
package bisq.api.rest_api.endpoints.info;

import bisq.api.dto.info.ApiInfoDto;
import bisq.api.rest_api.endpoints.RestApiBase;
import bisq.api.rest_api.endpoints.config.ApiFeature;
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

import java.util.function.UnaryOperator;

/**
 * What a client needs to know about the node before doing anything else: its version, the image
 * version of containerised distributions, and the supported {@link ApiFeature} keys. Static per
 * deployment, global and small, so it is the first request a client makes after pairing.
 * <p>
 * Not public: imageVersion names the exact docker build, so it keeps the SETTINGS guard of
 * {@code /settings/version}, which it replaces.
 */
@Slf4j
@Path("/info")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Info API", description = "Node version and supported API features, for the client handshake")
public class InfoRestApi extends RestApiBase {
    private final UnaryOperator<String> env;

    public InfoRestApi() {
        this(System::getenv);
    }

    // Lets tests supply the environment instead of reading the process's.
    InfoRestApi(UnaryOperator<String> env) {
        this.env = env;
    }

    @GET
    @Operation(
            summary = "Get node info",
            description = "The node's core version, its image version on containerised distributions, and the "
                    + "recent API features it supports as stable kebab-case keys.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Node info retrieved successfully",
                            content = @Content(schema = @Schema(implementation = ApiInfoDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getInfo() {
        try {
            String version = ApplicationVersion.getVersion().toString();
            String imageVersion = ImageVersion.resolve(env).orElse(null);
            return buildOkResponse(new ApiInfoDto(version, imageVersion, ApiFeature.allKeys()));
        } catch (Exception e) {
            log.error("Failed to retrieve node info", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }
}
