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

package pt.gongas.rtp.runnable;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import pt.gongas.rtp.CollisionType;
import pt.gongas.rtp.RtpPlugin;
import pt.gongas.rtp.util.LocationLinkedBuffer;
import pt.gongas.rtp.util.LocationUtil;
import pt.gongas.rtp.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LinkedBufferRunnable extends BukkitRunnable {

    private final RtpPlugin plugin;

    private final Logger logger;

    private final LocationLinkedBuffer linkedBuffer;

    private final World world;

    private final int viewDistance;

    private int pendingTasks = 0;

    private final int range;

    private static final int[][] CHUNK_OFFSETS = {
            {0, 0},
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1},
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1}
    };

    private static final int CHUNK_SIZE = 16;

    public LinkedBufferRunnable(RtpPlugin plugin, Logger logger, LocationLinkedBuffer linkedBuffer, World world, int viewDistance, int range) {
        this.plugin = plugin;
        this.logger = logger;
        this.linkedBuffer = linkedBuffer;
        this.world = world;
        this.viewDistance = viewDistance;
        this.range = range;
    }

    @Override
    public void run() {

        if (linkedBuffer.occupied() + pendingTasks < linkedBuffer.capacity()) {

            Location location = generateRandomCoordinates();
            pendingTasks++;

            // The 2nd argument (0) represents the first attempt
            attemptLocation(location, 0);
        }

    }

    private void attemptLocation(Location originalLocation, int attempt) {

        // All adjacent chunks to the originally chosen chunk have been checked and no valid location was found
        if (attempt >= CHUNK_OFFSETS.length) {
            pendingTasks--;
            return;
        }

        int offsetX = CHUNK_OFFSETS[attempt][0];
        int offsetZ = CHUNK_OFFSETS[attempt][1];

        int cx = (originalLocation.getBlockX() >> 4) + offsetX;
        int cz = (originalLocation.getBlockZ() >> 4) + offsetZ;

        // Use chunk base coordinates instead of location#getBlockX/Z
        // because the location might be anywhere in the chunk; we want the chunk's edge
        int baseX = cx << 4;
        int baseZ = cz << 4;

        Location location = originalLocation.clone();

        location.setX(baseX + plugin.getRandom().nextInt(CHUNK_SIZE));
        location.setZ(baseZ + plugin.getRandom().nextInt(CHUNK_SIZE));

        isLocationValid(location)
                .thenCompose(validY -> {

                    if (validY == null) {
                        // The cast is necessary so that the compiler in the thenAccept block below
                        // recognizes that the first element of the pair is a Location
                        return CompletableFuture.completedFuture(new Pair<>((Location) null, false));
                    }

                    if (validY == -1) {
                        // More efficient to scan blocks in an existing chunk for a valid location
                        // than picking a random location that may generate a new chunk
                        Location safeLocation = findSafeLocationInChunkSync(baseX, baseZ);
                        return CompletableFuture.completedFuture(new Pair<>(safeLocation, true));
                    }

                    location.setY(validY);

                    // Pre-generate the chunks around the player's teleport location
                    // (handles worst case by generating the maximum chunks the server allows)
                    return preloadNearbyChunks(cx, cz).thenApply(v -> new Pair<>(location, true));

                })
                .thenAccept(pair -> {

                    if (pair.value()) {

                        Location safeLocation = pair.key();

                        if (safeLocation != null) {
                            // A safe location was found; add it to the buffer
                            linkedBuffer.push(safeLocation, cx, cz);
                            pendingTasks--;
                        } else {
                            // Try to find a valid location in an adjacent chunk
                            attemptLocation(originalLocation, attempt + 1);
                        }

                    } else {
                        // This location is in an already generated chunk (possibly explored) or ocean.
                        // Adjacent chunks are likely generated too and probably ocean as well,
                        // so instead of searching nearby, we just pick a new random location.
                        pendingTasks--;
                    }

                })
                .exceptionally(e -> {
                    pendingTasks--;
                    logger.log(Level.SEVERE, "Exception generating location or loading chunks: " + location, e);
                    return null;
                });
    }

    private CompletableFuture<Integer> isLocationValid(Location location) {
        return world.getChunkAtAsync(location, false)
                .thenCompose(chunk -> {

                    // This chunk is already generated or is an ocean
                    if (chunk != null || world.getBiome(location).getKey().getKey().contains("ocean")) {
                        return CompletableFuture.completedFuture(null);
                    }

                    // No collision -> return valid block Y; otherwise return -1 so caller knows to try this or adjacent chunks.
                    // Valid blocks always have Y > 40 to avoid ravines, caves, etc.
                    return LocationUtil.getHighestSafeBlockYAtAsync(world, location)
                            .thenApply(pair -> pair.value() == CollisionType.NONE ? pair.key() : Integer.valueOf(-1));
                });
    }

    private Location findSafeLocationInChunkSync(int baseX, int baseZ) {

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                Location location = new Location(world, baseX + x, 0, baseZ + z);
                Pair<Integer, CollisionType> pair = LocationUtil.getHighestSafeBlockYAt(world, location);

                if (pair.value() == CollisionType.NONE) {
                    location.setY(pair.key());
                    return location;
                }

            }
        }

        return null;
    }

    private CompletableFuture<Void> preloadNearbyChunks(int cx, int cz) {

        List<CompletableFuture<Chunk>> futures = new ArrayList<>();

        for (int dx = -viewDistance; dx <= viewDistance; dx++) {

            for (int dz = -viewDistance; dz <= viewDistance; dz++) {

                if (dx != 0 || dz != 0) {
                    futures.add(world.getChunkAtAsync(cx + dx, cz + dz));
                }

            }

        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private Location generateRandomCoordinates() {
        int x = plugin.getRandom().nextInt(-range, range);
        int z = plugin.getRandom().nextInt(-range, range);
        return new Location(world, x, 0, z);
    }

}
