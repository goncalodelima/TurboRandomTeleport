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

package pt.gongas.rtp.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import pt.gongas.rtp.inventory.RtpInventory;
import pt.gongas.rtp.util.ExpiringMap;
import pt.gongas.rtp.util.config.Configuration;

import java.util.UUID;

@CommandAlias("rtp|randomtp|randomteleport")
public class RtpCommand extends BaseCommand {

    private final RtpInventory rtpInventory;

    private final ExpiringMap<UUID, Long> cooldownPlayers;

    private final Component cooldownMessage;

    private final long commandCooldown;

    public RtpCommand(Configuration lang, RtpInventory rtpInventory, ExpiringMap<UUID, Long> cooldownPlayers, long commandCooldown) {
        this.rtpInventory = rtpInventory;
        this.cooldownPlayers = cooldownPlayers;
        this.cooldownMessage = MiniMessage.miniMessage().deserialize(lang.getString("rtp-cooldown", "<red>Please wait a few seconds before typing this command again."));
        this.commandCooldown = commandCooldown;
    }

    @Default
    @Description("Opens the RTP Menu")
    public void randomTeleport(Player player) {

        Long cooldown = cooldownPlayers.get(player.getUniqueId());

        if (cooldown != null && System.currentTimeMillis() - cooldown < commandCooldown) {
            player.sendMessage(cooldownMessage);
            return;
        }

        rtpInventory.openMenu(player);
    }

}
