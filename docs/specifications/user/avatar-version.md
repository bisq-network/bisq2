# Avatar version

A user profile carries an `avatarVersion`, the version of the cat hash icon set which its owner used. The node derives the
icon from the key hash and the proof of work solution of the profile with the bucket configuration of this version.

## Rules

1. A node renders a profile whose `avatarVersion` it supports with that version. It renders every other value, also a
   negative one, like version 0.
2. The icon cache of a profile is in the directory of the version which the node renders, and the cleanup of outdated icons
   uses the same version.
3. The node does not reject a profile because of its `avatarVersion`.

## Reasons

- Rule 1: the value comes from the network. Before, `CatHashService` threw for every value other than 0, and nothing on the
  rendering path caught it, so one profile broke the rendering of every node which showed it. Version 0 is the version which
  every node supports.
- Rule 2: otherwise the cleanup would delete the cached icons of such profiles, and the cache would hold a directory for each
  value.
- Rule 3: a later version can add an avatar version. A node which rejected unknown values would drop the profiles of these
  users.

## Limits

`avatarVersion` is excluded from the hash of `UserProfile` version 1, so signature V1 does not cover it. Until the cutoff of
signature V1, a relay can change it in a copy of another user's profile. With rule 1 this changes at most the icon, and only
for profiles of a version which a node does not support.
