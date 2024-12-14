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

package bisq.http_api.dto;

import bisq.common.encoding.Hex;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.http_api.dto.bisq.common.network.AddressByTransportTypeMapDto;
import bisq.http_api.dto.bisq.common.network.AddressDto;
import bisq.http_api.dto.bisq.common.network.TransportTypeDto;
import bisq.http_api.dto.bisq.network.identity.NetworkIdDto;
import bisq.http_api.dto.bisq.security.keys.PubKeyDto;
import bisq.http_api.dto.java.security.PublicKey.PublicKeyDto;
import bisq.network.identity.NetworkId;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;

import java.security.PublicKey;
import java.util.Base64;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class DtoMapping {
    public static class PublicKeyMapping {
        public static PublicKey fromDto(PublicKeyDto dto) {
            try {
                byte[] bytes = Base64.getDecoder().decode(dto.encoded());
                return KeyGeneration.generatePublic(bytes);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize publicKey", e);
            }
        }

        public static PublicKeyDto toDto(PublicKey value) {
            return new PublicKeyDto(Base64.getEncoder().encodeToString(value.getEncoded()));
        }
    }

    public static class PubKeyMapping {
        public static PubKey fromDto(PubKeyDto dto) {
            return new PubKey(DtoMapping.PublicKeyMapping.fromDto(dto.publicKey()), dto.keyId());
        }

        public static PubKeyDto toDto(PubKey value) {
            PublicKey publicKey = value.getPublicKey();
            PublicKeyDto publicKeyDto = DtoMapping.PublicKeyMapping.toDto(publicKey);
            String keyId = value.getKeyId();
            byte[] hash = DigestUtil.hash(publicKey.getEncoded());
            String hashBase64 = Base64.getEncoder().encodeToString(hash);
            String id = Hex.encode(hash);
            return new PubKeyDto(publicKeyDto, keyId, hashBase64, id);
        }
    }

    public static class AddressMapping {
        public static Address fromDto(AddressDto dto) {
            return new Address(dto.host(), dto.port());
        }

        public static AddressDto toDto(Address value) {
            return new AddressDto(value.getHost(), value.getPort());
        }
    }

    public static class TransportTypeMapping {
        public static TransportType fromDto(TransportTypeDto dto) {
            if (dto == TransportTypeDto.CLEAR) {
                return TransportType.CLEAR;
            } else if (dto == TransportTypeDto.TOR) {
                return TransportType.TOR;
            } else if (dto == TransportTypeDto.I2P) {
                return TransportType.I2P;
            } else {
                throw new IllegalArgumentException("Unsupported transportType " + dto);
            }
        }

        public static TransportTypeDto toDto(TransportType value) {
            if (value == TransportType.CLEAR) {
                return TransportTypeDto.CLEAR;
            } else if (value == TransportType.TOR) {
                return TransportTypeDto.TOR;
            } else if (value == TransportType.I2P) {
                return TransportTypeDto.I2P;
            } else {
                throw new IllegalArgumentException("Unsupported transportType " + value);
            }
        }
    }

    public static class AddressByTransportTypeMapMapping {
        public static AddressByTransportTypeMap fromDto(AddressByTransportTypeMapDto dto) {
            return new AddressByTransportTypeMap(dto.map().entrySet().stream()
                    .collect(Collectors.toMap(entry -> TransportTypeMapping.fromDto(entry.getKey()),
                            entry -> AddressMapping.fromDto(entry.getValue())
                    )));
        }

        public static AddressByTransportTypeMapDto toDto(AddressByTransportTypeMap map) {
            return new AddressByTransportTypeMapDto(map.getMap().entrySet().stream()
                    .collect(Collectors.toMap(entry -> TransportTypeMapping.toDto(entry.getKey()),
                            entry -> AddressMapping.toDto(entry.getValue()),
                            (v1, v2) -> v1, // Merge function for duplicate keys (unlikely in TreeMap)
                            TreeMap::new
                    )));
        }
    }

    public static class NetworkIdMapping {
        public static NetworkId fromDto(NetworkIdDto dto) {
            return new NetworkId(AddressByTransportTypeMapMapping.fromDto(dto.addressByTransportTypeMap()),
                    PubKeyMapping.fromDto(dto.pubKey()));
        }

        public static NetworkIdDto toDto(NetworkId networkId) {
            return new NetworkIdDto(
                    AddressByTransportTypeMapMapping.toDto(networkId.getAddressByTransportTypeMap()),
                    PubKeyMapping.toDto(networkId.getPubKey())
            );
        }
    }


}
