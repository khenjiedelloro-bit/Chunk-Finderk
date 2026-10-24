package com.example.storagefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class StorageScanner {
    public static boolean enabled = false;
    public static boolean includeHoppers = false;

    public record Hit(ChunkPos pos, int count) {}

    public static volatile List<Hit> hits = List.of();

    private static int ticks = 0;

    private StorageScanner() {}

    public static void tick(Minecraft mc) {
        if (!enabled || mc.level == null || mc.player == null) {
            hits = List.of();
            return;
        }
        ticks++;
        if (ticks % 20 == 0) scan(mc);
        if (ticks % 10 == 0) drawHighlights(mc);
    }

    private static boolean isStorage(BlockEntity be) {
        if (be instanceof ChestBlockEntity
                || be instanceof BarrelBlockEntity
                || be instanceof ShulkerBoxBlockEntity) return true;
        return includeHoppers
                && (be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity);
    }

    private static void scan(Minecraft mc) {
        var level = mc.level;
        ChunkPos center = mc.player.chunkPosition();
        int r = mc.options.getEffectiveRenderDistance();
        List<Hit> found = new ArrayList<>();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int cx = center.x() + dx, cz = center.z() + dz;
                ChunkAccess access = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(access instanceof LevelChunk chunk)) continue;
                int count = 0;
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    if (isStorage(e.getValue())) count++;
                }
                if (count > 0) found.add(new Hit(new ChunkPos(cx, cz), count));
            }
        }
        found.sort(Comparator.comparingInt(h ->
                Math.abs(h.pos().x() - center.x()) + Math.abs(h.pos().z() - center.z())));
        hits = found;
    }

    private static void drawHighlights(Minecraft mc) {
        var level = mc.level;
        int py = mc.player.blockPosition().getY();
        int limit = Math.min(12, hits.size());

        for (int i = 0; i < limit; i++) {
            ChunkPos cp = hits.get(i).pos();
            double x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
            double x1 = x0 + 16, z1 = z0 + 16;
            for (int dy = -2; dy <= 4; dy += 2) {
                double y = py + dy + 0.5;
                for (double t = 0; t <= 16; t += 2) {
                    level.addParticle(ParticleTypes.END_ROD, x0 + t, y, z0, 0, 0, 0);
                    level.addParticle(ParticleTypes.END_ROD, x0 + t, y, z1, 0, 0, 0);
                    level.addParticle(ParticleTypes.END_ROD, x0, y, z0 + t, 0, 0, 0);
                    level.addParticle(ParticleTypes.END_ROD, x1, y, z0 + t, 0, 0, 0);
                }
            }
        }
    }
}
