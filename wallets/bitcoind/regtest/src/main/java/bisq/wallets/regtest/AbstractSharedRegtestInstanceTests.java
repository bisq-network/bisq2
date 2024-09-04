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

package bisq.wallets.regtest;

import bisq.wallets.regtest.process.BisqProcess;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractSharedRegtestInstanceTests<T extends BisqProcess, W> {
    private AbstractRegtestSetup<T> regtestSetup;

    @BeforeAll
    public void start() throws IOException, InterruptedException {
        regtestSetup = createRegtestSetup();
        regtestSetup.start();
    }

    @AfterAll
    public void stop() {
        regtestSetup.shutdown();
    }

    public abstract AbstractRegtestSetup<T> createRegtestSetup() throws IOException;
}
