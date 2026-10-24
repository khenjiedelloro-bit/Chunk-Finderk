package com.example.storagefinder;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class FinderScreen extends Screen {
    private static final String[] CATEGORIES = {"Movement", "Render", "World", "Player", "Misc"};
    private static final String[][] COMING_SOON = {
            {},
            {"Farm Timer", "Viewmodel", "Capes", "Hats", "Totem Counter"},
            {"Chunk Border"},
            {"Coordinates", "Armor Display"},
            {}
    };
    private static final int COLUMN_WIDTH = 120;
    private static final int GAP = 6;

    public FinderScreen() {
        super(Component.literal("Polute Client"));
    }

    @Override
    protected void init() {
        int totalWidth = CATEGORIES.length * COLUMN_WIDTH + (CATEGORIES.length - 1) * GAP;
        int left = this.width / 2 - totalWidth / 2;
        int top = this.height / 4;

        addRenderableWidget(new StringWidget(left, top - 24, totalWidth, 12, this.title, this.font));

        for (int c = 0; c < CATEGORIES.length; c++) {
            String category = CATEGORIES[c];
            int x = left + c * (COLUMN_WIDTH + GAP);
            int y = top;

            addRenderableWidget(new StringWidget(x, y, COLUMN_WIDTH, 12,
                    Component.literal(category), this.font));
            y += 18;

            for (Modules.Module m : Modules.ALL) {
                if (m.category().equals(category)) {
                    addRenderableWidget(moduleButton(m, x, y));
                    y += 24;
                }
            }
            for (String name : COMING_SOON[c]) {
                addRenderableWidget(new StringWidget(x, y, COLUMN_WIDTH, 12,
                        Component.literal(name + " (soon)"), this.font));
                y += 14;
            }
            if (category.equals("Misc")) {
                addRenderableWidget(Button.builder(Component.literal("Save settings"), b -> Modules.save())
                        .bounds(x, y, COLUMN_WIDTH, 20).build());
                y += 24;
                addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                        .bounds(x, y, COLUMN_WIDTH, 20).build());
            }
        }
    }

    private Button moduleButton(Modules.Module m, int x, int y) {
        return Button.builder(moduleLabel(m), b -> {
            m.toggle();
            b.setMessage(moduleLabel(m));
        }).bounds(x, y, COLUMN_WIDTH, 20).build();
    }

    private static Component moduleLabel(Modules.Module m) {
        return Component.literal(m.name() + ": " + (m.isEnabled() ? "ON" : "OFF"));
    }

    @Override
    public void onClose() {
        Modules.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
