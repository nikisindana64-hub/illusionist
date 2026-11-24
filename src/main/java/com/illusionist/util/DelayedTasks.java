package com.illusionist.util;

import com.illusionist.IllusionistMod;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Простейший планировщик: выполнить действие через N тиков (для «медленных» событий). */
@EventBusSubscriber(modid = IllusionistMod.MODID)
public final class DelayedTasks {
    private record Task(ServerLevel level, long due, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private DelayedTasks() {
    }

    public static void schedule(ServerLevel level, int delayTicks, Runnable action) {
        TASKS.add(new Task(level, level.getGameTime() + delayTicks, action));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (TASKS.isEmpty()) {
            return;
        }
        List<Task> ready = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.level().getGameTime() >= t.due()) {
                ready.add(t);
                it.remove();
            }
        }
        for (Task t : ready) {
            t.action().run();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TASKS.clear();
    }
}
