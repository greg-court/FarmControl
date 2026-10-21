package com.froobworld.farmcontrol.controller;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.froobworld.farmcontrol.FarmControl;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityRemoveEvent;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Ownership census persists across chunk unloads; never removes or edits wolves. */
public final class WolfTamingLimit implements Listener {
    private final FarmControl plugin;
    private final File file;
    private final Map<UUID, UUID> owners = new HashMap<>();

    public WolfTamingLimit(FarmControl plugin) throws Exception {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "wolf-owners.yml");
        if (plugin.getFcConfig().wolfTamingLimit.get() > 0 && !file.isFile()) {
            throw new IllegalStateException("Seed wolf-owners.yml from the stopped world's entity files before enabling the global taming limit");
        }
        if (file.isFile()) {
            var data = new YamlConfiguration();
            data.load(file);
            for (String wolf : data.getKeys(false)) {
                owners.put(UUID.fromString(wolf), UUID.fromString(data.getString(wolf)));
            }
        }
        // A newly installed census must be seeded from the stopped world's entity
        // files. Loaded wolves reconcile thereafter without loading extra chunks.
        for (var world : Bukkit.getWorlds()) {
            for (Wolf wolf : world.getEntitiesByClass(Wolf.class)) reconcile(wolf);
        }
        save();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Wolf census: " + owners.size() + " owned wolves; tame limit "
                + plugin.getFcConfig().wolfTamingLimit.get());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void limitTaming(EntityTameEvent event) {
        if (!(event.getEntity() instanceof Wolf) || !(event.getOwner() instanceof Player player)) return;
        int limit = plugin.getFcConfig().wolfTamingLimit.get();
        if (limit <= 0) return;
        long count = owners.values().stream().filter(player.getUniqueId()::equals).count();
        if (count >= limit) {
            event.setCancelled(true);
            player.sendMessage("You already own " + count + " dogs. The taming limit is " + limit + ".");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordTaming(EntityTameEvent event) {
        if (event.getEntity() instanceof Wolf wolf) {
            setOwner(wolf.getUniqueId(), event.getOwner().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void addedToWorld(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof Wolf wolf) reconcile(wolf);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void died(EntityDeathEvent event) {
        if (event.getEntity() instanceof Wolf wolf && owners.remove(wolf.getUniqueId()) != null) save();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void removed(EntityRemoveEvent event) {
        if (event.getEntity() instanceof Wolf wolf && event.getCause() != EntityRemoveEvent.Cause.UNLOAD
                && owners.remove(wolf.getUniqueId()) != null) save();
    }

    private void reconcile(Wolf wolf) {
        UUID owner = wolf.isTamed() && wolf.getOwner() != null ? wolf.getOwner().getUniqueId() : null;
        setOwner(wolf.getUniqueId(), owner);
    }

    private void setOwner(UUID wolf, UUID owner) {
        UUID previous = owner == null ? owners.remove(wolf) : owners.put(wolf, owner);
        if (!java.util.Objects.equals(previous, owner)) save();
    }

    private void save() {
        var data = new YamlConfiguration();
        owners.forEach((wolf, owner) -> data.set(wolf.toString(), owner.toString()));
        try {
            Files.createDirectories(file.toPath().getParent());
            var temporary = file.toPath().resolveSibling("wolf-owners.yml.tmp");
            Files.writeString(temporary, data.saveToString());
            Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot save the wolf ownership census", exception);
        }
    }
}
