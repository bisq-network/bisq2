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

package network.misq.tor;

import java.util.List;

import static network.misq.common.util.FileUtils.FILE_SEP;

public class Constants {
    public final static String LOCALHOST = "127.0.0.1";

    // Directories
    public final static String DOT_TOR_DIR = ".tor";
    public final static String HS_DIR = "hiddenservice";
    public final static String NATIVE_DIR = "native";
    public final static String WIN_DIR = "windows";
    public final static String LINUX_DIR = "linux";
    public final static String LINUX32_DIR = LINUX_DIR + FILE_SEP + "x86";
    public final static String LINUX64_DIR = LINUX_DIR + FILE_SEP + "x64";
    public final static String OSX_DIR = "osx";
    public final static String OSX_DIR_64 = OSX_DIR + FILE_SEP + "x64";

    // Files
    public final static String VERSION = "version";
    public final static String GEO_IP = "geoip";
    public final static String GEO_IPV_6 = "geoip6";
    public final static String TORRC = "torrc";
    public final static String PID = "pid";
    public final static String COOKIE = "control_auth_cookie";
    public final static String HOSTNAME = "hostname";
    public final static String PRIV_KEY = "private_key";
    public final static String TOR_ARCHIVE = "tor.tar.xz";

    // Torrc keys
    public final static String TORRC_DEFAULTS = "torrc.defaults";
    public final static String TORRC_NATIVE = "torrc.native";
    public final static String TORRC_KEY_GEOIP6 = "GeoIPv6File";
    public final static String TORRC_KEY_GEOIP = "GeoIPFile";
    public final static String TORRC_KEY_PID = "PidFile";
    public final static String TORRC_KEY_DATA_DIRECTORY = "DataDirectory";
    public final static String TORRC_KEY_COOKIE = "CookieAuthFile";

    // Tor control connection
    public final static String CONTROL_PORT_LOG_SUB_STRING = "Control listener listening on port ";
    public final static List<String> CONTROL_EVENTS = List.of("INFO", "NOTICE", "WARN", "ERR", "CIRC", "ORCONN", "HS_DESC", "HS_DESC_CONTENT");
    public final static String CONTROL_STATUS_BOOTSTRAP_PHASE = "status/bootstrap-phase";
    public final static String CONTROL_DISABLE_NETWORK = "DisableNetwork";
    public final static String CONTROL_NET_LISTENERS_SOCKS = "net/listeners/socks";
    public final static String CONTROL_RESET_CONF = "__OwningControllerProcess";
}
