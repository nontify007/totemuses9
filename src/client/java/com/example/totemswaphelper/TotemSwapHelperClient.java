package com.example.totemswaphelper;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class TotemSwapHelperClient implements ClientModInitializer {

	private static final int OFFHAND_SLOT = 45;

	private static boolean enabled = true;
	private static boolean alwaysActive = false;

	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath("totemswaphelper", "main")
		);

		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.totemswaphelper.toggle",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				category
		));

		registerCommand();

		ClientTickEvents.END_CLIENT_TICK.register(TotemSwapHelperClient::onClientTick);
	}

	private void registerCommand() {
		ClientCommands.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommands.literal("totemhelper")
						.then(ClientCommands.literal("always")
								.executes(ctx -> {
									alwaysActive = true;
									sendFeedback(Minecraft.getInstance(), "Mode: always active");
									return 1;
								}))
						.then(ClientCommands.literal("inventory")
								.executes(ctx -> {
									alwaysActive = false;
									sendFeedback(Minecraft.getInstance(), "Mode: only while inventory (E) is open");
									return 1;
								}))
						.then(ClientCommands.literal("on")
								.executes(ctx -> {
									enabled = true;
									sendFeedback(Minecraft.getInstance(), "Enabled");
									return 1;
								}))
						.then(ClientCommands.literal("off")
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
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
				foundIndex = i;
				break;
			}
		}
		if (foundIndex == -1) {
			return;
		}

		int menuSlot = foundIndex + 9;
		int containerId = player.containerMenu.containerId;

		if (client.gameMode == null) {
			return;
		}

		client.gameMode.handleInventoryMouseClick(containerId, menuSlot, 0, ClickType.PICKUP, player);
		client.gameMode.handleInventoryMouseClick(containerId, OFFHAND_SLOT, 0, ClickType.PICKUP, player);
		if (!player.containerMenu.getCarried().isEmpty()) {
			client.gameMode.handleInventoryMouseClick(containerId, menuSlot, 0, ClickType.PICKUP, player);
		}
	}

	private static void sendFeedback(Minecraft client, String message) {
		if (client.player != null) {
			client.player.sendSystemMessage(Component.literal("[Totem Swap Helper] " + message));
		}
	}
}
