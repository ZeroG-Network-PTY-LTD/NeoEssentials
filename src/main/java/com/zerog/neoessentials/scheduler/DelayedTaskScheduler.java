package com.zerog.neoessentials.scheduler;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Runs tasks a fixed number of server ticks in the future.
 *
 * <p>Deliberately avoids vanilla's {@code MinecraftServer#tell(TickTask)} — its erased
 * method signature has proven fragile across Minecraft versions (see the crash report
 * where {@code VanishManager} triggered a {@code NoSuchMethodError} on
 * {@code MinecraftServer.tell(Runnable)} against a 1.21.8 server despite compiling
 * cleanly against 1.21.1). This scheduler only depends on NeoForge's public
 * {@link ServerTickEvent}, which is stable across supported versions.</p>
 */
@EventBusSubscriber(modid = "neoessentials")
public class DelayedTaskScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DelayedTaskScheduler.class);

    private record DelayedTask(int dueTick, Runnable task) {}

    private static final ConcurrentLinkedQueue<DelayedTask> QUEUE = new ConcurrentLinkedQueue<>();
    private static volatile int tickCounter = 0;

    private DelayedTaskScheduler() {}

    /**
     * Schedule {@code task} to run on the server thread after {@code delayTicks} server ticks.
     * Safe to call from any thread; execution always happens during {@link ServerTickEvent.Post}.
     */
    public static void schedule(int delayTicks, Runnable task) {
        QUEUE.add(new DelayedTask(tickCounter + Math.max(delayTicks, 0), task));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        Iterator<DelayedTask> it = QUEUE.iterator();
        while (it.hasNext()) {
            DelayedTask dt = it.next();
            if (tickCounter >= dt.dueTick()) {
                it.remove();
                try {
                    dt.task().run();
                } catch (Exception e) {
                    LOGGER.error("Delayed task failed", e);
                }
            }
        }
    }
}
