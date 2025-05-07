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

package bisq.desktop.main.content.mu_sig.offerbook.btc;

import bisq.desktop.ServiceProvider;
import bisq.desktop.main.content.mu_sig.offerbook.MuSigOfferbookController;
import bisq.offer.Direction;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigOfferbookBtcController extends MuSigOfferbookController<MuSigOfferbookBtcModel, MuSigOfferbookBtcView> {
    public MuSigOfferbookBtcController(ServiceProvider serviceProvider, Direction direction) {
        super(serviceProvider, direction);
    }

    @Override
    protected MuSigOfferbookBtcView createAndGetView() {
        return new MuSigOfferbookBtcView(model, this);
    }

    @Override
    protected MuSigOfferbookBtcModel createAndGetModel(Direction direction) {
        return new MuSigOfferbookBtcModel(direction);
    }
}