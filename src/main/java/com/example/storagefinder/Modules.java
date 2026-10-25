package com.example.storagefinder;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class Modules {

    public abstract static class Module {
        private final String name;
        private final String category;
        private boolean enabled;

        protected Module(String name, String category) {
            this.name = name;
            this.category = category;
        }

        public String name() { return name; }
        public String category() { return category; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean value) { enabled = value; }
        public void toggle() { enabled = !enabled; }

        /** Called every client tick while the module is enabled. */
        public void tick(Minecraft mc) {}

        /** Saves extra values such as colours. Override to store more. */
        public void saveExtras(Properties props, String prefix) {}

        /** Loads the values written by saveExtras. */
        public void loadExtras(Properties props, String prefix) {}
    }

    public static final class AutoSprint extends Module {
        public AutoSprint() {
            super("Auto Sprint", "Movement");
        }

        @Override
        public void tick(Minecraft mc) {
            if (mc.player != null && mc.options.keyUp.isDown()) {
                mc.player.setSprinting(true);
            }
        }
    }

    /** A module with a colour the player picks. The effect is added in a later update. */
    public static class ColorModule extends Module {
        private int r;
        private int g;
        private int b;

        public ColorModule(String name, String category, int r, int g, int b) {
            super(name, category);
            this.r = clamp(r);
            this.g = clamp(g);
            this.b = clamp(b);
        }

        public int red() { return r; }
        public int green() { return g; }
        public int blue() { return b; }

        /** The colour as ARGB with full opacity. */
        public int argb() { return 0xFF000000 | (r << 16) | (g << 8) | b; }

        /** Changes one channel ('r', 'g' or 'b') by delta, staying between 0 and 255. */
        public void adjust(char channel, int delta) {
            switch (channel) {
                case 'r' -> r = clamp(r + delta);
                case 'g' -> g = clamp(g + delta);
                default -> b = clamp(b + delta);
            }
        }

        @Override
        public void saveExtras(Properties props, String prefix) {
            props.setProperty(prefix + ".r", Integer.toString(r));
            props.setProperty(prefix + ".g", Integer.toString(g));
            props.setProperty(prefix + ".b", Integer.toString(b));
        }

        @Override
        public void loadExtras(Properties props, String prefix) {
            r = readChannel(props, prefix + ".r", r);
            g = readChannel(props, prefix + ".g", g);
            b = readChannel(props, prefix + ".b", b);
        }

        private static int clamp(int v) {
            return Math.max(0, Math.min(255, v));
        }

        private static int readChannel(Properties props, String key, int fallback) {
            try {
                return clamp(Integer.parseInt(props.getProperty(key, Integer.toString(fallback)).trim()));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
    }

    public static final List<Module> ALL = new ArrayList<>();

    public static final ColorModule SATURATION_COLOR = new ColorModule("Saturation Color", "Render", 160, 40, 255);
    public static final ColorModule GLOWING_HAND = new ColorModule("Glowing Hand", "Render", 255, 60, 60);

    static {
        ALL.add(new AutoSprint());
        ALL.add(SATURATION_COLOR);
        ALL.add(GLOWING_HAND);
    }

    private Modules() {}

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("storagefinder.properties");
    }

    private static String key(Module m) {
        return m.name().toLowerCase().replace(' ', '_');
    }

    public static void load() {
        Path path = configFile();
        if (!Files.exists(path)) return;
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            props.load(reader);
        } catch (IOException e) {
            return;
        }
        for (Module m : ALL) {
            String prefix = key(m);
            String value = props.getProperty(prefix);
            if (value != null) m.setEnabled(Boolean.parseBoolean(value));
            m.loadExtras(props, prefix);
        }
    }

    public static void save() {
        Properties props = new Properties();
        for (Module m : ALL) {
            String prefix = key(m);
            props.setProperty(prefix, Boolean.toString(m.isEnabled()));
            m.saveExtras(props, prefix);
        }
        try (Writer writer = Files.newBufferedWriter(configFile())) {
            props.store(writer, "LuffyEssentials settings");
        } catch (IOException e) {
            // Settings just won't be saved this time.
        }
    }
                }
