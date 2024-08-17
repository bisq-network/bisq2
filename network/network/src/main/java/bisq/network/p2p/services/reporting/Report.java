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

package bisq.network.p2p.services.reporting;

import bisq.common.proto.NetworkProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@EqualsAndHashCode
@ToString
public final class Report implements NetworkProto {
    private final TreeMap<String, Integer> authorizedDataPerClassName;
    private final TreeMap<String, Integer> authenticatedDataPerClassName;
    private final TreeMap<String, Integer> mailboxDataPerClassName;
    private final int numConnections;
    private final int memoryUsed;
    private final int numThreads;
    private final double nodeLoad;


    public Report(TreeMap<String, Integer> authorizedDataPerClassName,
                  TreeMap<String, Integer> authenticatedDataPerClassName,
                  TreeMap<String, Integer> mailboxDataPerClassName,
                  int numConnections,
                  int memoryUsed,
                  int numThreads,
                  double nodeLoad) {
        this.authorizedDataPerClassName = authorizedDataPerClassName;
        this.authenticatedDataPerClassName = authenticatedDataPerClassName;
        this.mailboxDataPerClassName = mailboxDataPerClassName;
        this.numConnections = numConnections;
        this.memoryUsed = memoryUsed;
        this.numThreads = numThreads;
        this.nodeLoad = nodeLoad;
        verify();
    }

    @Override
    public void verify() {
        checkArgument(authorizedDataPerClassName.size() < 10000);
        checkArgument(authenticatedDataPerClassName.size() < 10000);
        checkArgument(mailboxDataPerClassName.size() < 10000);
    }

    @Override
    public bisq.network.protobuf.Report toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.Report.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.Report.newBuilder()
                .putAllAuthorizedDataPerClassName(authorizedDataPerClassName)
                .putAllAuthenticatedDataPerClassName(authenticatedDataPerClassName)
                .putAllMailboxDataPerClassName(mailboxDataPerClassName)
                .setNumConnections(numConnections)
                .setMemoryUsed(memoryUsed)
                .setNumThreads(numThreads)
                .setNodeLoad(nodeLoad);
    }

    public static Report fromProto(bisq.network.protobuf.Report proto) {
        return new Report(new TreeMap<>(proto.getAuthorizedDataPerClassNameMap()),
                new TreeMap<>(proto.getAuthenticatedDataPerClassNameMap()),
                new TreeMap<>(proto.getMailboxDataPerClassNameMap()),
                proto.getNumConnections(),
                proto.getMemoryUsed(),
                proto.getNumThreads(),
                proto.getNodeLoad());
    }
}