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

package dev.lambdaurora.funny_world_upgrader.mixin;

import dev.lambdaurora.funny_world_upgrader.FunnyWorldUpgrader;
import dev.lambdaurora.funny_world_upgrader.OldOverworldHeightLimitView;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.ChunkTickScheduler;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static dev.lambdaurora.funny_world_upgrader.FunnyWorldUpgrader.*;

@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {
    @Inject(method = "deserialize",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtCompound;getLong(Ljava/lang/String;)J", ordinal = 0),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILHARD)
    private static void onLightCheck(ServerWorld world, StructureManager structureManager, PointOfInterestStorage poiStorage,
                                     ChunkPos pos, NbtCompound nbt,
                                     CallbackInfoReturnable<ProtoChunk> cir,
            /* --------------------- Locals ------------------------------ */
                                     ChunkGenerator chunkGenerator, BiomeSource biomeSource,
                                     NbtCompound levelNbt, BiomeArray biomeArray, UpgradeData upgradeData,
                                     ChunkTickScheduler<Block> chunkBlockTickScheduler, ChunkTickScheduler<Fluid> chunkFluidTickScheduler,
                                     boolean lightOn, NbtList sectionsNbt, int verticalSectionsCount, ChunkSection[] sections,
                                     boolean hasSkyLight, ChunkManager chunkManager, LightingProvider lightingProvider) {
        int dataVersion = FunnyWorldUpgrader.DATA_VERSION_THREAD_LOCAL.get();

        if (dataVersion > 2692
                || world.getDimension() != world.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).get(DimensionType.OVERWORLD_ID))
            // New Chunk with proper format or non-overworld.
            return;

        // BiomeArray upgrade (warning: this is bad)
        var oldBiomeArray = new BiomeArray(world.getRegistryManager().get(Registry.BIOME_KEY),
                OldOverworldHeightLimitView.SELF, pos, biomeSource,
                levelNbt.contains("Biomes", 11) ? levelNbt.getIntArray("Biomes") : null);
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

        tryFixNbtSections(levelNbt, "PostProcessing", world);

        if (!DROP_PROTO_CHUNKS) {
            tryFixNbtSections(levelNbt, "Lights", world);
            tryFixNbtSections(levelNbt, "LiquidsToBeTicked", world);
            tryFixNbtSections(levelNbt, "ToBeTicked", world);
        }

        if (DROP_PROTO_CHUNKS && ChunkSerializer.getChunkType(nbt) == ChunkStatus.ChunkType.PROTOCHUNK) {
            cir.setReturnValue(new ProtoChunk(pos, UpgradeData.NO_UPGRADE_DATA, world));
        }
    }
}
