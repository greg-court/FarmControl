package com.froobworld.farmcontrol.hook.entitygetter;

import com.froobworld.farmcontrol.controller.entity.SnapshotEntity;
import com.froobworld.farmcontrol.FarmControl;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BukkitEntityGetterHook implements EntityGetterHook {
    private final FarmControl farmControl;

    public BukkitEntityGetterHook(FarmControl farmControl) {
        this.farmControl = farmControl;
    }

    @Override
    public CompletableFuture<Collection<Entity>> getEntities(World world, Class<?>... classes) {
        return CompletableFuture.completedFuture(world.getEntitiesByClasses(classes));
    }

    @Override
    public CompletableFuture<Collection<Entity>> getEntities(World world) {
        return CompletableFuture.completedFuture(world.getEntities());
    }

    @Override
    public CompletableFuture<List<SnapshotEntity>> getSnapshotEntities(World world, Class<?>... classes) {
        return CompletableFuture.completedFuture(snapshotEntities(world.getEntitiesByClasses(classes)));
    }

    @Override
    public CompletableFuture<List<SnapshotEntity>> getSnapshotEntities(World world) {
        return CompletableFuture.completedFuture(snapshotEntities(world.getEntities()));
    }

    private List<SnapshotEntity> snapshotEntities(Collection<Entity> entities) {
        int workstationDistance = farmControl.getFcConfig().villagerWorkstationDistance.get();
        List<SnapshotEntity> snapshots = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            snapshots.add(new SnapshotEntity(entity, workstationDistance));
        }
        return snapshots;
    }
}
