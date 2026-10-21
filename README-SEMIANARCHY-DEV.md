# Villager and wolf limits — development trial only

This branch is based on upstream FarmControl commit
`728efcfae298706f804ad08f2d90d5fd02f6b7e4` (1.3.0). It is not queued for
production. The neighbouring report directory retains the exact configurations,
world fixture scripts, observations, JAR and repository queue revision.

## Changes

- Add `category:unemployed-villager-without-home`: snapshot a villager's
  profession `NONE` and absence of `MemoryKey.HOME` using Bukkit's public API.
  The category is checked by upstream grouping before an entity can count or
  connect a group. Nitwits and employed/bed-linked villagers do not qualify.
- The dev profile uses count **10**, distance **2**, action **remove-awareness**.
  Grouping, scheduling, action ownership, persistence and undo stay upstream.
  No collision code, scoreboard teams, NoAI handling or breeding census changes.
- Add a Paper event listener blocking further wolf taming at 10 owned wolves.
  Existing pets are never edited/deleted. Breeding remains unrestricted. Owned
  puppies count towards subsequent taming attempts. No permission bypass.
- The wolf UUID-to-owner census is `plugins/FarmControl/wolf-owners.yml`.
  Native entity additions reconcile ownership; deaths/permanent removals release
  slots; chunk unloads retain them. Writes use a same-directory atomic rename.
  All of this listener runs on Paper's server thread; this experimental build
  does not claim Folia support.

## Build and operate

Build with JDK 17 and the upstream wrapper:

    JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew --no-daemon --offline build

Output: `build/libs/FarmControl-1.3.0-semianarchy-dev.3.jar`.
First install of the global wolf limit requires a complete ownership census of
all stopped-world entity files. `../scan-wolves.py` performs the read-only scan
of this fixed secondary clone, including passengers, then writes the derived
census. It does not load chunks into Minecraft. An enabled limit refuses to start
without its census file. Back up the census with the world's normal backups.

FarmControl's own `/farmcontrol reload` changes profile thresholds/actions and
the taming limit. Replacing the JAR requires a normal server restart. Restore
normal awareness through its supported profile removal/reload before permanent
uninstall. Do not hot-load or use the global `/reload`.

An abrupt crash can leave the separately saved census out of step with the
world's last entity save; a stopped-world census rebuild is the recovery path.
It is not a transactional extension to Minecraft's saves. Other plugins that
change ownership of an already-loaded wolf without a vanilla tame event are
reconciled when that entity next enters a world. Arbitrary offline world edits
also require a census rebuild. Neither caveat modifies or deletes a pet.

## Findings and promotion status

The general selector and native reload recovery work, but **do not promote this
profile as-is**: an unaware baby villager in one-block-deep water suffered
repeated drowning damage while the active control baby stayed healthy. Native
`remove-awareness` pauses behaviour needed for floating; passive water forces
and ageing continue. Collisions and ordinary cramming are deliberately retained.
The owner must decide whether to restrict this experiment to adults or use a
different approach. No such scope change has been silently applied.

The report distinguishes completed client actions, native API fixture actions,
failed fixture assumptions and still-unverified behaviour. Synthetic clients
with translated protocols were not a reliable substitute for a human movement
check on the full plugin stack; do not label that check successful without its
recorded server-coordinate evidence.
