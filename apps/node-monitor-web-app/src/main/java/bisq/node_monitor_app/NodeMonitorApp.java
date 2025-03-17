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

package bisq.node_monitor_app;

import bisq.application.Executable;
import bisq.common.threading.ThreadName;
import bisq.http_api.rest_api.util.StaticFileHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * JAX-RS application for the Bisq REST API
 * Swagger docs at: http://localhost:8082/doc/v1/index.html
 */
@Slf4j
public class NodeMonitorApp extends Executable<NodeMonitorApplicationService> {
    public static void main(String[] args) {
        ThreadName.set(NodeMonitorApp.class, "main");
        new NodeMonitorApp(args);
    }

    public NodeMonitorApp(String[] args) {
        super(args);
    }

    @Override
    protected void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
        applicationService.getRestApiService()
                .ifPresent(e -> e.addStaticFileHandler("/node-monitor",
                        new StaticFileHandler("/node-monitor/")));
    }

    @Override
    protected NodeMonitorApplicationService createApplicationService(String[] args) {
        return new NodeMonitorApplicationService(args);
    }
}
