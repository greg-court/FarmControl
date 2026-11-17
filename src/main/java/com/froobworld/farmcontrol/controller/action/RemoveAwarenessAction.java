package com.froobworld.farmcontrol.controller.action;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;

public class RemoveAwarenessAction extends Action {

    public RemoveAwarenessAction() {
        this("remove-awareness", Mob.class);
    }

    public RemoveAwarenessAction(String name, Class<? extends Mob> type) {
        super(name, type, false, true, true);
    }

    @Override
    public void doAction(Entity entity) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        mob.setAware(false);
    }

    @Override
    public void undoAction(Entity entity) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        mob.setAware(true);
    }
}
