package com.froobworld.farmcontrol.controller.action;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Silverfish;

/** The damage listener reads native action ownership; no extra entity flags are needed. */
public final class SilverfishProtectionAction extends Action {
    public SilverfishProtectionAction(String name) {
        super(name, Silverfish.class, false, true, true);
    }

    @Override
    public void doAction(Entity entity) {}

    @Override
    public void undoAction(Entity entity) {}
}
