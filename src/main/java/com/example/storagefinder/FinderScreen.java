package com.example.storagefinder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class FinderScreen extends Screen {

    // Sidebar sections. Combat, Mace and Spear are left out on purpose.
    private static final String[] CATEGORIES = {"Movement", "Render", "World", "Player", "Misc"};

    // Planned modules that aren't built yet, in the same order as CATEGORIES.
    private static final String[][] COMING_SOON = {
            {},
            {"Farm Timer", "Viewmodel", "Capes", "Hats", "Totem Counter"},
            {"Chunk Border"},
            {"Coordinates", "Armor Display"},
            {}
    };

    private static final int SIDEBAR_W = 130;
    private static final int HEADER_H = 46;
    private static final int ROW_H = 34;
    private static final int ROW_GAP = 4;
    private static final int PAD = 8;

    // Purple theme (ARGB)
    private static final int WINDOW_BG = 0xF2120E1A;
    private static final int SIDEBAR_BG = 0xFF171224;
    private static final int DIVIDER = 0xFF2A2140;
    private static final int ROW_BG = 0xFF1D1733;
    private static final int ROW_HOVER = 0xFF271F45;
    private static final int ROW_DIM = 0xFF151129;
    private static final int SELECTED_BG = 0xFF2E2150;
    private static final int ACCENT = 0xFFA25CFF;
    private static final int ACCENT_TEXT = 0xFFC9A6FF;
    private static final int TEXT = 0xFFEDE7FF;
    private static final int TEXT_DIM = 0xFF8E84A8;
    private static final int TOGGLE_OFF = 0xFF3A3150;
    private static final int KNOB = 0xFFFFFFFF;

    private record Box(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record Action(Box box, String label, Runnable action) {}

    private record Row(Box box, String name, String description, Modules.Module module) {}

    private final List<Box> categoryBoxes = new ArrayList<>();
    private final List<Action> actions = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private int selected = 0;

    private int winX, winY, winW, winH;

    public FinderScreen() {
        super(Component.literal("Pollution Client"));
    }

    @Override
    protected void init() {
        winW = Math.min(this.width - 16, 520);
        winH = Math.min(this.height - 16, 300);
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;

        categoryBoxes.clear();
        int y = winY + 54;
        for (int i = 0; i < CATEGORIES.length; i++) {
            categoryBoxes.add(new Box(winX + 6, y, SIDEBAR_W - 12, 20));
            y += 24;
        }

        actions.clear();
        int ay = winY + winH - 44;
        actions.add(new Action(new Box(winX + 6, ay, SIDEBAR_W - 12, 18), "Save settings", Modules::save));
        actions.add(new Action(new Box(winX + 6, ay + 22, SIDEBAR_W - 12, 18), "Close", this::onClose));

        buildRows();
    }

    private void buildRows() {
        rows.clear();
        String category = CATEGORIES[selected];
        int x = winX + SIDEBAR_W + PAD;
        int w = winW - SIDEBAR_W - PAD * 2;
        int y = winY + HEADER_H;
        for (Modules.Module m : Modules.ALL) {
            if (m.category().equals(category)) {
                rows.add(new Row(new Box(x, y, w, ROW_H), m.name(), describe(m.name()), m));
                y += ROW_H + ROW_GAP;
            }
        }
        for (String name : COMING_SOON[selected]) {
            rows.add(new Row(new Box(x, y, w, ROW_H), name, "Coming soon", null));
            y += ROW_H + ROW_GAP;
        }
    }

    private static String describe(String name) {
        return switch (name) {
            case "Auto Sprint" -> "Keeps you sprinting while you move forward.";
            case "Trail" -> "Particle trail behind you. Only you see it.";
            default -> "";
        };
    }

    private static int moduleCount(String category) {
        int n = 0;
        for (Modules.Module m : Modules.ALL) {
            if (m.category().equals(category)) n++;
        }
        return n;
    }

    private static int enabledCount(String category) {
        int n = 0;
        for (Modules.Module m : Modules.ALL) {
            if (m.category().equals(category) && m.isEnabled()) n++;
        }
        return n;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        // Window and sidebar
        g.fill(winX, winY, winX + winW, winY + winH, WINDOW_BG);
        g.fill(winX, winY, winX + SIDEBAR_W, winY + winH, SIDEBAR_BG);
        g.fill(winX + SIDEBAR_W - 1, winY, winX + SIDEBAR_W, winY + winH, DIVIDER);

        g.text(this.font, "Pollution Client", winX + 8, winY + 10, ACCENT_TEXT, false);
        g.text(this.font, "BETA RELEASE 1.0", winX + 8, winY + 22, TEXT_DIM, false);
        g.text(this.font, "MODULES", winX + 8, winY + 40, TEXT_DIM, false);

        for (int i = 0; i < CATEGORIES.length; i++) {
            Box b = categoryBoxes.get(i);
            boolean isSelected = i == selected;
            boolean hovered = b.contains(mouseX, mouseY);
            if (isSelected) {
                g.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), SELECTED_BG);
            } else if (hovered) {
                g.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), ROW_HOVER);
            }
            g.text(this.font, CATEGORIES[i], b.x() + 6, b.y() + 6, isSelected ? ACCENT_TEXT : TEXT, false);
            int total = moduleCount(CATEGORIES[i]) + COMING_SOON[i].length;
            g.text(this.font, String.valueOf(total), b.x() + b.w() - 12, b.y() + 6, TEXT_DIM, false);
        }

        g.text(this.font, "GENERAL", winX + 8, winY + winH - 58, TEXT_DIM, false);
        for (Action a : actions) {
            drawAction(g, a, mouseX, mouseY);
        }

        // Main header and search box
        int mainX = winX + SIDEBAR_W + PAD;
        String category = CATEGORIES[selected];
        g.text(this.font, category + " Modules", mainX, winY + 10, ACCENT_TEXT, false);
        g.text(this.font, moduleCount(category) + " modules, " + enabledCount(category) + " enabled",
                mainX, winY + 24, TEXT_DIM, false);

        int sx = winX + winW - 150;
        g.fill(sx, winY + 8, sx + 140, winY + 26, ROW_BG);
        g.text(this.font, "Search modules", sx + 6, winY + 13, TEXT_DIM, false);

        if (rows.isEmpty()) {
            g.text(this.font, "Press Esc to save and close.", mainX, winY + HEADER_H, TEXT_DIM, false);
        }
        for (Row r : rows) {
            drawRow(g, r, mouseX, mouseY);
        }
    }

    private void drawAction(GuiGraphicsExtractor g, Action a, int mouseX, int mouseY) {
        Box b = a.box();
        boolean hovered = b.contains(mouseX, mouseY);
        g.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), hovered ? ROW_HOVER : ROW_BG);
        g.text(this.font, a.label(), b.x() + 6, b.y() + 5, TEXT, false);
    }

    private void drawRow(GuiGraphicsExtractor g, Row r, int mouseX, int mouseY) {
        Box b = r.box();
        boolean soon = r.module() == null;
        boolean hovered = b.contains(mouseX, mouseY);
        int bg = soon ? ROW_DIM : (hovered ? ROW_HOVER : ROW_BG);
        g.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), bg);
        g.text(this.font, r.name(), b.x() + 8, b.y() + 6, soon ? TEXT_DIM : TEXT, false);
        g.text(this.font, r.description(), b.x() + 8, b.y() + 18, TEXT_DIM, false);

        if (!soon) {
            boolean on = r.module().isEnabled();
            int trackX = b.x() + b.w() - 34;
            int trackY = b.y() + 12;
            g.fill(trackX, trackY, trackX + 24, trackY + 10, on ? ACCENT : TOGGLE_OFF);
            int knobX = on ? trackX + 14 : trackX + 2;
            g.fill(knobX, trackY + 1, knobX + 8, trackY + 9, KNOB);
            g.text(this.font, ">", b.x() + b.w() - 50, b.y() + 12, TEXT_DIM, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        for (int i = 0; i < categoryBoxes.size(); i++) {
            if (categoryBoxes.get(i).contains(mx, my)) {
                selected = i;
                buildRows();
                return true;
            }
        }
        for (Action a : actions) {
            if (a.box().contains(mx, my)) {
                a.action().run();
                return true;
            }
        }
        for (Row r : rows) {
            if (r.module() != null && r.box().contains(mx, my)) {
                r.module().toggle();
                Modules.save();
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
