package com.example.storagefinder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class FinderScreen extends Screen {
    private static final String[] CATEGORIES = {"Movement", "Render", "World", "Player", "Misc"};
    private static final String[][] COMING_SOON = {
            {},
            {"Farm Timer", "Viewmodel", "Capes", "Hats", "Totem Counter"},
            {"Chunk Border"},
            {"Coordinates", "Armor Display"},
            {}
    };

    private static final int COLUMN_WIDTH = 130;
    private static final int GAP = 6;
    private static final int HEADER_HEIGHT = 16;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_GAP = 2;

    // Colours (ARGB). Change these to restyle the whole menu.
    private static final int HEADER_COLOR = 0xFF6B6B6B;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DIM_TEXT_COLOR = 0xFF8A8A8A;
    private static final int GEAR_COLOR = 0xFFBDBDBD;
    private static final int ON_COLOR = 0xFF6E6E6E;
    private static final int ON_HOVER_COLOR = 0xFF7F7F7F;
    private static final int OFF_COLOR = 0xFF2E2E2E;
    private static final int OFF_HOVER_COLOR = 0xFF3F3F3F;
    private static final int DIM_COLOR = 0xFF222222;

    private record Header(int x, int y, int w, int h, String text) {}

    private record Row(int x, int y, int w, int h, String label, Modules.Module module, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Header> headers = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();

    public FinderScreen() {
        super(Component.literal("Storage Finder"));
    }

    @Override
    protected void init() {
        headers.clear();
        rows.clear();

        int totalWidth = CATEGORIES.length * COLUMN_WIDTH + (CATEGORIES.length - 1) * GAP;
        int left = (this.width - totalWidth) / 2;
        int top = 20;

        for (int c = 0; c < CATEGORIES.length; c++) {
            String category = CATEGORIES[c];
            int x = left + c * (COLUMN_WIDTH + GAP);
            int y = top + HEADER_HEIGHT + ROW_GAP;

            headers.add(new Header(x, top, COLUMN_WIDTH, HEADER_HEIGHT, category));

            for (Modules.Module m : Modules.ALL) {
                if (m.category().equals(category)) {
                    rows.add(new Row(x, y, COLUMN_WIDTH, ROW_HEIGHT, m.name(), m, null));
                    y += ROW_HEIGHT + ROW_GAP;
                }
            }
            for (String name : COMING_SOON[c]) {
                rows.add(new Row(x, y, COLUMN_WIDTH, ROW_HEIGHT, name + " (soon)", null, null));
                y += ROW_HEIGHT + ROW_GAP;
            }
            if (category.equals("Misc")) {
                rows.add(new Row(x, y, COLUMN_WIDTH, ROW_HEIGHT, "Save settings", null, Modules::save));
                y += ROW_HEIGHT + ROW_GAP;
                rows.add(new Row(x, y, COLUMN_WIDTH, ROW_HEIGHT, "Close", null, this::onClose));
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(this.font, this.title.getString(), headers.get(0).x(), headers.get(0).y() - 12,
                TEXT_COLOR, false);

        for (Header h : headers) {
            graphics.fill(h.x(), h.y(), h.x() + h.w(), h.y() + h.h(), HEADER_COLOR);
            graphics.text(this.font, h.text(), h.x() + 4, h.y() + (h.h() - 9) / 2, TEXT_COLOR, false);
            graphics.text(this.font, "v", h.x() + h.w() - 10, h.y() + (h.h() - 9) / 2, TEXT_COLOR, false);
        }

        for (Row r : rows) {
            boolean hovered = r.contains(mouseX, mouseY);
            int bg;
            int textColor = TEXT_COLOR;
            if (r.module() != null) {
                boolean on = r.module().isEnabled();
                bg = on ? (hovered ? ON_HOVER_COLOR : ON_COLOR) : (hovered ? OFF_HOVER_COLOR : OFF_COLOR);
            } else if (r.action() != null) {
                bg = hovered ? OFF_HOVER_COLOR : OFF_COLOR;
            } else {
                bg = DIM_COLOR;
                textColor = DIM_TEXT_COLOR;
            }
            graphics.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), bg);
            graphics.text(this.font, r.label(), r.x() + 4, r.y() + (r.h() - 9) / 2, textColor, false);
            if (r.module() != null) {
                // Gear marker. Settings panels come in a later version.
                graphics.fill(r.x() + r.w() - 12, r.y() + 5, r.x() + r.w() - 5, r.y() + r.h() - 5, GEAR_COLOR);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (Row r : rows) {
            if (r.contains(event.x(), event.y())) {
                if (r.module() != null) {
                    r.module().toggle();
                } else if (r.action() != null) {
                    r.action().run();
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
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
