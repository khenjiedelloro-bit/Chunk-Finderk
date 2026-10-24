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

    public static final List<Module> ALL = new ArrayList<>();

    static {
        ALL.add(new AutoSprint());
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
            String value = props.getProperty(key(m));
            if (value != null) m.setEnabled(Boolean.parseBoolean(value));
        }
    }

    public static void save() {
        Properties props = new Properties();
        for (Module m : ALL) {
            props.setProperty(key(m), Boolean.toString(m.isEnabled()));
        }
        try (Writer writer = Files.newBufferedWriter(configFile())) {
            props.store(writer, "Storage Finder settings");
        } catch (IOException e) {
            // Settings just won't be saved this time.
        }
    }
}
