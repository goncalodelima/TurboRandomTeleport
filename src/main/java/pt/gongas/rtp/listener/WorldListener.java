/*
 *
 *  * This file is part of TurboRandomTeleport - https://github.com/goncalodelima/TurboRandomTeleport
 *  * Copyright (c) 2026 goncalodelima and contributors
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package pt.gongas.rtp.listener;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pt.gongas.rtp.RtpPlugin;
import pt.gongas.rtp.WorldType;
import pt.gongas.rtp.runnable.LinkedBufferRunnable;
import pt.gongas.rtp.util.LocationLinkedBuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class WorldListener implements Listener {

    private final RtpPlugin plugin;

    private final Logger logger;

    private final Map<String, LinkedBufferRunnable> runnableMap = new HashMap<>();

    private final int viewDistance;

    private final int range;

    public WorldListener(RtpPlugin plugin, Logger logger, int viewDistance, int range) {

        this.plugin = plugin;
        this.logger = logger;
        this.viewDistance = viewDistance;
        this.range = range;

        // Scheduled 5 ticks later to ensure world management plugins have already loaded the worlds

        new BukkitRunnable() {
            @Override
            public void run() {

                for (WorldType worldType : WorldType.values()) {

                    String worldName = worldType.getWorldName();

                    if (!runnableMap.containsKey(worldName)) {

                        World world = Bukkit.getWorld(worldName);

                        if (world != null) {
                            loadResourcesWorld(world, worldType, worldName);
                        }

                    }

                }

            }
        }.runTaskLater(plugin, 5);

    }

    /**
     * If a world loads at runtime, schedule a task to populate the buffer.
     * Also used on plugin startup for plugins like WorldManager that load worlds later.
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();
        WorldType worldType = WorldType.getByWorldName(worldName);
        loadResourcesWorld(world, worldType, worldName);
    }

    /**
     * If a world unloads at runtime, cancel the task responsible for populating its buffer.
     * Prevents tasks from running for worlds that are no longer loaded.
     */
    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {

        LinkedBufferRunnable runnable = runnableMap.remove(event.getWorld().getName());

        if (runnable != null) {
            runnable.cancel();
        }

    }

    /**
     * If a buffered location belongs to the chunk that was just loaded, remove it.
     * The player who loaded the chunk may modify it and make the location invalid.
     * This also ensures RTP only teleports players to previously unexplored chunks.
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {

        Chunk chunk = event.getChunk();

        String worldName = event.getWorld().getName();
        WorldType worldType = WorldType.getByWorldName(worldName);

        if (worldType != null) {
            worldType.getLinkedBuffer().invalidateChunk(chunk);
        }

    }

    private void loadResourcesWorld(World world, WorldType worldType, String worldName) {

        if (worldType != null) {
            loadResourcesWorldByType(world, worldName, worldType.getLinkedBuffer());
        }

    }

    private void loadResourcesWorldByType(World world, String worldName, LocationLinkedBuffer linkedBuffer) {
        LinkedBufferRunnable runnable = new LinkedBufferRunnable(plugin, logger, linkedBuffer, world, viewDistance, range);
        runnableMap.put(worldName, runnable);
        runnable.runTaskTimer(plugin, 20, 20);
    }

}
