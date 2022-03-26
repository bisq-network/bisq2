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

package bisq.application;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestApplicationDistributedDataProtoResolver {
    @Test
    public void testApplicationProtoResolver() {
       /* ProtoResolverRepository resolver = new MockResolver();
        Ping ping = new Ping(42);
        Any proto = Any.pack(ping.toNetworkMessageProto().getPing());
        NetworkMessage networkMessage = NetworkMessage.fromAny(proto);
        assertTrue(networkMessage instanceof Ping);
        Ping resolved = (Ping) networkMessage;
        assertEquals(ping, resolved);*/
        
        //todo add data from other modules
    }

/*
  private static class MockResolver implements ProtoResolverRepository {
        public NetworkMessage fromAnyToNetworkMessage(Any any) throws UnresolvableProtobufMessageException {
            ProtoResolverRepository.ProtoPackageAndMessageName protoPackageAndMessageName = ProtoResolverRepository.getProtoPackageAndMessageName(any);
            // We do not use reflection for security reasons
            String protoPackage = protoPackageAndMessageName.protoPackage();
            String protoMessageName = protoPackageAndMessageName.protoMessageName();
            try {
                if (protoPackage.equals("network")) {
                    if (protoMessageName.equals("Ping")) {
                        bisq.network.protobuf.Ping unpack = any.unpack(bisq.network.protobuf.Ping.class);
                        Ping ping = Ping.fromProto(unpack);
                        return ping;
                    }
                }
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }

            throw new UnresolvableProtobufMessageException(any);
        }

      @Override
      public DistributedData fromAnyToDistributedData(Any proto) {
          return null;
      }
  }*/
}