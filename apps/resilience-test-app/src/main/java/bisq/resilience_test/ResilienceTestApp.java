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

package bisq.resilience_test;

import bisq.application.Executable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResilienceTestApp extends Executable<ResilienceTestApplicationService> {
    public static void main(String[] args) {
        Thread.currentThread().setName("ResilienceTestApp.main");
        new ResilienceTestApp(args);
    }

    public ResilienceTestApp(String[] args) {
        super(args);
    }

    @Override
    protected ResilienceTestApplicationService createApplicationService(String[] args) {
        return new ResilienceTestApplicationService(args);
    }

    @Override
    protected void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
        if (throwable != null) {
            shutdown();
            // shutdown fails to call exitJvm when stopping early
            // hence the explicit call to exitJvm
            exitJvm();
        }
    }
}

