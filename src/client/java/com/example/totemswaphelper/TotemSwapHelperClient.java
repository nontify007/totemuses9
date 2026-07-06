package com.example.totemswaphelper;

/*
 * IMPORTANT NOTE ON MAPPINGS
 * ---------------------------------------------------------------------------
 * Minecraft 26.1+ ships unobfuscated and Fabric develops directly against
 * Mojang's official mappings (no more Yarn). That means class/field/method
 * names below use Mojang's real names (Minecraft, LocalPlayer, ItemStack,
 * AbstractContainerMenu, ClickType, etc.) instead of the old Yarn names
 * (MinecraftClient, ClientPlayerEntity, SlotActionType, ...).
 *
 * If a name below doesn't match exactly what your IDE resolves once you run
 * `./gradlew genSources`, do a quick rename - the *logic* (which slots we
 * click, in what order) will still be correct, only the exact identifier
 * Mojang chose might differ slightly between 26.1.x patch releases.
 * ---------------------------------------------------------------------------
 */

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/**
 * Totem Swap Helper
 * -----------------
 * A client-side QoL mod for SINGLEPLAYER / your own private worlds.
 *
 * If your offhand does not hold a Totem of Undying, this mod finds a totem
 * in your inventory and swaps it into your offhand automatically.
 *
 * Two modes:
 *  - "inventory" mode (default): only runs while your survival inventory
 *    screen (the one you open with E) is open.
 *  - "always" mode: runs every tick regardless of what screen is open.
 *
 * Toggle the whole mod on/off with Right Shift (configurable in
 * Controls > Key Binds). Switch modes with the /totemhelper command.
 *
 * NOTE: Do not use this on multiplayer servers you don't own/control -
 * automatically managing your offhand item is against the rules of most
 * public PvP servers and their anti-cheat will very likely flag it.
 */
public class TotemSwapHelperClient implements ClientModInitializer {

	/** Slot index of the offhand slot inside the player's own inventory menu. */
	private static final int OFFHAND_SLOT = 45;

	private static boolean enabled = true;
	private static boolean alwaysActive = false;

	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.totemswaphelper.toggle",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				"category.totemswaphelper"
		));

		registerCommand();

		ClientTickEvents.END_CLIENT_TICK.register(TotemSwapHelperClient::onClientTick);
	}

	private void registerCommand() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("totemhelper")
						.then(ClientCommandManager.literal("always")
								.executes(ctx -> {
									alwaysActive = true;
									sendFeedback(Minecraft.getInstance(), "Mode: always active");
									return 1;
								}))
						.then(ClientCommandManager.literal("inventory")
								.executes(ctx -> {
									alwaysActive = false;
									sendFeedback(Minecraft.getInstance(), "Mode: only while inventory (E) is open");
									return 1;
								}))
						.then(ClientCommandManager.literal("on")
								.executes(ctx -> {
									enabled = true;
									sendFeedback(Minecraft.getInstance(), "Enabled");
									return 1;
								}))
						.then(ClientCommandManager.literal("off")
								.executes(ctx -> {
									enabled = false;
									sendFeedback(Minecraft.getInstance(), "Disabled");
									return 1;
								}))
				)
		);
	}

	private static void onClientTick(Minecraft client) {
		while (toggleKey.consumeClick()) {
			enabled = !enabled;
			sendFeedback(client, enabled ? "Enabled" : "Disabled");
		}

		if (!enabled) {
			return;
		}

		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}

		boolean inventoryScreenOpen = client.screen instanceof InventoryScreen;
		if (!alwaysActive && !inventoryScreenOpen) {
			return;
		}

		// Only act while the player's own default inventory menu is open
		// (not a chest, crafting table, furnace, etc.) so slot indices are
		// guaranteed to line up with OFFHAND_SLOT.
		if (player.containerMenu != player.inventoryMenu) {
			return;
		}

		tryFillOffhandWithTotem(client, player);
	}

	private static void tryFillOffhandWithTotem(Minecraft client, LocalPlayer player) {
		ItemStack offhand = player.getOffhandItem();
		if (offhand.is(Items.TOTEM_OF_UNDYING)) {
			return;
		}

		Inventory inventory = player.getInventory();
		int foundIndex = -1;
		for (int i = 0; i < inventory.items.size(); i++) {
			if (inventory.items.get(i).is(Items.TOTEM_OF_UNDYING)) {
				foundIndex = i;
				break;
			}
		}
		if (foundIndex == -1) {
			return;
		}

		// inventory.items covers hotbar + main inventory (36 slots), and in the
		// player's own menu those map 1:1 to menu slots 9..44.
		int menuSlot = foundIndex + 9;
		int containerId = player.containerMenu.containerId;

		if (client.gameMode == null) {
			return;
		}

		// 1. Pick the totem up onto the cursor.
		client.gameMode.handleInventoryMouseClick(containerId, menuSlot, 0, ClickType.PICKUP, player);
		// 2. Place it into the offhand slot (this also picks up whatever was
		//    previously in the offhand slot onto the cursor, if anything).
		client.gameMode.handleInventoryMouseClick(containerId, OFFHAND_SLOT, 0, ClickType.PICKUP, player);
		// 3. If something was previously in the offhand, put it back where
		//    the totem came from (that slot is now empty).
		if (!player.containerMenu.getCarried().isEmpty()) {
			client.gameMode.handleInventoryMouseClick(containerId, menuSlot, 0, ClickType.PICKUP, player);
		}
	}

	private static void sendFeedback(Minecraft client, String message) {
		if (client.player != null) {
			client.player.displayClientMessage(Component.literal("[Totem Swap Helper] " + message), true);
		}
	}
}
