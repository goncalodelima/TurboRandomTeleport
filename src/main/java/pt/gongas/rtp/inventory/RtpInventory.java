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

package pt.gongas.rtp.inventory;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pt.gongas.rtp.RtpPlugin;
import pt.gongas.rtp.WorldType;
import pt.gongas.rtp.inventory.holder.RtpInventoryHolder;
import pt.gongas.rtp.manager.RtpManager;
import pt.gongas.rtp.store.lock.TeleportLockStore;
import pt.gongas.rtp.store.request.TeleportRequestStore;
import pt.gongas.rtp.util.LocationLinkedBuffer;
import pt.gongas.rtp.util.config.Configuration;

import java.util.*;

public class RtpInventory implements Listener {

    private final RtpPlugin plugin;

    private final RtpManager rtpManager;

    private final LocationLinkedBuffer overworldLinkedBuffer;

    private final LocationLinkedBuffer netherLinkedBuffer;

    private final LocationLinkedBuffer endLinkedBuffer;

    private final TeleportRequestStore teleportRequestStore;

    private final TeleportLockStore teleportLockStore;

    private final boolean secondaryServer;

    private final byte[] connectMessage;

    private final int rows;

    private final Component title;

    private final int overWorldSlot;

    private final int netherSlot;

    private final int endSlot;

    private final Component overworldName;

    private final List<Component> overworldLore = new ArrayList<>();

    private final Component netherName;

    private final List<Component> netherLore = new ArrayList<>();

    private final Component endName;

    private final List<Component> endLore = new ArrayList<>();

    public RtpInventory(RtpPlugin plugin, RtpManager rtpManager, Configuration inventory, LocationLinkedBuffer overworldLinkedBuffer, LocationLinkedBuffer netherLinkedBuffer, LocationLinkedBuffer endLinkedBuffer, TeleportRequestStore teleportRequestStore, TeleportLockStore teleportLockStore, boolean secondaryServer, byte[] connectMessage) {
        this.plugin = plugin;
        this.rtpManager = rtpManager;
        this.rows = inventory.getInt("rtp.size", 27);
        this.title = MiniMessage.miniMessage().deserialize(inventory.getString("rtp.title", "ʀᴀɴᴅᴏᴍ ᴛᴇʟᴇᴘᴏʀᴛ"));
        this.overWorldSlot = inventory.getInt("rtp.overWorld.slot", 11);
        this.netherSlot = inventory.getInt("rtp.netherSlot.slot", 13);
        this.endSlot = inventory.getInt("rtp.endSlot.slot", 15);
        this.overworldLinkedBuffer = overworldLinkedBuffer;
        this.netherLinkedBuffer = netherLinkedBuffer;
        this.endLinkedBuffer = endLinkedBuffer;
        this.teleportRequestStore = teleportRequestStore;
        this.teleportLockStore = teleportLockStore;

        this.overworldName = MiniMessage.miniMessage().deserialize(inventory.getString("rtp.overWorld.name", "<green>ᴏᴠᴇʀᴡᴏʀʟᴅ"));
        this.secondaryServer = secondaryServer;
        this.connectMessage = connectMessage;

        for (String string : inventory.getStringList("rtp.overWorld.lore")) {
            overworldLore.add(MiniMessage.miniMessage().deserialize(string));
        }

        this.netherName = MiniMessage.miniMessage().deserialize(inventory.getString("rtp.nether.name", "<green>ɴᴇᴛʜᴇʀ"));

        for (String string : inventory.getStringList("rtp.nether.lore")) {
            netherLore.add(MiniMessage.miniMessage().deserialize(string));
        }

        this.endName = MiniMessage.miniMessage().deserialize(inventory.getString("rtp.end.name", "<green>ᴇɴᴅ"));

        for (String string : inventory.getStringList("rtp.end.lore")) {
            endLore.add(MiniMessage.miniMessage().deserialize(string));
        }

    }

    public void openMenu(Player player) {

        RtpInventoryHolder gui = new RtpInventoryHolder(rows, title);
        Inventory inventory = gui.getInventory();

        if (overWorldSlot != -1) {
            ItemStack overworldItem = ItemStack.of(Material.GRASS_BLOCK);
            overworldItem.setData(DataComponentTypes.ITEM_NAME, overworldName);
            overworldItem.setData(DataComponentTypes.LORE, ItemLore.lore(overworldLore));
            inventory.setItem(overWorldSlot, overworldItem);
        }

        if (netherSlot != -1) {
            ItemStack netherItem = ItemStack.of(Material.NETHERRACK);
            netherItem.setData(DataComponentTypes.ITEM_NAME, netherName);
            netherItem.setData(DataComponentTypes.LORE, ItemLore.lore(netherLore));
            inventory.setItem(netherSlot, netherItem);
        }

        if (endSlot != -1) {
            ItemStack endItem = ItemStack.of(Material.END_STONE);
            endItem.setData(DataComponentTypes.ITEM_NAME, endName);
            endItem.setData(DataComponentTypes.LORE, ItemLore.lore(endLore));
            inventory.setItem(endSlot, endItem);
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getInventory().getHolder(false) instanceof RtpInventoryHolder)) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot == overWorldSlot) {
            handleClick((Player) event.getWhoClicked(), overworldLinkedBuffer, WorldType.OVERWORLD);
        } else if (slot == netherSlot) {
            handleClick((Player) event.getWhoClicked(), netherLinkedBuffer, WorldType.NETHER);
        } else if (slot == endSlot) {
            handleClick((Player) event.getWhoClicked(), endLinkedBuffer, WorldType.END);
        }

    }

    private void handleClick(Player player, LocationLinkedBuffer locationLinkedBuffer, WorldType worldType) {

        player.closeInventory();

        teleportLockStore.add(player.getUniqueId())
                .thenAcceptAsync(added -> {

                    if (!added) {
                        return;
                    }

                    if (secondaryServer) {

                        teleportRequestStore.put(player.getUniqueId(), worldType.name())
                                .thenAcceptAsync(stored -> {

                                    if (!stored) {
                                        teleportLockStore.remove(player.getUniqueId());
                                        return;
                                    }

                                    player.sendPluginMessage(plugin, "BungeeCord", connectMessage);

                                }, Bukkit.getScheduler().getMainThreadExecutor(plugin));

                    } else {
                        rtpManager.teleportPlayer(player, locationLinkedBuffer);
                    }

                }, Bukkit.getScheduler().getMainThreadExecutor(plugin));

    }

}
