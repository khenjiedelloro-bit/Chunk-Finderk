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

    private static final String OPTIMIZER = "Redstone Optimizer";

    // Options inside Redstone Optimizer. These only change what you see.
    private static final String[][] OPTIMIZER_ITEMS = {
            {"Disable Redstone Dust", "Hides redstone dust in your view. Circuits still work."},
            {"Hide Piston", "Hides piston movement in your view. Pistons still move."},
            {"Hide TNT Explosion", "Hides the blast. Ignited TNT stays visible with a fuse timer."},
            {"Hide Dropped Items", "Hides dropped items in your view. The items still exist."}
    };

    private static final int SIDEBAR_W = 150;
    private static final int HEADER_H = 50;
    private static final int ROW_H = 36;
    private static final int ROW_GAP = 4;

    // Purple theme (ARGB)
    private static final int WINDOW_BG = 0xF2120E1A;
    private static final int SIDEBAR_BG = 0xFF181126;
    private static final int ROW_BG = 0xFF1F1836;
    private static final int ROW_HOVER = 0xFF2A2148;
    private static final int ROW_DIM = 0xFF161129;
    private static final int SELECTED_BG = 0xFF33235A;
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

    private record Row(Box box, String name, String description, Modules.Module module, boolean opensOptimizer) {}

    private final List<Box> categoryBoxes = new ArrayList<>();
    private final List<Action> actions = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private int selected = 0;
    private boolean optimizerOpen = false;

    private int winX, winY, winW, winH;
    private Box backBox;
    private Box searchBox;

    public FinderScreen() {
        super(Component.literal("Pollution Client"));
    }

    @Override
    protected void init() {
        winW = Math.min(this.width - 16, 540);
        winH = Math.min(this.height - 16, 360);
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;

        categoryBoxes.clear();
        int y = winY + 58;
        for (int i = 0; i < CATEGORIES.length; i++) {
            categoryBoxes.add(new Box(winX + 8, y, SIDEBAR_W - 16, 22));
            y += 26;
        }

        actions.clear();
        int ay = winY + winH - 60;
        actions.add(new Action(new Box(winX + 8, ay, SIDEBAR_W - 16, 22), "Save settings", Modules::save));
        actions.add(new Action(new Box(winX + 8, ay + 26, SIDEBAR_W - 16, 22), "Close", this::onClose));

        backBox = new Box(mainX(), winY + 10, 56, 18);
        searchBox = new Box(winX + winW - 156, winY + 10, 144, 20);

        buildRows();
    }

    private int mainX() {
        return winX + SIDEBAR_W + 12;
    }

    private int mainW() {
        return winW - SIDEBAR_W - 24;
    }

    private void buildRows() {
        rows.clear();
        int x = mainX();
        int w = mainW();
        int y = winY + HEADER_H;

        if (optimizerOpen) {
            for (String[] item : OPTIMIZER_ITEMS) {
                rows.add(new Row(new Box(x, y, w, ROW_H), item[0], item[1], null, false));
                y += ROW_H + ROW_GAP;
            }
            return;
        }

        String category = CATEGORIES[selected];
        for (Modules.Module m : Modules.ALL) {
            if (m.category().equals(category)) {
                rows.add(new Row(new Box(x, y, w, ROW_H), m.name(), describe(m.name()), m, false));
                y += ROW_H + ROW_GAP;
            }
        }
        if (category.equals("Render")) {
            rows.add(new Row(new Box(x, y, w, ROW_H), OPTIMIZER, "Right-click to open", null, true));
            y += ROW_H + ROW_GAP;
        }
        for (String name : COMING_SOON[selected]) {
            rows.add(new Row(new Box(x, y, w, ROW_H), name, "Coming soon", null, false));
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

    private static int totalCount(int index) {
        String c = CATEGORIES[index];
        int n = moduleCount(c) + COMING_SOON[index].length;
        return c.equals("Render") ? n + 1 : n;
    }

    private static String plural(int n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    // Draws a rectangle with rounded corners using fills only.
    private static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        int rr = Math.min(r, Math.min(w, h) / 2);
        g.fill(x, y + rr, x + w, y + h - rr, color);
        for (int dy = 0; dy < rr; dy++) {
            double d = rr - dy - 0.5;
            int inset = rr - (int) Math.round(Math.sqrt(rr * rr - d * d));
            g.fill(x + inset, y + dy, x + w - inset, y + dy + 1, color);
            g.fill(x + inset, y + h - 1 - dy, x + w - inset, y + h - dy, color);
        }
    }

    // Small pixel icons for the sidebar. Each is about 10 by 10 pixels.
    private static void drawIcon(GuiGraphicsExtractor g, int index, int x, int y, int color) {
        switch (index) {
            case 0 -> drawRunner(g, x, y, color);
            case 1 -> drawEye(g, x, y, color);
            case 2 -> drawGlobe(g, x, y, color);
            case 3 -> drawPerson(g, x, y, color);
            default -> drawBars(g, x, y, color);
        }
    }

    private static void drawRunner(GuiGraphicsExtractor g, int x, int y, int c) {
        g.fill(x + 5, y, x + 9, y + 3, c);
        g.fill(x + 4, y + 3, x + 8, y + 7, c);
        g.fill(x + 1, y + 7, x + 4, y + 9, c);
        g.fill(x + 5, y + 7, x + 8, y + 10, c);
    }

    private static void drawEye(GuiGraphicsExtractor g, int x, int y, int c) {
        rounded(g, x, y + 2, 10, 6, 3, c);
        rounded(g, x + 3, y + 3, 4, 4, 2, SIDEBAR_BG);
    }

    private static void drawGlobe(GuiGraphicsExtractor g, int x, int y, int c) {
        rounded(g, x, y, 10, 10, 5, c);
        g.fill(x + 5, y + 1, x + 6, y + 9, SIDEBAR_BG);
        g.fill(x + 1, y + 5, x + 9, y + 6, SIDEBAR_BG);
    }

    private static void drawPerson(GuiGraphicsExtractor g, int x, int y, int c) {
        rounded(g, x + 3, y, 4, 4, 2, c);
        rounded(g, x + 1, y + 5, 8, 5, 2, c);
    }

    private static void drawBars(GuiGraphicsExtractor g, int x, int y, int c) {
        g.fill(x, y + 1, x + 10, y + 3, c);
        g.fill(x, y + 4, x + 10, y + 6, c);
        g.fill(x, y + 7, x + 10, y + 9, c);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        // Window and sidebar
        rounded(g, winX, winY, winW, winH, 12, WINDOW_BG);
        rounded(g, winX + 4, winY + 4, SIDEBAR_W - 8, winH - 8, 10, SIDEBAR_BG);

        g.text(this.font, "Pollution Client", winX + 14, winY + 12, ACCENT_TEXT, false);
        g.text(this.font, "BETA RELEASE 1.0", winX + 14, winY + 24, TEXT_DIM, false);
        g.text(this.font, "MODULES", winX + 14, winY + 42, TEXT_DIM, false);

        for (int i = 0; i < CATEGORIES.length; i++) {
            Box b = categoryBoxes.get(i);
            boolean isSelected = i == selected;
            boolean hovered = b.contains(mouseX, mouseY);
            if (isSelected) {
                rounded(g, b.x(), b.y(), b.w(), b.h(), 6, SELECTED_BG);
            } else if (hovered) {
                rounded(g, b.x(), b.y(), b.w(), b.h(), 6, ROW_HOVER);
            }
            drawIcon(g, i, b.x() + 8, b.y() + 6, isSelected ? ACCENT : TEXT_DIM);
            g.text(this.font, CATEGORIES[i], b.x() + 24, b.y() + 7, isSelected ? ACCENT_TEXT : TEXT, false);
            g.text(this.font, String.valueOf(totalCount(i)), b.x() + b.w() - 14, b.y() + 7, TEXT_DIM, false);
        }

        g.text(this.font, "GENERAL", winX + 14, winY + winH - 74, TEXT_DIM, false);
        for (Action a : actions) {
            Box b = a.box();
            boolean hovered = b.contains(mouseX, mouseY);
            rounded(g, b.x(), b.y(), b.w(), b.h(), 6, hovered ? ROW_HOVER : ROW_BG);
            g.text(this.font, a.label(), b.x() + 8, b.y() + 7, TEXT, false);
        }

        // Main area
        int mx = mainX();
        if (optimizerOpen) {
            Box bb = backBox;
            rounded(g, bb.x(), bb.y(), bb.w(), bb.h(), 6, bb.contains(mouseX, mouseY) ? ROW_HOVER : ROW_BG);
            g.text(this.font, "< Back", bb.x() + 8, bb.y() + 5, TEXT, false);
            g.text(this.font, OPTIMIZER, mx + 66, winY + 14, ACCENT_TEXT, false);
            g.text(this.font, "Client-side only. Other players still see everything.", mx, winY + 34, TEXT_DIM, false);
        } else {
            String category = CATEGORIES[selected];
            g.text(this.font, category + " Modules", mx, winY + 12, ACCENT_TEXT, false);
            g.text(this.font, plural(moduleCount(category), "module") + ", "
                    + enabledCount(category) + " enabled", mx, winY + 26, TEXT_DIM, false);
        }

        Box sb = searchBox;
        rounded(g, sb.x(), sb.y(), sb.w(), sb.h(), 6, ROW_BG);
        g.text(this.font, "Search modules", sb.x() + 8, sb.y() + 6, TEXT_DIM, false);

        for (Row r : rows) {
            drawRow(g, r, mouseX, mouseY);
        }
    }

    private void drawRow(GuiGraphicsExtractor g, Row r, int mouseX, int mouseY) {
        Box b = r.box();
        boolean hovered = b.contains(mouseX, mouseY);
        boolean dim = r.module() == null && !r.opensOptimizer();
        rounded(g, b.x(), b.y(), b.w(), b.h(), 8, dim ? ROW_DIM : (hovered ? ROW_HOVER : ROW_BG));
        g.text(this.font, r.name(), b.x() + 10, b.y() + 7, dim ? TEXT_DIM : TEXT, false);
        g.text(this.font, r.description(), b.x() + 10, b.y() + 19, TEXT_DIM, false);

        if (r.opensOptimizer()) {
            g.text(this.font, ">", b.x() + b.w() - 16, b.y() + 13, TEXT_DIM, false);
        } else if (r.module() != null) {
            boolean on = r.module().isEnabled();
            g.text(this.font, ">", b.x() + b.w() - 56, b.y() + 13, TEXT_DIM, false);
            int trackX = b.x() + b.w() - 30;
            int trackY = b.y() + 13;
            rounded(g, trackX, trackY, 22, 10, 5, on ? ACCENT : TOGGLE_OFF);
            rounded(g, on ? trackX + 12 : trackX + 2, trackY + 1, 8, 8, 4, KNOB);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        for (int i = 0; i < categoryBoxes.size(); i++) {
            if (categoryBoxes.get(i).contains(mx, my)) {
                selected = i;
                optimizerOpen = false;
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
        if (optimizerOpen && backBox.contains(mx, my)) {
            optimizerOpen = false;
            buildRows();
            return true;
        }

        Row hit = null;
        for (Row r : rows) {
            if (r.box().contains(mx, my)) {
                hit = r;
                break;
            }
        }
        if (hit != null) {
            if (hit.opensOptimizer()) {
                if (event.button() == 1) { // right click opens the page
                    optimizerOpen = true;
                    buildRows();
                }
            } else if (hit.module() != null) {
                hit.module().toggle();
                Modules.save();
            }
            return true;
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
