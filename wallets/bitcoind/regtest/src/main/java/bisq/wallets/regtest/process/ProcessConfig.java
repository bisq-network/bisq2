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

package bisq.wallets.regtest.process;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public final class ProcessConfig {
    private final String name;
    private final List<String> args;
    private final Map<String, String> environmentVars;

    public ProcessConfig(String name, List<String> args, Map<String, String> environmentVars) {
        this.name = name;
        this.args = args;
        this.environmentVars = environmentVars;
    }

    public List<String> toCommandList() {
        List<String> commands = new ArrayList<>();
        commands.add(name);
        commands.addAll(args);
        return commands;
    }
}
