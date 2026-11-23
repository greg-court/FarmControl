# SemiAnarchy FarmControl additions

Based on upstream 1.3.0 commit
`728efcfae298706f804ad08f2d90d5fd02f6b7e4`, including its world-retention fix.
Release 1.3.0-semianarchy.3 adds group-scoped silverfish actions and recovery of
FrozenSilverfish-owned state. Release 1.3.0-semianarchy.7 restores vanilla
pet taming on Paper and Folia.

## Newest-first trimming

Release 1.3.0-semianarchy.4 lets a profile set `group.preserve-variants: false`.
This orders eligible excess mobs by age across variants such as sheep colours
or villager professions. Profiles with the setting omitted keep the usual
variant-preserving order. Group membership, exclusions and scheduling retain
their existing behaviour.

## Crowded silverfish

Three optional native profile actions replace the dedicated FrozenSilverfish
plugin:

- `pause-silverfish` uses `Mob#setAware(false)`, retaining normal physics and
  collisions. Its separate action name inherits native default undo settings
  (`damage`, `interact` and `target` false), so repeated farm damage does not
  wake the crowd or change the existing villager `remove-awareness` settings.
- `prevent-drowning` cancels drowning damage only while the silverfish has this
  action assigned.
- `push-off-campfires` gives unaware, actioned silverfish the same 0.3 horizontal /
  0.2 vertical escape impulse as FrozenSilverfish 1.1.1 when campfire damage occurs.
  Campfire damage itself is not cancelled. Attribution is in THIRD-PARTY-NOTICES.

The homelab policy assigns all three above 15 silverfish (count 16), linked at
distance 10, and keeps its independent 1,000 population limit. Each action can
instead be assigned by a separate profile with its own threshold. Native group
checks, exclusions, action tracking, unload cleanup and recovery are reused;
there is no extra periodic scan, timer or mutable state store. Leaving a qualifying group
restores awareness and removes the protections at a subsequent cycle. New mobs
remain normal until a cycle assigns actions. These profiles are opt-in; the
upstream default configuration does not enable them.

Remove FrozenSilverfish when installing this build. On startup and entity entry,
the replacement recognises its `frozen_silverfish` tag and restores only that
legacy awareness state, unless FarmControl already owns `pause-silverfish`.
Its `frozensilverfish:collisions_off` marker is also cleared and collisions
restored unless FarmControl owns `disable-collisions`. Previously unloaded fish
recover when their chunks load. Other unmarked unaware mobs are left alone;
world files do not need an offline rewrite. Normal group policy then applies.
This migration remains available for old chunks and does not force-load them.

On the 17 November Leaf 26.3-24 hourly-snapshot clone, live checks passed the
15/16 boundary, ten-block chaining across separate enclosures, split and count
recovery, scoped drowning damage, campfire damage/escape, falling and water
transport. A 1,024-fish group was trimmed to 1,000 healthy survivors. Native reload
retained the expected policy. Loaded and previously unloaded legacy-tagged fish
recovered; an unmarked unaware control stayed unchanged. Monster activation was
temporarily disabled in this playerless lab so physics advanced. This is not a
player-kill XP retest, a farm-throughput guarantee or a matched performance test.

## Villager selector

`category:unemployed-villager-without-home` selects jobless villagers without
a claimed bed or a nearby workstation. The check covers all vanilla workstations,
including filled cauldrons. Set `villager-workstation-distance` in `config.yml`
to 1 (default) or 2, then run `/farmcontrol reload`. Distance counts steps
between blocks sharing a face. The existing category key is retained. Filtering
happens before grouping, so
exempt villagers neither count nor link separate groups. The scan uses the
existing snapshot cycle and only reads blocks in loaded, owned chunks.

Homelab uses this for the crowded holding-pen awareness rule. Adding a workstation
beside a paused villager lets it wake on a subsequent check and claim a profession
through vanilla AI. Named and other native exclusions and event-based undo remain.
FarmControl does not change breeding or collision rules as part of this exemption.

The isolated trial passed 9/10 thresholds, filtering before grouping, wall
chaining, position/profession/valid-bed recovery, native reload and clean restart.
Full AI and collisions remained enabled. A paused baby in shallow still water
took drowning damage; the active comparison stayed healthy. The owner accepted
this behaviour on 22 October 2026 and approved queuing the unchanged rule.
Flowing-water safety, actual iron output and full-stack human pushing were not
established by that trial; they are not represented as passed.

## Build and recovery

Build using JDK 17 and the upstream wrapper:

    ./gradlew --no-daemon build

The release includes its exact source commit, build command and artifact digest.
The homelab repository owns the authoritative profile, limits and player tips.
The new JAR requires a normal restart. Later profile/configuration changes use
`/farmcontrol reload`. Before removing the custom villager rule or plugin,
remove its profile and reload to restore affected villagers. Never hot-load the
JAR or use the global `/reload` command.
