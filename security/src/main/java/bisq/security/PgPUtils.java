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

package bisq.security;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SignatureException;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class PgPUtils {

    public static boolean isSignatureValid(Path pubKeyFile, Path sigFile, Path dataFile) {
        try {
            PGPPublicKeyRing pgpPublicKeyRing = readPgpPublicKeyRing(pubKeyFile);
            PGPSignature pgpSignature = readPgpSignature(sigFile);
            long keyIdFromSignature = pgpSignature.getKeyID();
            PGPPublicKey publicKey = checkNotNull(pgpPublicKeyRing.getPublicKey(keyIdFromSignature), "No public key found for key ID from signature");
            return isSignatureValid(pgpSignature, publicKey, dataFile);
        } catch (PGPException | IOException | SignatureException e) {
            log.error("Signature verification failed. \npubKeyFile={} \nsigFile={} \ndataFile={}.",
                    pubKeyFile, sigFile, dataFile, e);
            return false;
        }
    }

    public static PGPPublicKeyRing readPgpPublicKeyRing(Path pubKeyFile) throws IOException, PGPException {
        try (InputStream inputStream = PGPUtil.getDecoderStream(Files.newInputStream(pubKeyFile))) {
            PGPPublicKeyRingCollection publicKeyRingCollection = new PGPPublicKeyRingCollection(inputStream, new JcaKeyFingerprintCalculator());
            Iterator<PGPPublicKeyRing> iterator = publicKeyRingCollection.getKeyRings();
            if (iterator.hasNext()) {
                return iterator.next();
            } else {
                throw new PGPException("Could not find public keyring in provided key file");
            }
        }
    }

    public static PGPSignature readPgpSignature(Path sigFile) throws IOException, SignatureException {
        try (InputStream inputStream = PGPUtil.getDecoderStream(Files.newInputStream(sigFile))) {
            PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(inputStream, new JcaKeyFingerprintCalculator());
            Object signatureObject = pgpObjectFactory.nextObject();
            if (signatureObject instanceof PGPSignatureList signatureList) {
                checkArgument(!signatureList.isEmpty(), "signatureList must not be empty");
                return signatureList.get(0);
            } else if (signatureObject instanceof PGPSignature) {
                return (PGPSignature) signatureObject;
            } else {
                throw new SignatureException("Could not find signature in provided signature file");
            }
        }
    }

    public static boolean isSignatureValid(PGPSignature pgpSignature,
                                           PGPPublicKey publicKey,
                                           Path dataFile) throws IOException, PGPException {
        checkArgument(pgpSignature.getKeyID() == publicKey.getKeyID(), "Key ID from signature not matching key ID from pub Key");
        pgpSignature.init(new BcPGPContentVerifierBuilderProvider(), publicKey);
        try (InputStream inputStream = new DataInputStream(new BufferedInputStream(Files.newInputStream((dataFile))))) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while (true) {
                bytesRead = inputStream.read(buffer, 0, 1024);
                if (bytesRead == -1)
                    break;
                pgpSignature.update(buffer, 0, bytesRead);
            }
            return pgpSignature.verify();
        }
    }
}