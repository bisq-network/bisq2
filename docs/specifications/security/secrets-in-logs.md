# Secrets in logs

This specification defines that a node never writes a secret to its log, and how the node logs the commands and
replies of the Tor control port, which carry secrets.

A secret is data which lets somebody act as the node or as one of its identities, or control a local service of the
node:

- a private key: an EC private key, the private key of an onion service (Tor), the private key of an I2P destination;
- the control password of the Tor control port, and the auth cookie of an external Tor.

## Problem

Users share their log files. The error popup has a button which zips `bisq.log` and the Tor log for a bug report.
Users also attach logs to issues and send them to support.

From version 2.1.3 to version 2.1.13, the node wrote every Tor control command to the log at INFO and hid only the
data of `AUTHENTICATE`. The command `ADD_ONION` contains the private key of the onion service. The node sends it at every start,
once for the default identity and once for each user profile. Seed nodes and oracle nodes use the same code. So every
log file contained the onion private keys of all identities of the node.

Whoever has the private key of an onion service can publish the same onion service and can sign the proof of the onion
address in the connection handshake. So that person can connect to peers as that address and can receive messages
which peers send to it. The content of confidential messages stays protected, because the EC private keys were not in
the log. The onion key of an identity does not change, so an old log file stays dangerous while the identity exists.

## Requirements

1. A node never writes a secret to a log, at any log level. This includes the message of an exception, because the
   callers log exception messages, and the `toString` of an object which holds a secret.
2. **Tor control commands.** The log form of a command keeps the command keyword and only arguments which are known to
   be public. The arguments of every other command are replaced by `[hidden]`:
   - `ADD_ONION`: the log keeps the key type and the port mappings and hides the key, for example
     `ADD_ONION ED25519-V3:[hidden] Port=9999,12345`. Other arguments, for example client authorization data, are
     hidden.
   - `AUTHENTICATE`: the password or the auth cookie is hidden: `AUTHENTICATE [hidden]`.
   - `SETCONF`: the arguments are hidden, because the value of an option can be a password.
   - `GETINFO`, `HSFETCH`, `RESETCONF`, `SETEVENTS`, `TAKEOWNERSHIP`: the arguments are info keys, onion service IDs,
     option names and event names. They are public, so the log keeps them.
3. **Tor control replies.** The log form of a reply line keeps the status code and the status text, for example
   `250 OK` or an error message of Tor, and keeps asynchronous events, for example `650 HS_DESC ...`. Reply data of the
   form `Key=Value` keeps its value only if the key is known to have a public value (`ServiceID`,
   `net/listeners/socks`). Otherwise the value is hidden, for example `250-PrivateKey=[hidden]`. A line without a
   status code is a line of a multi-line data reply. It has no fixed form, so it is hidden.
4. The exception of a failed Tor control command contains only the log forms of the command and of the reply. The
   exception of a failed `SETCONF` names only the option.

## Reasons

- Rules 2 and 3 list what is public instead of what is secret. The earlier rule hid only commands which contain
  `AUTHENTICATE`, and `ADD_ONION` was missing from it. With a list of public parts, a new command does not leak a
  secret if its author does not think about the log.
- `ADD_ONION` keeps the key type and the port mappings, because they help support and contain no secret. The key
  argument is handled by its position, not by searching for `=`: the base64 form of a key ends with `=`.
- Rule 3 hides reply data by default, because Tor returns keys which it generates as reply data: `PrivateKey=` after
  `ADD_ONION NEW:...`. Bisq always sends its own key, so Tor does not send this line today, but a change of the command
  must not leak a key.
- Rule 3 keeps status texts, because they help support. They are messages of Tor. For the commands which Bisq sends,
  they do not contain a secret: the error replies to `AUTHENTICATE` and `ADD_ONION` are fixed texts without the
  password, the cookie or the key (checked in the source of Tor 0.4.9). Bisq does not subscribe to events which carry
  secrets.

## Compatibility

The rules change only the log, not the network protocol or persisted data.

Log files which versions 2.1.3 to 2.1.13 wrote contain the onion private keys of all identities. Log files are appended
across versions, so `bisq.log` keeps such lines after the upgrade until it rolls over at 10 MB. When the desktop app
builds the zip file of the error report, it replaces each such key in the copies of the log files with `[hidden]`
and keeps the key type. The log files on disk, the rolled log files, the log which the error popup opens, and the logs
of headless nodes still contain the keys. Users who shared such a log, and operators of seed and oracle nodes, need a
decision about the deletion of old log files and the rotation of onion keys. A new onion key changes the network
address of the identity.

## Related specifications

- [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md), rule 7: exception messages of
  integrity rules do not contain the rejected value, because users share their log files.
