# SemiAnarchy FarmControl additions

Based on upstream 1.3.0 commit
`728efcfae298706f804ad08f2d90d5fd02f6b7e4`, including its world-retention fix.
Release 1.3.0-semianarchy.1 retains the tested dev.3 runtime code; only the build
identification and this documentation change. MIT licence and upstream credit
remain unchanged.

## Villager selector

`category:unemployed-villager-without-home` snapshots profession `NONE` and the
absence of `MemoryKey.HOME` through Bukkit's public API. It filters before native
grouping: employed villagers, nitwits and bed-linked villagers neither count nor
connect groups. Normal FarmControl action ownership, scheduling and undo remain.
No collision, scoreboard-team, NoAI or breeding-control code is changed.

The owner's policy uses count 10, native distance 2 and `remove-awareness` only.
The homelab configuration owns the check interval. Neighbour chaining compares block positions on each
axis, ignores walls and has no maximum overall group span. Moving a villager
outside the group restores awareness on a subsequent check. Paused villagers
need to wake before claiming new jobs or beds. Upstream exclusions and undo on
interaction, damage or targeting remain enabled.

The isolated trial passed 9/10 thresholds, filtering before grouping, wall
chaining, position/profession/valid-bed recovery, native reload and clean restart.
Full AI and collisions remained enabled. A paused baby in shallow still water
took drowning damage; the active comparison stayed healthy. The owner accepted
this behaviour on 22 October 2026 and approved queuing the unchanged rule.
Flowing-water safety, actual iron output and full-stack human pushing were not
established by that trial; they are not represented as passed.

## Wolf taming

`wolf-taming-limit: 10` prevents additional taming once an owner UUID has ten
recorded wolves, including unloaded ones. Existing wolves are never edited or
deleted; breeding is deliberately unrestricted. Puppies count against later
taming, so this is not a hard ten-dog population cap. Native additions, successful
tames, deaths and permanent removals update `plugins/FarmControl/wolf-owners.yml`.
Writes use an atomic same-directory replacement. No permissions bypass the limit.

An enabled limit refuses startup without a complete ownership census. The
homelab Ansible role installs a stopped-world reader and invokes it through the
normal systemd `ExecStartPre` lifecycle on every start. It scans saved entity
regions, including passengers, without changing world data. Rebuilding at every
start reconciles crashes against Minecraft's saved world; no second scheduler or
chunk loading is involved. This build is Paper-only, not Folia compatible.
Other plugins' ownership edits to already-loaded wolves reconcile when those
wolves next enter a world. The census is not a transaction with entity saves.

The trial verified native bone interactions for the tenth/eleventh dog,
independent owners, cancelled tames, death freeing a slot, unloaded pets and
restart retention. It did not establish a native puppy birth; there is no
breeding-cancellation handler.

## Build and recovery

Build using JDK 17 and the upstream wrapper:

    ./gradlew --no-daemon build

The release includes its exact source commit, build command and artifact digest.
The homelab repository owns the authoritative profile, limits and player tips.
The new JAR requires a normal restart. Later profile/configuration changes use
`/farmcontrol reload`. Before removing the custom villager rule or plugin,
remove its profile and reload to restore affected villagers. Never hot-load the
JAR or use the global `/reload` command.
