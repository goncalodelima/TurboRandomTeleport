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

package pt.gongas.rtp.util;

import net.minecraft.world.level.ChunkPos;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pt.gongas.rtp.CollisionType;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LocationUtil {

    public static final Set<Material> SAFE_BLOCKS = Set.of(
            Material.SHORT_GRASS,
            Material.SHORT_DRY_GRASS,
            Material.TALL_GRASS,
            Material.TALL_DRY_GRASS,
            Material.LEAF_LITTER,
            Material.CORNFLOWER,
            Material.CACTUS_FLOWER,
            Material.SUNFLOWER,
            Material.TORCHFLOWER,
            Material.WILDFLOWERS
    );

    public static @NotNull CompletableFuture<@NotNull Pair<@Nullable Integer, @NotNull CollisionType>> getHighestSafeBlockYAtAsync(World world, Location location) {

        // We load the chunk first before getting the block so it can be loaded asynchronously

        return world.getChunkAtAsync(location).thenApply(chunk -> {

            Block block = world.getHighestBlockAt(location.getBlockX(), location.getBlockZ(), HeightMap.MOTION_BLOCKING_NO_LEAVES);

            if (block.getY() > 40) {

                if (isLiquidBlock(block)) {
                    return new Pair<>(null, CollisionType.LIQUID);
                }

                Block blockAbove = block.getRelative(BlockFace.UP); // This can never be liquid because of the HeightMap

                if (isSafe(blockAbove)) {

                    Block blockTwoAbove = blockAbove.getRelative(BlockFace.UP); // This can never be liquid because of the HeightMap

                    if (isSafe(blockTwoAbove)) {
                        return new Pair<>(blockAbove.getY(), CollisionType.NONE);
                    }

                }

            }

            return new Pair<>(null, CollisionType.UNKNOWN);
        });
    }

    /* Use this method only for locations within chunks that are already loaded.
    Calling it on an unloaded chunk will trigger synchronous chunk loading via World#getHighestBlockAt. */
    public static @NotNull Pair<@Nullable Integer, @NotNull CollisionType> getHighestSafeBlockYAt(World world, Location location) {

        Block block = world.getHighestBlockAt(location.getBlockX(), location.getBlockZ(), HeightMap.MOTION_BLOCKING_NO_LEAVES);

        if (block.getY() > 40) {

            if (isLiquidBlock(block)) {
                return new Pair<>(null, CollisionType.LIQUID);
            }

            Block blockAbove = block.getRelative(BlockFace.UP); // This can never be liquid because of the HeightMap

            if (isSafe(blockAbove)) {

                Block blockTwoAbove = blockAbove.getRelative(BlockFace.UP); // This can never be liquid because of the HeightMap

                if (isSafe(blockTwoAbove)) {
                    return new Pair<>(blockAbove.getY(), CollisionType.NONE);
                }

            }

        }

        return new Pair<>(null, CollisionType.UNKNOWN);
    }

    private static boolean isSafe(Block block) {
        Material type = block.getType();
        return type.isAir() || SAFE_BLOCKS.contains(type);
    }

    private static boolean isLiquidBlock(Block block) {

        Material type = block.getType();

        if (type == Material.WATER || type == Material.LAVA || type == Material.SEAGRASS || type == Material.TALL_SEAGRASS) {
            return true;
        }

        if (block.getBlockData() instanceof Waterlogged waterlogged) {
            return waterlogged.isWaterlogged();
        }

        return false;
    }

    public static long getChunkKey(Location location) {
        return getChunkKey(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static long getChunkKey(int chunkX, int chunkZ) {
        return ChunkPos.asLong(chunkX, chunkZ);
    }

}
