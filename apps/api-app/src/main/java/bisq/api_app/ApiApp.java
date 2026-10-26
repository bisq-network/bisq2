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

package bisq.api_app;

import bisq.api.ApiService;
import bisq.api.HttpServerBootstrapService;
import bisq.application.Executable;
import lombok.extern.slf4j.Slf4j;

/**
 * JAX-RS application for the Bisq REST API
 * Swagger docs at: http://localhost:8090/doc/v1/index.html if rest is enabled
 * is used without websockets
 */
@Slf4j
public class ApiApp extends Executable<ApiApplicationService> {
    public static void main(String[] args) {
        Thread.currentThread().setName("ApiApp.main");
        new ApiApp(args);
    }

    public ApiApp(String[] args) {
        super(args);
    }

    @Override
    protected ApiApplicationService createApplicationService(String[] args) {
        return new ApiApplicationService(args);
    }

    @Override
    protected void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
        if (throwable == null && Boolean.TRUE.equals(result) && isApiServerRunning()) {
            return;
        }
        // Without exiting we would keep running without the API, and a supervisor like Docker would never restart us.
        // A cause that persists (e.g. the port is used by another process) leads to a restart loop, in which every
        // restart bootstraps Tor and connects to the seed nodes again. We accept that over a node which looks alive
        // but cannot be used.
        // TODO: Check that the API server port is free before the network initialization, so that this failure
        //  exits fast and does not repeat the network startup on every restart.
        String httpServerError = applicationService.getApiService().getHttpServerBootstrapService().getErrorMessage().get();
        log.error("Initialization failed. We shut down with a failure exit code. Error: {}; HTTP server error: {}",
                throwable != null ? throwable.toString() : "none",
                httpServerError != null ? httpServerError : "none");
        shutdownWithFailure();
    }

    // HttpServerBootstrapService does not fail the startup if the server cannot start (the desktop app shows the
    // error instead), so we check its state.
    private boolean isApiServerRunning() {
        ApiService apiService = applicationService.getApiService();
        return !apiService.getApiConfig().isEnabled() ||
                apiService.getHttpServerBootstrapService().getState().get() == HttpServerBootstrapService.State.RUNNING;
    }
}
