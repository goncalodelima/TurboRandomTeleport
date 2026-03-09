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

package pt.gongas.rtp;

import co.aikar.commands.BukkitCommandManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import pt.gongas.rtp.command.RtpCommand;
import pt.gongas.rtp.inventory.RtpInventory;
import pt.gongas.rtp.inventory.holder.RtpInventoryHolder;
import pt.gongas.rtp.listener.WorldListener;
import pt.gongas.rtp.util.ExpiringMap;
import pt.gongas.rtp.util.LocationLinkedBuffer;
import pt.gongas.rtp.util.config.Configuration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class RtpPlugin extends JavaPlugin {

    private final List<LocationLinkedBuffer> linkedBuffers = new LinkedList<>();

    private Random random;

    private Configuration data;

    private Metrics metrics;

    @Override
    public void onEnable() {

        random = new Random();

        saveDefaultConfig();

        data = new Configuration(this, "data", "data.yml");
        data.saveDefaultConfig();

        Configuration lang = new Configuration(this, "lang", "lang.yml");
        lang.saveDefaultConfig();

        Configuration inventory = new Configuration(this, "inventory", "inventory.yml");
        inventory.saveDefaultConfig();

        long commandCooldown = Math.min(1, getConfig().getLong("commandCooldown", 15));

        int viewDistance = getServer().getViewDistance();
        int bufferSize = Math.max(2, getConfig().getInt("world.buffer-size", 100));
        int range = Math.max(1, getConfig().getInt("world.generate-coordenates-range", 1_000_000));

        LocationLinkedBuffer overworldLinkedBuffer = new LocationLinkedBuffer(bufferSize);
        LocationLinkedBuffer netherLinkedBuffer = new LocationLinkedBuffer(bufferSize);
        LocationLinkedBuffer endLinkedBuffer = new LocationLinkedBuffer(bufferSize);

        loadBuffer(overworldLinkedBuffer, "buffers.overworld");
        loadBuffer(netherLinkedBuffer, "buffers.nether");
        loadBuffer(endLinkedBuffer, "buffers.end");

        WorldType.OVERWORLD.setLinkedBuffer(overworldLinkedBuffer);
        WorldType.NETHER.setLinkedBuffer(netherLinkedBuffer);
        WorldType.END.setLinkedBuffer(endLinkedBuffer);

        linkedBuffers.add(overworldLinkedBuffer);
        linkedBuffers.add(netherLinkedBuffer);
        linkedBuffers.add(endLinkedBuffer);

        ExpiringMap<UUID, Long> cooldownPlayers = new ExpiringMap<>(this, 20 * 30); // 30 seconds

        RtpInventory rtpInventory = new RtpInventory(lang, inventory, overworldLinkedBuffer, netherLinkedBuffer, endLinkedBuffer, cooldownPlayers);
        getServer().getPluginManager().registerEvents(rtpInventory, this);

        BukkitCommandManager commandManager = new BukkitCommandManager(this);
        commandManager.registerCommand(new RtpCommand(lang, rtpInventory, cooldownPlayers, commandCooldown * 1000));

        getServer().getPluginManager().registerEvents(new WorldListener(this, getLogger(), viewDistance, range), this);

        // BStats Metrics
        metrics = new Metrics(this, 30015);
    }

    @Override
    public void onDisable() {

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof RtpInventoryHolder) {
                player.closeInventory();
            }

        }

        if (data != null) {

            int pos = 0;
            String[] worlds = {"overworld", "nether", "end"};

            outer:
            for (LocationLinkedBuffer linkedBuffer : linkedBuffers) {

                List<Map<String, Object>> serializedLocations = new ArrayList<>();

                for (Location location : linkedBuffer.getAllLocations()) {

                    Map<String, Object> map = serializeLocation(location);

                    if (map == null) {
                        continue outer;
                    }

                    serializedLocations.add(map);
                }

                String path = "buffers." + worlds[pos];
                data.set(path, serializedLocations.isEmpty() ? null : serializedLocations);

                pos++;
            }

            data.saveConfig();

        }

        if (metrics != null) {
            metrics.shutdown();
        }

    }

    private @Nullable Map<String, Object> serializeLocation(Location location) {

        World world = location.getWorld();

        if (world == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("world", world.getName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        map.put("yaw", location.getYaw());
        map.put("pitch", location.getPitch());
        return map;
    }

    private Location deserializeLocation(Map<String, Object> map) {
        String worldName = (String) map.get("world");
        double x = (double) map.get("x");
        double y = (double) map.get("y");
        double z = (double) map.get("z");
        float yaw = ((Double) map.get("yaw")).floatValue();
        float pitch = ((Double) map.get("pitch")).floatValue();
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    private void loadBuffer(LocationLinkedBuffer buffer, String path) {

        List<Map<?, ?>> list = data.getMapList(path);

        for (Map<?, ?> map : list) {
            Location loc = deserializeLocation((Map<String, Object>) map);
            buffer.push(loc, loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        }

    }

    public Random getRandom() {
        return random;
    }

}
