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

package bisq.tor.controller.events.events;

// Tor events are defined in https://gitlab.torproject.org/tpo/core/tor/-/blob/main/src/feature/control/control_events.c#L81
// Used to implement SETEVENTS and GETINFO events/names
public enum EventType {
    STATUS_CLIENT,
    HS_DESC
}

/*
const struct control_event_t control_event_table[] = {
  { EVENT_CIRCUIT_STATUS, "CIRC" },
  { EVENT_CIRCUIT_STATUS_MINOR, "CIRC_MINOR" },
  { EVENT_STREAM_STATUS, "STREAM" },
  { EVENT_OR_CONN_STATUS, "ORCONN" },
  { EVENT_BANDWIDTH_USED, "BW" },
  { EVENT_DEBUG_MSG, "DEBUG" },
  { EVENT_INFO_MSG, "INFO" },
  { EVENT_NOTICE_MSG, "NOTICE" },
  { EVENT_WARN_MSG, "WARN" },
  { EVENT_ERR_MSG, "ERR" },
  { EVENT_NEW_DESC, "NEWDESC" },
  { EVENT_ADDRMAP, "ADDRMAP" },
  { EVENT_DESCCHANGED, "DESCCHANGED" },
  { EVENT_NS, "NS" },
  { EVENT_STATUS_GENERAL, "STATUS_GENERAL" },
  { EVENT_STATUS_CLIENT, "STATUS_CLIENT" },
  { EVENT_STATUS_SERVER, "STATUS_SERVER" },
  { EVENT_GUARD, "GUARD" },
  { EVENT_STREAM_BANDWIDTH_USED, "STREAM_BW" },
  { EVENT_CLIENTS_SEEN, "CLIENTS_SEEN" },
  { EVENT_NEWCONSENSUS, "NEWCONSENSUS" },
  { EVENT_BUILDTIMEOUT_SET, "BUILDTIMEOUT_SET" },
  { EVENT_GOT_SIGNAL, "SIGNAL" },
  { EVENT_CONF_CHANGED, "CONF_CHANGED"},
  { EVENT_CONN_BW, "CONN_BW" },
  { EVENT_CELL_STATS, "CELL_STATS" },
  { EVENT_CIRC_BANDWIDTH_USED, "CIRC_BW" },
  { EVENT_TRANSPORT_LAUNCHED, "TRANSPORT_LAUNCHED" },
  { EVENT_HS_DESC, "HS_DESC" },
  { EVENT_HS_DESC_CONTENT, "HS_DESC_CONTENT" },
  { EVENT_NETWORK_LIVENESS, "NETWORK_LIVENESS" },
  { 0, NULL },
};

 */
