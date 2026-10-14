# User profile proof of work

A user profile carries a proof of work. Its payload is the hash of the profile's public key. The nym
and the cat hash icon of the profile are derived from the key hash and the solution of this proof.

## Rules

1. A node accepts a user profile from the network only if all of these hold:
   - The payload of the proof of work equals the hash of the profile's public key.
   - The claimed difficulty is at least the minting difficulty `MINT_NYM_DIFFICULTY` (2^16). A
     difficulty which is not a number (NaN) fails this rule.
   - The solution is valid work for the claimed difficulty.
2. A node which creates a profile mints the proof with `MINT_NYM_DIFFICULTY`.
3. A node ignores a profile which breaks rule 1. It does not show it and does not use it for nym
   or nickname decisions.
4. A node rejects a user profile whose proof of work payload is not the hash of the profile's public key when it
   parses the profile: in network messages, in inventory responses and in persisted data, also when the profile is part
   of other data, for example a contract or a chat message. The rule is part of the integrity check of the profile
   (see [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)).

## Reason

The work makes it expensive to search for a key whose nym and icon look like those of another
user. The verifier checks the work against the difficulty which the profile claims. Without the
minimum in rule 1, a profile could claim a difficulty of 0 and pay about one hash per attempt
instead of about 2^16.

Reason for rule 4: a proof of work accepts a payload of up to 10 000 000 bytes, because other uses need large payloads.
Before rule 4, any key pair could publish user profiles with a large payload. Nodes of all types parsed, stored,
persisted and relayed them, read them again at each start, and logged the payload in hex when they ignored the profile.
Only the size limit of a message bounded such a profile. Rule 1 alone does not prevent this, because the network store
does not check it (see [Limits](#limits)). With rule 4, a node drops such a profile when it parses it: a broadcast is
dropped without closing the connection, an inventory entry is skipped, and a persisted entry is skipped at load.

## Compatibility

All released versions mint with `MINT_NYM_DIFFICULTY` (the value is unchanged since 2022). Profiles
of unmodified nodes therefore pass rule 1.

All released versions since 2.0.0 also create the payload as the hash of the profile key, and ignore a profile with
another payload. Profiles of unmodified nodes therefore pass rule 4. Data which contains a profile with another payload
can come only from a modified client. A node of this version skips such data when it loads its stores; for private
stores it keeps a copy of the file (see [`../persistence/unreadable-store-files.md`](../persistence/unreadable-store-files.md)).
Nodes of earlier versions still store and relay such profiles.

## Limits

Rule 1 applies where a node accepts profiles from the network data store. The network store
itself does not check the difficulty and the work, so it still stores and relays a profile which
breaks only these parts of rule 1. Profiles embedded in other messages, for example in a contract
or a mediation request, are not checked against these parts. Rule 4 applies everywhere, because
it is checked at parsing.
