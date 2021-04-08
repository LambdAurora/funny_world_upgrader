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

import net.fabricmc.api.ModInitializer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.HeightLimitView;

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
}
