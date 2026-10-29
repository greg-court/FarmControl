package com.froobworld.farmcontrol.controller;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.froobworld.farmcontrol.FarmControl;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTameEvent;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps a persistent owner census for the three vanilla pets that follow their player. */
public final class FollowerPetTamingLimit implements Listener {
    private final FarmControl plugin;
    private final File file;
    private final Map<UUID, UUID> owners = new HashMap<>();
    // Count in-flight successful candidates atomically across independent region threads.
    private final Map<UUID, UUID> reservations = new HashMap<>();

    public FollowerPetTamingLimit(FarmControl plugin) throws Exception {
        this.plugin = plugin;
        // Preserve the existing census path for in-place upgrades.
        file = new File(plugin.getDataFolder(), "wolf-owners.yml");
        if (plugin.getFcConfig().followerPetTamingLimit.get() > 0 && !file.isFile()) {
            throw new IllegalStateException("Seed wolf-owners.yml from the stopped world's entity files before enabling the follower-pet taming limit");
        }
        if (file.isFile()) {
            var data = new YamlConfiguration();
            data.load(file);
            for (String pet : data.getKeys(false)) {
                owners.put(UUID.fromString(pet), UUID.fromString(data.getString(pet)));
            }
        }
        // A newly installed census must be seeded from the stopped world's entity
        // files. Loaded pets reconcile thereafter without loading extra chunks.
        for (var world : Bukkit.getWorlds()) {
            plugin.getHookManager().getEntityGetterHook().getEntities(world, Wolf.class, Cat.class, Parrot.class)
                    .thenAccept(entities -> entities.forEach(entity ->
                            plugin.getHookManager().getSchedulerHook().runEntityTask(
                                    () -> reconcile(entity), null, entity)));
        }
        save();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Follower-pet census: " + owners.size() + " owned pets; tame limit "
                + plugin.getFcConfig().followerPetTamingLimit.get());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public synchronized void limitTaming(EntityTameEvent event) {
        if (!isFollowerPet(event.getEntity()) || !(event.getOwner() instanceof Player player)) return;
        int limit = plugin.getFcConfig().followerPetTamingLimit.get();
        if (limit <= 0) return;
        UUID owner = player.getUniqueId();
        long count = owners.values().stream().filter(owner::equals).count()
                + reservations.values().stream().filter(owner::equals).count();
        if (count >= limit) {
            event.setCancelled(true);
            player.sendMessage("You have reached the " + limit
                    + "-pet taming limit for dogs, cats and parrots.");
        } else {
            reservations.put(event.getEntity().getUniqueId(), owner);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public synchronized void recordTaming(EntityTameEvent event) {
        UUID pet = event.getEntity().getUniqueId();
        reservations.remove(pet);
        // A later listener may cancel the tame: release its reservation without recording it.
        if (!event.isCancelled() && isFollowerPet(event.getEntity())) {
            setOwner(pet, event.getOwner().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void addedToWorld(EntityAddToWorldEvent event) {
        reconcile(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public synchronized void died(EntityDeathEvent event) {
        if (isFollowerPet(event.getEntity()) && owners.remove(event.getEntity().getUniqueId()) != null) save();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public synchronized void removed(EntityRemoveEvent event) {
        if (isFollowerPet(event.getEntity()) && event.getCause() != EntityRemoveEvent.Cause.UNLOAD
                // A shoulder parrot still belongs to its owner; it is stored with the player.
                && event.getCause() != EntityRemoveEvent.Cause.PICKUP
                && owners.remove(event.getEntity().getUniqueId()) != null) save();
    }

    private static boolean isFollowerPet(Entity entity) {
        return entity instanceof Wolf || entity instanceof Cat || entity instanceof Parrot;
    }

    private void reconcile(Entity entity) {
        if (!isFollowerPet(entity) || !(entity instanceof Tameable pet)) return;
        UUID owner = pet.isTamed() && pet.getOwner() != null ? pet.getOwner().getUniqueId() : null;
        setOwner(entity.getUniqueId(), owner);
    }

    private synchronized void setOwner(UUID pet, UUID owner) {
        UUID previous = owner == null ? owners.remove(pet) : owners.put(pet, owner);
        if (!java.util.Objects.equals(previous, owner)) save();
    }

    private synchronized void save() {
        var data = new YamlConfiguration();
        owners.forEach((pet, owner) -> data.set(pet.toString(), owner.toString()));
        try {
            Files.createDirectories(file.toPath().getParent());
            var temporary = file.toPath().resolveSibling("wolf-owners.yml.tmp");
            Files.writeString(temporary, data.saveToString());
            Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot save the follower-pet ownership census", exception);
        }
    }
}
