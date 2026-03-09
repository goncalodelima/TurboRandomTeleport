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
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pt.gongas.rtp.inventory.holder.RtpInventoryHolder;
import pt.gongas.rtp.util.ExpiringMap;
import pt.gongas.rtp.util.LocationLinkedBuffer;
import pt.gongas.rtp.util.config.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RtpInventory implements Listener {

    private final LocationLinkedBuffer overworldLinkedBuffer;

    private final LocationLinkedBuffer netherLinkedBuffer;

    private final LocationLinkedBuffer endLinkedBuffer;

    private final ExpiringMap<UUID, Long> cooldownPlayers;

    private final Component teleportMessage;

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

    private final PotionEffect resistanceEffect = new PotionEffect(PotionEffectType.RESISTANCE, 20 * 5, 255);

    private final PotionEffect weaknessEffect = new PotionEffect(PotionEffectType.WEAKNESS, 20 * 5, 255);

    private static final Sound TELEPORT_SOUND = Sound.sound(Key.key("entity.enderman.teleport"), Sound.Source.UI, 1f, 1f);

    public RtpInventory(Configuration lang, Configuration inventory, LocationLinkedBuffer overworldLinkedBuffer, LocationLinkedBuffer netherLinkedBuffer, LocationLinkedBuffer endLinkedBuffer, ExpiringMap<UUID, Long> cooldownPlayers) {

        this.rows = inventory.getInt("rtp.size", 27);
        this.title = MiniMessage.miniMessage().deserialize(inventory.getString("rtp.title", "ʀᴀɴᴅᴏᴍ ᴛᴇʟᴇᴘᴏʀᴛ"));
        this.overWorldSlot = inventory.getInt("rtp.overWorld.slot", 11);
        this.netherSlot = inventory.getInt("rtp.netherSlot.slot", 13);
        this.endSlot = inventory.getInt("rtp.endSlot.slot", 15);
        this.overworldLinkedBuffer = overworldLinkedBuffer;
        this.netherLinkedBuffer = netherLinkedBuffer;
        this.endLinkedBuffer = endLinkedBuffer;
        this.cooldownPlayers = cooldownPlayers;
        this.teleportMessage = MiniMessage.miniMessage().deserialize(lang.getString("teleport-success", "<green>You have been successfully teleported! You are invincible for 5 seconds..."));

        this.overworldName = MiniMessage.miniMessage().deserialize(inventory.getString("rtp.overWorld.name", "<green>ᴏᴠᴇʀᴡᴏʀʟᴅ"));

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
            handleClick(event.getWhoClicked(), overworldLinkedBuffer);
        } else if (slot == netherSlot) {
            handleClick(event.getWhoClicked(), netherLinkedBuffer);
        } else if (slot == endSlot) {
            handleClick(event.getWhoClicked(), endLinkedBuffer);
        }

    }

    private void handleClick(HumanEntity player, LocationLinkedBuffer locationLinkedBuffer) {

        player.closeInventory();

        // Pop a location from the buffer in a "pending" state.
        // This ensures the location is temporarily reserved and not taken by another teleport
        // while the async teleport is in progress. The occupied counter will only be decremented
        // if the teleport succeeds (via confirmPop()), otherwise the location is returned to the buffer.
        Location location = locationLinkedBuffer.popPending();

        if (location == null) {
            return;
        }

        player.teleportAsync(location.add(0.5, 0, 0.5)).thenAccept(success -> {

            if (success) {

                locationLinkedBuffer.confirmPop();

                // Potions are used to give the player resistance so that they don't take damage,
                // and they can't damage anyone in the first 5 seconds of teleportation.
                player.addPotionEffect(resistanceEffect);
                player.addPotionEffect(weaknessEffect);

                player.playSound(TELEPORT_SOUND, Sound.Emitter.self());
                player.sendMessage(teleportMessage);

            } else {
                locationLinkedBuffer.push(location, location.getBlockX() >> 4, location.getBlockZ() >> 4);
            }

        });

        cooldownPlayers.put(player.getUniqueId(), System.currentTimeMillis());
    }

}
