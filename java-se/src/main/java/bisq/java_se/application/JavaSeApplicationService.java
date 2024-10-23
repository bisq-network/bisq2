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
import bisq.java_se.facades.JavaSeJdkFacade;
import bisq.java_se.jvm.JvmMemoryReportService;
import bisq.java_se.facades.JavaSeGuavaFacade;
import bisq.security.pow.equihash.Equihash;
import bisq.tor.TorService;
import bisq.tor.process.NativeTorProcess;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public abstract class JavaSeApplicationService extends ApplicationService {
    protected final MigrationService migrationService;
    protected final MemoryReportService memoryReportService;

    public JavaSeApplicationService(String configFileName, String[] args) {
        super(configFileName, args, PlatformUtils.getUserDataDir());

        // The JDKs for Java SE and Android have different API support, thus, we use a
        // facade with Android compatible APIs by default and set for Java SE based applicationServices
        // the Java SE facade.
        Equihash.setGuavaFacade(new JavaSeGuavaFacade());
        NativeTorProcess.setJdkFacade(new JavaSeJdkFacade());
        TorService.setJdkFacade(new JavaSeJdkFacade());

        migrationService = new MigrationService(getConfig().getBaseDir());
        memoryReportService = new JvmMemoryReportService(getConfig().getMemoryReportIntervalSec(), getConfig().isIncludeThreadListInMemoryReport());
    }
}
