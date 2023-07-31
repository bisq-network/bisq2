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

import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.Version;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
public final class ReleaseNotification implements AuthorizedDistributedData {
    public final static int MAX_MESSAGE_LENGTH = 10000;
    public final static long TTL = TimeUnit.DAYS.toMillis(180);

    // todo Production key not set yet - we use devMode key only yet
    public static final Set<String> AUTHORIZED_PUBLIC_KEYS = Set.of(
            // ReleaseManager1
            "3056301006072a8648ce3d020106052b8104000a0342000498742bc67190380704173a3db345b7a7281a6d57fc754bd006740d99b8d5dcce8556034b61ce974ce95a708482d2921609d93b83361266fa209157ebc3f62983"
    );


    private final MetaData metaData = new MetaData(TTL, 100_000, ReleaseNotification.class.getSimpleName());
    private final String id;
    private final long date;
    private final boolean isPreRelease;
    private final boolean isLauncherUpdate;
    private final String releaseNotes;
    private final String versionString;
    private final String releaseManagerProfileId;
    private final boolean staticPublicKeysProvided;

    private transient final Version version;

    public ReleaseNotification(String id,
                               long date,
                               boolean isPreRelease,
                               boolean isLauncherUpdate,
                               String releaseNotes,
                               String versionString,
                               String releaseManagerProfileId,
                               boolean staticPublicKeysProvided) {
        this.id = id;
        this.date = date;
        this.isPreRelease = isPreRelease;
        this.isLauncherUpdate = isLauncherUpdate;
        this.releaseNotes = releaseNotes;
        this.versionString = versionString;
        this.releaseManagerProfileId = releaseManagerProfileId;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        version = new Version(versionString);
    }

    @Override
    public bisq.bonded_roles.protobuf.ReleaseNotification toProto() {
        return bisq.bonded_roles.protobuf.ReleaseNotification.newBuilder()
                .setId(id)
                .setDate(date)
                .setIsPreRelease(isPreRelease)
                .setIsLauncherUpdate(isLauncherUpdate)
                .setReleaseNotes(releaseNotes)
                .setVersionString(versionString)
                .setReleaseManagerProfileId(releaseManagerProfileId)
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .build();
    }

    public static ReleaseNotification fromProto(bisq.bonded_roles.protobuf.ReleaseNotification proto) {
        return new ReleaseNotification(proto.getId(),
                proto.getDate(),
                proto.getIsPreRelease(),
                proto.getIsLauncherUpdate(),
                proto.getReleaseNotes(),
                proto.getVersionString(),
                proto.getReleaseManagerProfileId(),
                proto.getStaticPublicKeysProvided());
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
            return DevMode.AUTHORIZED_DEV_PUBLIC_KEYS;
        } else {
            return AUTHORIZED_PUBLIC_KEYS;
        }
    }

    @Override
    public boolean staticPublicKeysProvided() {
        return staticPublicKeysProvided;
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return !Version.isValid(versionString) || releaseNotes.length() > MAX_MESSAGE_LENGTH;
    }
}