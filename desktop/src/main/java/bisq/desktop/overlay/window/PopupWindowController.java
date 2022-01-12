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

package bisq.desktop.overlay.window;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.common.view.Controller;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PopupWindowController implements Controller {
    private final PopupWindowModel model;
    @Getter
    private final PopupWindowView view;
    private final DefaultServiceProvider serviceProvider;

    public PopupWindowController(DefaultServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        model = new PopupWindowModel(serviceProvider);
        view = new PopupWindowView(model, this);
    }

    @Override
    public void setData(Object data) {
        if (data != null) {
            log.info(data.toString()); //todo
        }
    }

    @Override
    public void onViewAttached() {
    }

    @Override
    public void onViewDetached() {
    }
}
