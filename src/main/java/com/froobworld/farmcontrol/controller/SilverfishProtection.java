package com.froobworld.farmcontrol.controller;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.froobworld.farmcontrol.FarmControl;
import com.froobworld.farmcontrol.data.FcData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Farm protections follow the same group membership and undo lifecycle as other actions. */
public final class SilverfishProtection implements Listener {
    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    private static final NamespacedKey LEGACY_COLLISIONS = NamespacedKey.fromString("frozensilverfish:collisions_off");
    private static final NamespacedKey CAMPFIRE = NamespacedKey.minecraft("campfire");

    public SilverfishProtection(FarmControl plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (var world : Bukkit.getWorlds()) {
            plugin.getHookManager().getEntityGetterHook().getEntities(world).thenAccept(entities -> {
                for (var entity : entities) {
                    if (entity instanceof Silverfish silverfish) {
                        plugin.getHookManager().getSchedulerHook().runEntityTaskAsap(
                                () -> restoreLegacy(silverfish), null, silverfish);
                    }
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Silverfish silverfish)) return;
        DamageType type = event.getDamageSource().getDamageType();
        if (type == DamageType.DROWN
                && hasAction(silverfish, "prevent-drowning")) {
            event.setCancelled(true);
        } else if (type.getKey().equals(CAMPFIRE)
                && !silverfish.isAware() && hasAction(silverfish, "push-off-campfires")) {
            pushOffCampfire(silverfish);
        }
    }

    private static boolean hasAction(Silverfish silverfish, String action) {
        FcData data = FcData.get(silverfish);
        return data != null && data.getActions().contains(action);
    }

    // Same escape impulse as FrozenSilverfish 1.1.1; see THIRD-PARTY-NOTICES.
    private static void pushOffCampfire(Silverfish silverfish) {
        Block block = silverfish.getLocation().getBlock();
        List<BlockFace> exits = new ArrayList<>(4);
        for (BlockFace side : SIDES) {
            Block next = block.getRelative(side);
            if (next.isPassable() || next.getType() == Material.CAMPFIRE
                    || next.getType() == Material.SOUL_CAMPFIRE) exits.add(side);
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector push;
        if (exits.isEmpty()) {
            double angle = random.nextDouble(Math.PI * 2);
            push = new Vector(Math.cos(angle), 0, Math.sin(angle));
        } else {
            push = exits.get(random.nextInt(exits.size())).getDirection();
        }
        silverfish.setVelocity(push.multiply(0.3).setY(0.2));
    }

    @EventHandler
    public void onEntityAdded(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof Silverfish silverfish) restoreLegacy(silverfish);
    }

    /** Restore only FrozenSilverfish-owned state, including fish in previously unloaded chunks. */
    private static void restoreLegacy(Silverfish silverfish) {
        if (silverfish.removeScoreboardTag("frozen_silverfish") && !hasAction(silverfish, "pause-silverfish")) {
            silverfish.setAware(true);
        }
        var data = silverfish.getPersistentDataContainer();
        if (data.has(LEGACY_COLLISIONS, PersistentDataType.BYTE)) {
            if (!hasAction(silverfish, "disable-collisions")) silverfish.setCollidable(true);
            data.remove(LEGACY_COLLISIONS);
        }
    }
}
