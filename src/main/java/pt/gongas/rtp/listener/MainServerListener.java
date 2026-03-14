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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import pt.gongas.rtp.RtpPlugin;
import pt.gongas.rtp.WorldType;
import pt.gongas.rtp.manager.RtpManager;
import pt.gongas.rtp.store.request.TeleportRequestStore;
import pt.gongas.rtp.util.LocationLinkedBuffer;

public class MainServerListener implements Listener {

    private final RtpPlugin plugin;

    private final RtpManager rtpManager;

    private final LocationLinkedBuffer overworldLinkedBuffer;

    private final LocationLinkedBuffer netherLinkedBuffer;

    private final LocationLinkedBuffer endLinkedBuffer;

    private final TeleportRequestStore teleportRequestStore;

    public MainServerListener(RtpPlugin plugin, RtpManager rtpManager, LocationLinkedBuffer overworldLinkedBuffer, LocationLinkedBuffer netherLinkedBuffer, LocationLinkedBuffer endLinkedBuffer, TeleportRequestStore teleportRequestStore) {
        this.plugin = plugin;
        this.rtpManager = rtpManager;
        this.overworldLinkedBuffer = overworldLinkedBuffer;
        this.netherLinkedBuffer = netherLinkedBuffer;
        this.endLinkedBuffer = endLinkedBuffer;
        this.teleportRequestStore = teleportRequestStore;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        teleportRequestStore.remove(event.getPlayer().getUniqueId())
                .thenAcceptAsync(value -> {

                    if (value == null || !player.isConnected()) {
                        return;
                    }

                    WorldType worldType = WorldType.valueOf(value);
                    LocationLinkedBuffer locationLinkedBuffer;

                    if (worldType == WorldType.OVERWORLD) {
                        locationLinkedBuffer = overworldLinkedBuffer;
                    } else if (worldType == WorldType.NETHER) {
                        locationLinkedBuffer = netherLinkedBuffer;
                    } else {
                        locationLinkedBuffer = endLinkedBuffer;
                    }

                    rtpManager.teleportPlayer(player, locationLinkedBuffer);

                }, Bukkit.getScheduler().getMainThreadExecutor(plugin));


    }

}
