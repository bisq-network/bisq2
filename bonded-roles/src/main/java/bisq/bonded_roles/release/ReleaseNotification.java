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

package bisq.bonded_roles.release;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.common.annotation.ExcludeForHash;
import bisq.common.application.DevMode;
import bisq.common.platform.Version;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

import static bisq.network.p2p.services.data.storage.MetaData.*;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
public final class ReleaseNotification implements AuthorizedDistributedData {
    private static final int VERSION = 1;
    public final static int MAX_MESSAGE_LENGTH = 10_000;

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_100_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    private final String id;
    private final long date;
    private final boolean isPreRelease;
    private final boolean isLauncherUpdate;
    private final String releaseNotes;
    private final String versionString;
    private final String releaseManagerProfileId;

    // ExcludeForHash from version 1 on to not treat data from different oracle nodes with different staticPublicKeysProvided value as duplicate data.
    // We add version 2 and 3 for extra safety...
    // Once no nodes with versions below 2.1.0  are expected anymore in the network we can remove the parameter
    // and use default `@ExcludeForHash` instead.
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    @EqualsAndHashCode.Exclude
    private final boolean staticPublicKeysProvided;

    // transient fields are excluded by default for EqualsAndHashCode
    private transient final Version releaseVersion;

    public ReleaseNotification(String id,
                               long date,
                               boolean isPreRelease,
                               boolean isLauncherUpdate,
                               String releaseNotes,
                               String versionString,
                               String releaseManagerProfileId,
                               boolean staticPublicKeysProvided) {
        this(VERSION,
                id,
                date,
                isPreRelease,
                isLauncherUpdate,
                releaseNotes,
                versionString,
                releaseManagerProfileId,
                staticPublicKeysProvided);
    }

    public ReleaseNotification(int version,
                                String id,
                                long date,
                                boolean isPreRelease,
                                boolean isLauncherUpdate,
                                String releaseNotes,
                                String versionString,
                                String releaseManagerProfileId,
                                boolean staticPublicKeysProvided) {
        this.version = version;
        this.id = id;
        this.date = date;
        this.isPreRelease = isPreRelease;
        this.isLauncherUpdate = isLauncherUpdate;
        this.releaseNotes = releaseNotes;
        this.versionString = versionString;
        this.releaseManagerProfileId = releaseManagerProfileId;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        releaseVersion = new Version(versionString);

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateId(id);
        NetworkDataValidation.validateDate(date);
        NetworkDataValidation.validateText(releaseNotes, MAX_MESSAGE_LENGTH);
        NetworkDataValidation.validateVersion(versionString);
        NetworkDataValidation.validateProfileId(releaseManagerProfileId);
    }

    @Override
    public bisq.bonded_roles.protobuf.ReleaseNotification.Builder getBuilder(boolean serializeForHash) {
        return bisq.bonded_roles.protobuf.ReleaseNotification.newBuilder()
                .setId(id)
                .setDate(date)
                .setIsPreRelease(isPreRelease)
                .setIsLauncherUpdate(isLauncherUpdate)
                .setReleaseNotes(releaseNotes)
                .setVersionString(versionString)
                .setReleaseManagerProfileId(releaseManagerProfileId)
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .setVersion(version);
    }

    @Override
    public bisq.bonded_roles.protobuf.ReleaseNotification toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static ReleaseNotification fromProto(bisq.bonded_roles.protobuf.ReleaseNotification proto) {
        return new ReleaseNotification(
                proto.getVersion(),
                proto.getId(),
                proto.getDate(),
                proto.getIsPreRelease(),
                proto.getIsLauncherUpdate(),
                proto.getReleaseNotes(),
                proto.getVersionString(),
                proto.getReleaseManagerProfileId(),
                proto.getStaticPublicKeysProvided()
        );
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.bonded_roles.protobuf.ReleaseNotification.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        if (DevMode.isDevMode()) {
            return AuthorizedPubKeys.DEV_PUB_KEYS;
        } else {
            return AuthorizedPubKeys.RELEASE_MANAGER_PUB_KEYS;
        }
    }

    @Override
    public double getCostFactor() {
        return 0.5;
    }

    @Override
    public boolean staticPublicKeysProvided() {
        return staticPublicKeysProvided;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return !Version.isValid(versionString) || releaseNotes.length() > MAX_MESSAGE_LENGTH;
    }
}