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

package network.misq.desktop.main.content;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.api.DefaultApi;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.common.view.View;
import network.misq.desktop.main.content.markets.MarketsController;
import network.misq.desktop.main.content.offerbook.OfferbookController;
import network.misq.desktop.overlay.OverlayController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ContentViewController implements Controller {
    private final DefaultApi api;
    private final OverlayController overlayController;
    private final Map<Class<? extends Controller>, Controller> map = new ConcurrentHashMap<>();
    private ContentViewModel model;
    @Getter
    private ContentView view;

    public ContentViewController(DefaultApi api, OverlayController overlayController) {
        this.api = api;
        this.overlayController = overlayController;
    }

    @Override
    public void initialize() {
        this.model = new ContentViewModel();
        this.view = new ContentView(model, this);

        addController(new MarketsController(api));
        addController(new OfferbookController(api, this, overlayController));
    }

    @Override
    public void onViewAdded() {
    }

    @Override
    public void onViewRemoved() {
    }

    public void onNavigationRequest(Class<? extends Controller> controllerClass) {
        Controller controller = map.get(controllerClass);
        controller.initialize();
        View view = controller.getView();
        model.selectView(view);
    }

    private void addController(Controller controller) {
        map.put(controller.getClass(), controller);
    }
}
