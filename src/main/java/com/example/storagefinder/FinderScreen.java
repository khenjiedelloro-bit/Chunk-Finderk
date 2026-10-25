package com.example.storagefinder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class FinderScreen extends Screen {

    private static final String BRAND = "LuffyEssentials";
    private static final String[] CATEGORIES = {"Movement", "Render", "World", "Player", "Misc"};

    private static final int SIDEBAR_W = 150;
    private static final int HEADER_H = 50;
    private static final int ROW_H = 36;
    private static final int ROW_GAP = 4;
    private static final int EDITOR_ROWS_Y = 66;   // where colour rows start, relative to the window top
    private static final int CHANNEL_STEP = 16;

    // Purple theme (ARGB), brighter than before
    private static final int WINDOW_BG = 0xF21A0633;
    private static final int SIDEBAR_BG = 0xFF230A44;
    private static final int ROW_BG = 0xFF2C1357;
    private static final int ROW_HOVER = 0xFF3D1C75;
    private static final int BUTTON_BG = 0xFF4A1F8F;
    private static final int SELECTED_BG = 0xFF5A24B0;
    private static final int ACCENT = 0xFFB52EFF;
    private static final int ACCENT_TEXT = 0xFFE3B3FF;
    private static final int TEXT = 0xFFF5EEFF;
    private static final int TEXT_DIM = 0xFFA88AD6;
    private static final int TOGGLE_OFF = 0xFF4A2F7A;
    private static final int KNOB = 0xFFFFFFFF;

    private record Box(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record Action(Box box, String label, Runnable action) {}

    // A row is either a module (module != null) or one colour channel while editing (channel != '\0').
    private record Row(Box box, String name, String description, Modules.Module module, char channel) {}

    private final List<Box> categoryBoxes = new ArrayList<>();
    private final List<Action> actions = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private int selected = 0;
    private Modules.ColorModule editing = null;

    private int winX, winY, winW, winH;
    private Box backBox;
    private Box searchBox;

    public FinderScreen() {
        super(Component.literal(BRAND));
    }

    @Override
    protected void init() {
        winW = Math.min(this.width - 16, 560);
        winH = Math.min(this.height - 16, 380);
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

        if (editing != null) {
            int y = winY + EDITOR_ROWS_Y;
            rows.add(new Row(new Box(x, y, w, ROW_H), "Red", "", null, 'r'));
            y += ROW_H + ROW_GAP;
            rows.add(new Row(new Box(x, y, w, ROW_H), "Green", "", null, 'g'));
            y += ROW_H + ROW_GAP;
            rows.add(new Row(new Box(x, y, w, ROW_H), "Blue", "", null, 'b'));
            return;
        }

        int y = winY + HEADER_H;
        String category = CATEGORIES[selected];
        for (Modules.Module m : Modules.ALL) {
            if (m.category().equals(category)) {
                rows.add(new Row(new Box(x, y, w, ROW_H), m.name(), describe(m.name()), m, '\0'));
                y += ROW_H + ROW_GAP;
            }
        }
    }

    private static String describe(String name) {
        return switch (name) {
            case "Auto Sprint" -> "Keeps you sprinting while you move forward.";
            case "Saturation Color" -> "Tints your screen with a faint colour. Effect comes next.";
            case "Glowing Hand" -> "Glows the item in your hand. Effect comes next.";
            default -> "";
        };
    }

    // Counts modules in a category, optionally only the enabled ones.
    private static int count(String category, boolean enabledOnly) {
        int n = 0;
        for (Modules.Module m : Modules.ALL) {
            if (m.category().equals(category) && (!enabledOnly || m.isEnabled())) n++;
        }
        return n;
    }

    private static String plural(int n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    private static String channelName(char c) {
        return switch (c) {
            case 'r' -> "Red";
            case 'g' -> "Green";
            default -> "Blue";
        };
    }

    private int channelValue(char c) {
        return switch (c) {
            case 'r' -> editing.red();
            case 'g' -> editing.green();
            default -> editing.blue();
        };
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

    // Draws the brand name with each letter bobbing up and down.
    private void drawTitle(GuiGraphicsExtractor g) {
        long t = System.currentTimeMillis();
        int x = winX + 14;
        for (int i = 0; i < BRAND.length(); i++) {
            String letter = String.valueOf(BRAND.charAt(i));
            int wave = (int) Math.round(Math.sin(t / 200.0 + i * 0.6) * 2.0);
            g.text(this.font, letter, x, winY + 12 + wave, ACCENT_TEXT, false);
            x += this.font.width(letter);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        // Window and sidebar
        rounded(g, winX, winY, winW, winH, 12, WINDOW_BG);
        rounded(g, winX + 4, winY + 4, SIDEBAR_W - 8, winH - 8, 10, SIDEBAR_BG);

        drawTitle(g);
        g.text(this.font, "BETA RELEASE 1.0", winX + 14, winY + 26, TEXT_DIM, false);
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
            g.text(this.font, String.valueOf(count(CATEGORIES[i], false)), b.x() + b.w() - 14, b.y() + 7,
                    TEXT_DIM, false);
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
        if (editing != null) {
            Box bb = backBox;
            rounded(g, bb.x(), bb.y(), bb.w(), bb.h(), 6, bb.contains(mouseX, mouseY) ? ROW_HOVER : ROW_BG);
            g.text(this.font, "< Back", bb.x() + 8, bb.y() + 5, TEXT, false);
            g.text(this.font, editing.name(), mx + 66, winY + 14, ACCENT_TEXT, false);
            g.text(this.font, "Click + or - to change each colour. Saves automatically.", mx, winY + 30,
                    TEXT_DIM, false);
            int py = winY + 44;
            rounded(g, mx, py, mainW(), 14, 6, editing.argb());
            g.text(this.font, String.format("#%06X", editing.argb() & 0xFFFFFF), mx + 6, py + 3, TEXT, true);
        } else {
            String category = CATEGORIES[selected];
            g.text(this.font, category + " Modules", mx, winY + 12, ACCENT_TEXT, false);
            g.text(this.font, plural(count(category, false), "module") + ", "
                    + count(category, true) + " enabled", mx, winY + 26, TEXT_DIM, false);
        }

        Box sb = searchBox;
        rounded(g, sb.x(), sb.y(), sb.w(), sb.h(), 6, ROW_BG);
        g.text(this.font, "Search modules", sb.x() + 8, sb.y() + 6, TEXT_DIM, false);

        if (editing == null && rows.isEmpty()) {
            g.text(this.font, "No modules here yet.", mx, winY + HEADER_H + 4, TEXT_DIM, false);
        }
        for (Row r : rows) {
            drawRow(g, r, mouseX, mouseY);
        }
    }

    private void drawRow(GuiGraphicsExtractor g, Row r, int mouseX, int mouseY) {
        Box b = r.box();
        boolean hovered = b.contains(mouseX, mouseY);
        rounded(g, b.x(), b.y(), b.w(), b.h(), 8, hovered ? ROW_HOVER : ROW_BG);

        if (r.channel() != '\0') {
            drawChannelRow(g, r, mouseX, mouseY);
            return;
        }

        g.text(this.font, r.name(), b.x() + 10, b.y() + 7, TEXT, false);
        g.text(this.font, r.description(), b.x() + 10, b.y() + 19, TEXT_DIM, false);

        Modules.Module m = r.module();
        if (m instanceof Modules.ColorModule cm) {
            rounded(g, b.x() + b.w() - 86, b.y() + 11, 14, 14, 4, cm.argb());
            g.text(this.font, ">", b.x() + b.w() - 52, b.y() + 13, TEXT_DIM, false);
        }
        drawToggle(g, b, m.isEnabled());
    }

    private void drawToggle(GuiGraphicsExtractor g, Box b, boolean on) {
        int trackX = b.x() + b.w() - 30;
        int trackY = b.y() + 13;
        rounded(g, trackX, trackY, 22, 10, 5, on ? ACCENT : TOGGLE_OFF);
        rounded(g, on ? trackX + 12 : trackX + 2, trackY + 1, 8, 8, 4, KNOB);
    }

    private void drawChannelRow(GuiGraphicsExtractor g, Row r, int mouseX, int mouseY) {
        Box b = r.box();
        g.text(this.font, channelName(r.channel()) + ": " + channelValue(r.channel()),
                b.x() + 10, b.y() + 13, TEXT, false);

        Box minus = minusBox(b);
        Box plus = plusBox(b);
        rounded(g, minus.x(), minus.y(), minus.w(), minus.h(), 6,
                minus.contains(mouseX, mouseY) ? ROW_HOVER : BUTTON_BG);
        rounded(g, plus.x(), plus.y(), plus.w(), plus.h(), 6,
                plus.contains(mouseX, mouseY) ? ROW_HOVER : BUTTON_BG);
        g.text(this.font, "-", minus.x() + 12, minus.y() + 6, TEXT, false);
        g.text(this.font, "+", plus.x() + 11, plus.y() + 6, TEXT, false);
    }

    // Clickable areas inside a row.
    private static Box editBox(Box b) {
        return new Box(b.x() + b.w() - 100, b.y(), 68, b.h());
    }

    private static Box minusBox(Box b) {
        return new Box(b.x() + b.w() - 70, b.y() + 8, 30, 20);
    }

    private static Box plusBox(Box b) {
        return new Box(b.x() + b.w() - 34, b.y() + 8, 30, 20);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        for (int i = 0; i < categoryBoxes.size(); i++) {
            if (categoryBoxes.get(i).contains(mx, my)) {
                selected = i;
                editing = null;
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
        if (editing != null && backBox.contains(mx, my)) {
            editing = null;
            buildRows();
            return true;
        }

        for (Row r : rows) {
            if (!r.box().contains(mx, my)) continue;
            if (r.channel() != '\0') {
                handleChannelClick(r, mx, my);
            } else if (r.module() != null) {
                handleModuleClick(r, mx, my);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void handleModuleClick(Row r, double mx, double my) {
        Modules.Module m = r.module();
        if (m instanceof Modules.ColorModule cm && editBox(r.box()).contains(mx, my)) {
            editing = cm;
            buildRows();
            return;
        }
        m.toggle();
        Modules.save();
    }

    private void handleChannelClick(Row r, double mx, double my) {
        if (minusBox(r.box()).contains(mx, my)) {
            editing.adjust(r.channel(), -CHANNEL_STEP);
        } else if (plusBox(r.box()).contains(mx, my)) {
            editing.adjust(r.channel(), CHANNEL_STEP);
        } else {
            return;
        }
        Modules.save();
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
