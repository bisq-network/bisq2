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

package bisq.rest_api;

import bisq.application.Executable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestApiApp extends Executable<RestApiApplicationService> {
    public static void main(String[] args) {
        new RestApiApp(args);
    }

    private JaxRsApplication jaxRsApplication;

    public RestApiApp(String[] args) {
        super(args);
    }

    @Override
    protected void launchApplication(String[] args) {
        jaxRsApplication = new JaxRsApplication(args, () -> applicationService);

        super.launchApplication(args);
    }

    @Override
    protected void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
        jaxRsApplication.initialize();
    }

    @Override
    protected RestApiApplicationService createApplicationService(String[] args) {
        return new RestApiApplicationService(args);
    }

    @Override
    public void shutdown() {
        if (jaxRsApplication != null) {
            jaxRsApplication.shutdown();
        }

        super.shutdown();
    }
}
