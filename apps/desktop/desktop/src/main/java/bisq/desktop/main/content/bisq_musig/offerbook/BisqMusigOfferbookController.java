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

package bisq.desktop.main.content.bisq_musig.offerbook;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannelDomain;
import bisq.desktop.ServiceProvider;
import bisq.desktop.main.content.chat.ChatController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BisqMusigOfferbookController extends ChatController<BisqMusigOfferbookView, BisqMusigOfferbookModel> {
    private final BisqMusigOfferbookModel bisqMusigOfferbookModel;

    public BisqMusigOfferbookController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_MUSIG_OFFERBOOK, NavigationTarget.BISQ_MUSIG_OFFERBOOK);

        bisqMusigOfferbookModel = getModel();
    }

    @Override
    public BisqMusigOfferbookModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return null;
    }

    @Override
    public BisqMusigOfferbookView createAndGetView() {
        return null;
    }

    @Override
    public void onActivate() {
        super.onActivate();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
    }
}
