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

package bisq.common.network;

import org.junit.jupiter.api.Test;

import static bisq.common.network.Address.removeProtocolPrefix;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressTest {
    @Test
    void testValidI2PBase64Address() {
        String host = "OazWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpa03btjnuHsU5Gsx61g7CqT2YA8J6EHOiS~-XZpiNt01BQAEAAcAAA==";
        int port = 1234;
        Address address = Address.from(host, port);
        assertInstanceOf(I2PAddress.class, address);
        assertEquals(host, address.getHost());
        assertEquals(port, address.getPort());
        assertTrue(((I2PAddress) address).getDestinationBase32().isEmpty());
    }

    @Test
    void testValidI2PBase64AndBas32Address() {
        String b64 = "OazWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpa03btjnuHsU5Gsx61g7CqT2YA8J6EHOiS~-XZpiNt01BQAEAAcAAA==";
        String b32 = "wgglodqww5sifflx4ptugbn2yjey3x3xlkstozs7dmzw22wo6qfa.b32.i2p";
        int port = 1234;
        I2PAddress address = new I2PAddress(b64, b32, port);
        assertEquals(b64, address.getHost());
        assertEquals(b64, address.getDestinationBase64());
        assertTrue(address.getDestinationBase32().isPresent());
        assertEquals(b32, address.getDestinationBase32().get());
        assertEquals(port, address.getPort());
    }

    @Test
    void testInvalidI2PBase64Address() {
        // too short
        String host = "DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpa03btjnuHsU5Gsx61g7CqT2YA8J6EHOiS~-XZpiNt01BQAEAAcAAA==";
        int port = 1234;
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Address.from(host, port)
        );

        // too long
        String host2 = "OazWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpa03btjnuHsU5Gsx61g7CqT2YA8J6EHOiS~-XZpiNt01BQAEAAcAAA==";
        IllegalArgumentException ex2 = assertThrows(
                IllegalArgumentException.class,
                () -> Address.from(host2, port)
        );

        // unsupported char
        String host3 = "#OazWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpa03btjnuHsU5Gsx61g7CqT2YA8J6EHOiS~-XZpiNt01BQAEAAcAAA==";
        IllegalArgumentException ex3 = assertThrows(
                IllegalArgumentException.class,
                () -> Address.from(host3, port)
        );

        // with .i2p suffix (we don't support that internally)
        String host4 = "OazWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpa03btjnuHsU5Gsx61g7CqT2YA8J6EHOiS~-XZpiNt01BQAEAAcAAA==.i2p";
        IllegalArgumentException ex4 = assertThrows(
                IllegalArgumentException.class,
                () -> Address.from(host4, port)
        );
        // Missing port
        String host5 = "OazWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpTm81gObqFuwTCNJZLeu5DqswBCBoIM34tZX0eHg8WGlObzWA5uoW7BMI0lkt67kOqzAEIGggzfi1lfR4eDxYaU5vNYDm6hbsEwjSWS3ruQ6rMAQgaCDN-LWV9Hh4PFhpa03btjnuHsU5Gsx61g7CqT2YA8J6EHOiS~-XZpiNt01BQAEAAcAAA==.i2p";
        assertThrows(IllegalArgumentException.class, () -> Address.fromFullAddress(host5));
    }

    @Test
    void testValidTorAddress() {
        assertInstanceOf(TorAddress.class, Address.from("m3h2p7j2mfl6w2u6g5hx7o5fek7e6fhb4i2h6h6syh5w4slf6xqrv7ad.onion", 3333));
    }

    @Test
    void testInvalidTorAddress() {
        // v2 address
        assertThrows(IllegalArgumentException.class, () -> Address.from("duskgytldkxiuqc6.onion", 1234));
        assertThrows(IllegalArgumentException.class, () -> Address.from("mm3h2p7j2mfl6w2u6g5hx7o5fek7e6fhb4i2h6h6syh5w4slf6xqrv7ad.onion", 1234));
        assertThrows(IllegalArgumentException.class, () -> Address.from("3h2p7j2mfl6w2u6g5hx7o5fek7e6fhb4i2h6h6syh5w4slf6xqrv7ad.onion", 1234));
        assertThrows(IllegalArgumentException.class, () -> Address.fromFullAddress("m3h2p7j2mfl6w2u6g5hx7o5fek7e6fhb4i2h6h6syh5w4slf6xqrv7ad.onion"));
    }

    @Test
    void testValidClearnetAddress() {
        assertInstanceOf(ClearnetAddress.class, Address.from("192.168.0.10", 8080));
        assertInstanceOf(ClearnetAddress.class, Address.from("0.0.0.0", 8080));
        assertInstanceOf(ClearnetAddress.class, Address.from("2001:0db8:85a3:0000:0000:8a2e:0370:7334", 8080));
        assertInstanceOf(ClearnetAddress.class, Address.from("::", 8080));
        assertInstanceOf(ClearnetAddress.class, Address.fromFullAddress("[2001:db8:85a3::8a2e:370:7334]:9000"));
        assertInstanceOf(ClearnetAddress.class, Address.fromFullAddress("[2001:db8:db8:db8:85a3:8a2e:370:7334]:9000"));
        assertInstanceOf(ClearnetAddress.class, Address.fromFullAddress("[::1]:8080"));
        assertInstanceOf(ClearnetAddress.class, Address.fromFullAddress("[::1]:8080"));
    }

    @Test
    void testInvalidClearnetAddress() {
        assertThrows(IllegalArgumentException.class, () -> Address.from("127.0.0.1", 0));
        assertThrows(IllegalArgumentException.class, () -> Address.from("127.0.0.1", 10000000));
        assertThrows(IllegalArgumentException.class, () -> Address.from("127.0.0.256", 4444));
        assertThrows(IllegalArgumentException.class, () -> Address.from(":", 80));
        assertThrows(IllegalArgumentException.class, () -> Address.from("example.com", 80));
        assertThrows(IllegalArgumentException.class, () -> Address.fromFullAddress("127.0.0.1"));
        assertThrows(IllegalArgumentException.class, () -> Address.fromFullAddress("2001:db8:85a3::8a2e:370:7334]:9000"));
        assertThrows(IllegalArgumentException.class, () -> Address.fromFullAddress("[2001:db8:db8:db8:db8:85a3::370:7334]:9000"));
        assertThrows(IllegalArgumentException.class, () -> Address.fromFullAddress("[2001:db8:db8:db8:db8:85a3:8a2e:370:7334]:9000"));
        assertThrows(IllegalArgumentException.class, () -> Address.fromFullAddress("[2001:db8:85a3::8a2e:370:7334:9000"));
        assertThrows(IllegalArgumentException.class, () -> Address.fromFullAddress("2001:db8:85a3::8a2e:370:7334:9000"));
    }

    @Test
    void testRemoveProtocolPrefix() {
        // Valid inputs where a protocol is present and should be stripped
        assertEquals("example.com:8080", removeProtocolPrefix("http://example.com:8080"));
        assertEquals("example.com", removeProtocolPrefix("https://example.com"));
        assertEquals("b32example.i2p", removeProtocolPrefix("i2p://b32example.i2p"));
        assertEquals("example.com", removeProtocolPrefix("HtTpS://example.com"));
        assertEquals("host:1234", removeProtocolPrefix("my-scheme+v1.2://host:1234"));
        // assertEquals("http://example.com", removeProtocolPrefix("http://http://example.com")); // only first scheme removed

        // Null input
        //  assertNull(removeProtocolPrefix(null));
        // Inputs that don't have a valid scheme at the start
        assertEquals("example.com:8080", removeProtocolPrefix("example.com:8080"));
        assertEquals("ftp//example.com", removeProtocolPrefix("ftp//example.com")); // invalid scheme format
        assertEquals(":8080", removeProtocolPrefix(":8080"));
        //assertEquals("://host", removeProtocolPrefix("://host")); // missing scheme name
    }

    @Test
    void testFailingRemoveProtocolPrefix() {
        // Missing scheme before ://
        assertThrows(IllegalArgumentException.class, () -> removeProtocolPrefix("://host"));
        // Repeated scheme
        assertThrows(IllegalArgumentException.class, () -> removeProtocolPrefix("http://http://example.com"));
        // Empty after removing scheme
        assertThrows(IllegalArgumentException.class, () -> removeProtocolPrefix("custom://"));
    }
}
