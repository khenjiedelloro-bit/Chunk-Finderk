package com.example.storagefinder;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class FinderScreen extends Screen {
    private Button finderButton;
    private Button hopperButton;
    private StringWidget summary;
    private final StringWidget[] rows = new StringWidget[6];
    private int refresh = 0;

    public FinderScreen() {
        super(Component.literal("Storage Finder"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 4;

        addRenderableWidget(new StringWidget(cx - 100, y - 24, 200, 12, this.title, this.font));

        finderButton = addRenderableWidget(Button.builder(finderLabel(), b -> {
            StorageScanner.enabled = !StorageScanner.enabled;
            b.setMessage(finderLabel());
        }).bounds(cx - 100, y, 200, 20).build());

        hopperButton = addRenderableWidget(Button.builder(hopperLabel(), b -> {
            StorageScanner.includeHoppers = !StorageScanner.includeHoppers;
            b.setMessage(hopperLabel());
        }).bounds(cx - 100, y + 24, 200, 20).build());

        summary = addRenderableWidget(new StringWidget(cx - 100, y + 52, 200, 12, Component.empty(), this.font));
        for (int i = 0; i < rows.length; i++) {
            rows[i] = addRenderableWidget(new StringWidget(cx - 100, y + 68 + i * 12, 200, 12, Component.empty(), this.font));
        }

        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(cx - 50, y + 68 + rows.length * 12 + 8, 100, 20).build());
        updateText();
    }

    @Override
    public void tick() {
        if (++refresh % 10 == 0) updateText();
    }

    private void updateText() {
        List<StorageScanner.Hit> hits = StorageScanner.hits;
        if (!StorageScanner.enabled) {
            summary.setMessage(Component.literal("Finder is off."));
        } else {
            summary.setMessage(Component.literal("Chunks with storage: " + hits.size()));
        }
        for (int i = 0; i < rows.length; i++) {
            if (StorageScanner.enabled && i < hits.size()) {
                var h = hits.get(i);
                rows[i].setMessage(Component.literal(
                        "Chunk " + h.pos().x() + ", " + h.pos().z()
                                + "  (block " + h.pos().getMinBlockX() + ", " + h.pos().getMinBlockZ()
                                + ")  x" + h.count()));
            } else {
                rows[i].setMessage(Component.empty());
            }
        }
    }

    private Component finderLabel() {
        return Component.literal("Chunk Finder: " + (StorageScanner.enabled ? "ON" : "OFF"));
    }

    private Component hopperLabel() {
        return Component.literal("Include hoppers/dispensers: " + (StorageScanner.includeHoppers ? "ON" : "OFF"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
