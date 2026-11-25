package com.froobworld.farmcontrol.controller.action;

import com.froobworld.farmcontrol.controller.ActionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Uses the optional public Canvas API on the entity's owning region.
 */
public final class CanvasAllayAction extends Action {
    private final Method setter;

    private CanvasAllayAction(String name, Method setter) {
        super(name, Allay.class, false, false, true);
        this.setter = setter;
    }

    public static void register(ActionManager actions) {
        final Class<?> controls;
        try {
            controls = Class.forName("io.canvasmc.canvas.entity.AllayFarmControl",
                    false, Bukkit.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            return;
        }
        try {
            actions.addAction(new CanvasAllayAction("skip-allay-living-entity-scan",
                    controls.getMethod("setLivingEntityScanSuppressed", boolean.class)));
            actions.addAction(new CanvasAllayAction("disable-allay-idle-flight",
                    controls.getMethod("setIdleFlightSuppressed", boolean.class)));
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Canvas allay controls have an incompatible API", exception);
        }
    }

    @Override
    public void doAction(Entity entity) {
        apply(entity, true);
    }

    @Override
    public void undoAction(Entity entity) {
        apply(entity, false);
    }

    private void apply(Entity entity, boolean suppressed) {
        try {
            setter.invoke(entity, suppressed);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to apply allay profile action " + getName(), exception);
        }
    }
}
