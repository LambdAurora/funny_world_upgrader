/*
 * Copyright (c) 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.funny_world_upgrader;

import dev.lambdaurora.funny_world_upgrader.mixin.BiomeArrayAccessor;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionType;

/**
 * Represents the Funny World Upgrader mod.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class FunnyWorldUpgrader implements ModInitializer {
    public static final int HORIZONTAL_SECTION_COUNT = MathHelper.log2DeBruijn(16) - 2;
    public static final boolean DROP_PROTO_CHUNKS = Boolean.getBoolean("drop-proto-chunks");

    public static final ThreadLocal<Integer> DATA_VERSION_THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public void onInitialize() {
        System.out.println("Trans rights are human rights.");
    }

    /**
     * Attempts to converts old sections to new sections with shifting.
     *
     * @param level the level NBT
     * @param key the key to the list of sections
     * @param view height limit view to get the expected vertical sections
     */
    public static void tryFixNbtSections(NbtCompound level, String key, HeightLimitView view) {
        if (level.contains("Lights", NbtElement.LIST_TYPE)) {
            fixNbtSections(level.getList(key, NbtElement.LIST_TYPE), view);
        }
    }

    /**
     * Attempts to converts old sections to new sections with shifting.
     *
     * @param list the list to fix
     * @param view height limit view to get the expected vertical sections
     */
    public static void fixNbtSections(NbtList list, HeightLimitView view) {
        if (list.size() != view.countVerticalSections()) {
            for (int i = 0; i < 4; i++)
                list.add(0, new NbtList());
            for (int i = 0; i < 4; i++)
                list.add(new NbtList());
        }
    }

    public static boolean tryConvert(ServerWorld world,
                                     ChunkPos pos, NbtCompound nbt,
                                     BiomeSource biomeSource,
                                     NbtCompound levelNbt, BiomeArray biomeArray,
                                     ChunkSection[] sections) {
        // We don't touch non-overworld dimensions
        if (world.getDimension() != world.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).get(DimensionType.OVERWORLD_ID))
            return true;

        // Chunks with proper world height have a biome array of length 1536, before it was 1024.
        var biomesNbt = levelNbt.contains("Biomes", 11) ? levelNbt.getIntArray("Biomes") : null;
        if (biomesNbt != null && biomesNbt.length == 1536)
            return true;

        int dataVersion = FunnyWorldUpgrader.DATA_VERSION_THREAD_LOCAL.get();

        // Was generated while there was the datapack for sure
        // Means the biomes array was null if it got here and this condition is true.
        if (dataVersion > 2692)
            return true;

        // BiomeArray upgrade (warning: this is bad)
        var oldBiomeArray = new BiomeArray(world.getRegistryManager().get(Registry.BIOME_KEY),
                OldOverworldHeightLimitView.SELF, pos, biomeSource,
                biomesNbt);
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                for (int y = 0; y < 96; y++) {
                    int oldY = y;
                    if (y < 16) {
                        oldY = 0;
                    } else if (y >= 80) {
                        oldY = 63;
                    }
                    var old = oldBiomeArray.getBiomeForNoiseGen(x * 4, oldY * 4, z * 4);
                    var i = y << HORIZONTAL_SECTION_COUNT * 2 | z << HORIZONTAL_SECTION_COUNT | x;
                    ((BiomeArrayAccessor) biomeArray).getData()[i] = old;
                }
            }
        }

        if (!DROP_PROTO_CHUNKS || ChunkSerializer.getChunkType(nbt) == ChunkStatus.ChunkType.LEVELCHUNK) {
            for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                int sectionY = world.sectionIndexToCoord(sectionIndex);
                if (sections[sectionIndex] == null) {
                    // Custom logic needed!
                    if (sectionY < 0) {
                        // Time to put funny blocks?
                        var section = new ChunkSection(sectionY);
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = 0; y < 16; y++) {
                                    section.getContainer().set(x, y, z, Blocks.DEEPSLATE.getDefaultState());
                                }
                            }
                        }

                        if (sectionY == world.getBottomSectionCoord()) {
                            // Copy bedrock.
                            var oldBottomSection = sections[world.sectionCoordToIndex(0)];
                            if (oldBottomSection != null) {
                                for (int x = 0; x < 16; x++) {
                                    for (int z = 0; z < 16; z++) {
                                        for (int y = 0; y < 5; y++) {
                                            if (oldBottomSection.getBlockState(x, y, z).isOf(Blocks.BEDROCK)) {
                                                section.getContainer().set(x, y, z, Blocks.BEDROCK.getDefaultState());
                                            }
                                        }
                                    }
                                }
                            } else {
                                for (int x = 0; x < 16; x++) {
                                    for (int z = 0; z < 16; z++) {
                                        section.getContainer().set(x, 0, z, Blocks.BEDROCK.getDefaultState());
                                    }
                                }
                            }
                        }

                        section.calculateCounts();
                        sections[sectionIndex] = section;
                    }
                } else if (sectionY == 0) {
                    var oldBottomSection = sections[sectionIndex];
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = 0; y < 5; y++) {
                                if (oldBottomSection.getBlockState(x, y, z).isOf(Blocks.BEDROCK)) {
                                    oldBottomSection.setBlockState(x, y, z, Blocks.DEEPSLATE.getDefaultState(), false);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (levelNbt.contains("Heightmaps", NbtElement.COMPOUND_TYPE)) {
            var heightmaps = levelNbt.getCompound("Heightmaps");

            heightmaps.getKeys().forEach(key -> {
                var heightmapData = heightmaps.getLongArray(key);
                var oldElementBits = MathHelper.log2DeBruijn(257);
                var oldArray = new PackedIntegerArray(oldElementBits, 256);
                System.arraycopy(heightmapData, 0, oldArray.getStorage(), 0, heightmapData.length);
                var newElementBits = MathHelper.log2DeBruijn(world.getHeight() + 1);
                var newArray = new PackedIntegerArray(newElementBits, 256);
                for (int i = 0; i < 256; i++) {
                    newArray.set(i, oldArray.get(i) - world.getBottomY());
                }
                heightmaps.putLongArray(key, newArray.getStorage());
            });
        }

        tryFixNbtSections(levelNbt, "PostProcessing", world);

        if (!DROP_PROTO_CHUNKS) {
            tryFixNbtSections(levelNbt, "Lights", world);
            tryFixNbtSections(levelNbt, "LiquidsToBeTicked", world);
            tryFixNbtSections(levelNbt, "ToBeTicked", world);
        }

        return !(DROP_PROTO_CHUNKS && ChunkSerializer.getChunkType(nbt) == ChunkStatus.ChunkType.PROTOCHUNK);
    }
}
