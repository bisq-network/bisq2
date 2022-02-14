package bisq.i2p;

import lombok.extern.slf4j.Slf4j;
import net.i2p.I2PAppContext;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;

import java.util.Optional;

@Slf4j
public class I2pUtils {

    /**
     * Only call when necessary, because it can be slow. May involve a network lookup.
     *
     * @param i2pAddress Address in any valid i2p format
     * @return Address in base64 format, or null if it cannot be looked up
     */
    public static Optional<String> maybeLookupAndConvertToBase64(String i2pAddress) {
        return getDestinationFor(i2pAddress)
                .map(Destination::toBase64);
    }

    public static Optional<Destination> getDestinationFor(String peer) {
        try {
            // Successful if peer is already in base64 format, else throws DataFormatException
            return Optional.of(new Destination(peer));
        } catch (DataFormatException e) {
            // Try to resolve any other format like *.i2p, *b32.i2p through a network lookup
            // Nullable because lookup can fail
            return Optional.ofNullable( I2PAppContext.getGlobalContext().namingService().lookup(peer) );
        }
    }
}
