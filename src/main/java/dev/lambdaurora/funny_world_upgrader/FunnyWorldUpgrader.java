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

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Represents the Funny World Upgrader mod.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class FunnyWorldUpgrader implements ModInitializer {
    public static final String NAMESPACE = "funny_world_upgrader";

    public static final int HORIZONTAL_SECTION_COUNT = MathHelper.log2DeBruijn(16) - 2;
    public static final int HORIZONTAL_BIT_MASK = (1 << HORIZONTAL_SECTION_COUNT) - 1;

    public static final ThreadLocal<Integer> DATA_VERSION_THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public void onInitialize() {
        System.out.println("Trans rights are human rights.");
    }

    public static Identifier id(String path) {
        return new Identifier(NAMESPACE, path);
    }
}
