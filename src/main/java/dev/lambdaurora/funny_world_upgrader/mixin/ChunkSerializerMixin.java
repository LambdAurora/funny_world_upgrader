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
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.ChunkTickScheduler;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

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
        if (!FunnyWorldUpgrader.tryConvert(world, pos, nbt, biomeSource, levelNbt, biomeArray, sections))
            cir.setReturnValue(new ProtoChunk(pos, UpgradeData.NO_UPGRADE_DATA, world));
    }
}
