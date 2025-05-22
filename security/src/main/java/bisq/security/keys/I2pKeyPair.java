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

package bisq.security.keys;

import bisq.common.proto.PersistableProto;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.i2p.crypto.EncType;
import net.i2p.crypto.SigType;
import net.i2p.data.Certificate;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.data.PrivateKey;
import net.i2p.data.SigningPrivateKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public class I2PKeyPair implements PersistableProto<I2PKeyPair> {
    private final PrivateKey privateKey;
    private final SigningPrivateKey signingPrivateKey;
    private final Destination destination;

    public I2PKeyPair(PrivateKey privateKey,
                      SigningPrivateKey signingPrivateKey,
                      Destination destination) {
        this.privateKey = privateKey;
        this.signingPrivateKey = signingPrivateKey;
         this.destination = destination;
    }

    @Override
    public bisq.security.protobuf.I2PKeyPair toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.security.protobuf.I2PKeyPair.Builder getBuilder(boolean serializeForHash) {
        return bisq.security.protobuf.I2PKeyPair.newBuilder()
                .setPrivateKey(ByteString.copyFrom(privateKey.getData()))
                .setSigningPrivateKey(ByteString.copyFrom(signingPrivateKey.getData()))
                .setDestination(destination.toString());
    }

    public static I2PKeyPair fromProto(bisq.security.protobuf.I2PKeyPair proto) {
        byte[] encData = proto.getPrivateKey().toByteArray();
        if (encData.length == 0) {
            throw new IllegalArgumentException("Missing encryption privateKey field");
        }
        int encPubLen = EncType.ELGAMAL_2048.getPubkeyLen();

        PrivateKey priv = new PrivateKey(EncType.ELGAMAL_2048, encData);

        byte[] sigData = proto.getSigningPrivateKey().toByteArray();
        if (sigData.length == 0) {
            throw new IllegalArgumentException("Missing signingPrivateKey field");
        }
        SigningPrivateKey signPriv = new SigningPrivateKey(SigType.EdDSA_SHA512_Ed25519, sigData);

        int signPubLen = signPriv.toPublic().getData().length; // e.g. 32 for Ed25519

        int totalLen = encPubLen + signPubLen;
        if (totalLen > 384) {
            throw new IllegalArgumentException(
                    "Public keys exceed 384 bytes: " + totalLen);
        }
        int paddingLength = 384 - totalLen;
        SecureRandom rng = new SecureRandom();
        byte[] pad = new byte[paddingLength];
        rng.nextBytes(pad);

        Destination dest = new Destination();
        dest.setPublicKey(priv.toPublic());
        dest.setSigningPublicKey(signPriv.toPublic());
        dest.setCertificate(Certificate.NULL_CERT);
        dest.setPadding(pad);
        return new I2PKeyPair(priv, signPriv, dest);
    }


    public String getBase32Address() {
        return destination.toBase32();
    }

    public String getBase64Destination() {
        return destination.toBase64();
    }

    public byte[] getPrivateKeyFileBytes() throws IOException, DataFormatException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        destination.writeBytes(baos);
        privateKey.writeBytes(baos);
        signingPrivateKey.writeBytes(baos);
        baos.flush();
        return baos.toByteArray();
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof I2pKeyPair that)) return false;

        return Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(destination);
    }
}
