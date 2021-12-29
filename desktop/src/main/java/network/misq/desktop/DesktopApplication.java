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

package network.misq.desktop;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import network.misq.api.DefaultApi;
import network.misq.application.DefaultApplicationSetup;
import network.misq.application.Executable;
import network.misq.application.ApplicationOptions;

@Slf4j
public class DesktopApplication extends Executable<DefaultApplicationSetup> {
    private StageController stageController;
    protected DefaultApi api;

    public DesktopApplication(String[] args) {
        super(args);
    }

    @Override
    protected DefaultApplicationSetup createApplicationSetup(ApplicationOptions applicationOptions, String[] args) {
        return new DefaultApplicationSetup(applicationOptions, args);
    }

    @Override
    protected void createApi() {
        api = new DefaultApi(applicationSetup);
    }

    @Override
    protected void launchApplication() {
        stageController = new StageController(api);
        stageController.launchApplication().whenComplete((success, throwable) -> {
            if (throwable == null) {
                log.info("Java FX Application initialized");
                onApplicationLaunched();
            } else {
                log.warn("Could not launch JavaFX application.", throwable);
            }
        });
    }

    @Override
    protected void onInitializeDomainCompleted() {
        Platform.runLater(stageController::activate);
    }

    @Override
    protected void onInitializeDomainFailed(Throwable throwable) {
        super.onInitializeDomainFailed(throwable);
        stageController.onInitializeDomainFailed();
    }

    @Override
    public void shutdown() {
        if (stageController != null) {
            stageController.shutdown();
        }

        super.shutdown();
    }
}
