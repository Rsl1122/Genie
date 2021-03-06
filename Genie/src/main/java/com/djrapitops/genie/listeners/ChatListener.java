package com.djrapitops.genie.listeners;

import com.djrapitops.genie.Genie;
import com.djrapitops.genie.MessageType;
import com.djrapitops.genie.Messages;
import com.djrapitops.genie.Settings;
import com.djrapitops.genie.lamp.Lamp;
import com.djrapitops.genie.lamp.LampItem;
import com.djrapitops.genie.lamp.LampManager;
import com.djrapitops.plugin.settings.ColorScheme;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * @author AuroraLS3
 */
public class ChatListener implements Listener {

    private final Genie plugin;

    public ChatListener(Genie plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) {
            return;
        }
        ItemStack item = getItemInHand(player).filter(LampItem::isLampItem)
                .orElse(null);
        if (item == null) return;
        ColorScheme color = plugin.getColorScheme();
        String mCol = color.getMainColor();
        String sCol = color.getSecondaryColor();

        Optional<UUID> lampUUID = LampItem.getLampUUID(item.getItemMeta());
        if (!lampUUID.isPresent()) return;

        LampManager lampManager = plugin.getLampManager();
        Lamp lamp = lampManager.getLamp(lampUUID.get());
        Messages msg = plugin.getMsg();

        String prefix = mCol + "[Genie] " + sCol;
        if (!lamp.hasWishesLeft()) {
            player.sendMessage(prefix + msg.getMessage(MessageType.OUT_OF_WISHES));
            return;
        }
        String message = event.getMessage().toLowerCase();

        if (makeAWish(player, message)) {
            lampManager.wish(lamp);
            String wishesLeft = color.getTertiaryColor() + "" + lamp.getWishes() + sCol;

            player.sendMessage(prefix + msg.getMessage(MessageType.WISHES_LEFT).replace("WISHES", wishesLeft));

            String fulfillMsg = mCol + "[Genie] " + ChatColor.GOLD + msg.getMessage(MessageType.FULFILL);

            if (plugin.getConfig().getBoolean(Settings.ANNOUNCE_WISH_FULFILL.getPath())) {
                player.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(fulfillMsg));
            } else {
                player.sendMessage(fulfillMsg);
            }
        } else {
            player.sendMessage(prefix + plugin.getMsg().getMessage(MessageType.CANNOT_FIND));
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    private Optional<ItemStack> getItemInHand(Player player) {
        try {
            return Optional.ofNullable(player.getInventory().getItemInMainHand());
        } catch (Throwable e) {
            return getItemInHandOldVersion(player);
        }
    }

    private Optional<ItemStack> getItemInHandOldVersion(Player player) {
        try {
            return Optional.ofNullable(player.getInventory().getItemInHand()); // Old version support
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    private boolean makeAWish(Player player, String message) {
        return plugin.getWishManager().wish(player, message);
    }
}
