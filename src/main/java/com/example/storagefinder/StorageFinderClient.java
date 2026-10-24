package com.example.storagefinder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class StorageFinderClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("storagefinder", "main"));

    private static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        Modules.load();
        menuKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.storagefinder.menu", GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        while (menuKey.consumeClick()) {
            if (mc.level != null && mc.gui.screen() == null) {
                mc.gui.setScreen(new FinderScreen());
            }
        }
        if (mc.level != null && mc.player != null) {
            for (Modules.Module m : Modules.ALL) {
                if (m.isEnabled()) m.tick(mc);
            }
        }
    }
}
