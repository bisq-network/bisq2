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
}
