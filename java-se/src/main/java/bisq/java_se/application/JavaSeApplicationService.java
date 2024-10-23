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

package bisq.java_se.application;

import bisq.application.ApplicationService;
import bisq.common.platform.MemoryReportService;
import bisq.common.platform.PlatformUtils;
import bisq.evolution.migration.MigrationService;
import bisq.java_se.jvm.JvmMemoryReportService;
import bisq.java_se.guava.GuavaJavaSeFacade;
import bisq.security.pow.equihash.Equihash;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public abstract class JavaSeApplicationService extends ApplicationService {
    protected final MigrationService migrationService;
    protected final MemoryReportService memoryReportService;

    public JavaSeApplicationService(String configFileName, String[] args) {
        super(configFileName, args, PlatformUtils.getUserDataDir());

        // Guava has different APIs for Java SE and Android.
        // Thus, we use a facade with Android compatible APIs by default and let the Desktop app set the Java SE facade
        // containing APIs only supported for Java SE compatible JDKs.
        Equihash.setGuavaFacade(new GuavaJavaSeFacade());

        migrationService = new MigrationService(getConfig().getBaseDir());
        memoryReportService = new JvmMemoryReportService(getConfig().getMemoryReportIntervalSec(), getConfig().isIncludeThreadListInMemoryReport());
    }
}
