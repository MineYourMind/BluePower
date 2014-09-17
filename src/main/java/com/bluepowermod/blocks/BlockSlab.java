/*
 * This file is part of Blue Power.
 *
 *      Blue Power is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      Blue Power is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with Blue Power.  If not, see <http://www.gnu.org/licenses/>
 */

package com.bluepowermod.blocks;

import com.bluepowermod.init.BPBlocks;
import com.bluepowermod.util.Refs;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.world.World;

/**
 * Created by Quetzi on 17/09/14.
 */
public class BlockSlab extends net.minecraft.block.BlockSlab {

    public BlockSlab(boolean isFullBlock, Material material) {

        super(isFullBlock, material);
    }

    @SideOnly(Side.CLIENT)
    private static boolean func_150003_a(Block block) {

        return block == BPBlocks.gravel_slab || block == BPBlocks.sand_slab;
    }

    /**
     * Gets an item for the block being called on. Args: world, x, y, z
     */
    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World p_149694_1_, int p_149694_2_, int p_149694_3_, int p_149694_4_) {

        return func_150003_a(this) ? Item.getItemFromBlock(this) : (this == BPBlocks.double_gravel_slab ? Item.getItemFromBlock(BPBlocks.gravel_slab) : (this == BPBlocks.double_sand_slab ? Item.getItemFromBlock(BPBlocks.sand_slab) : Item.getItemFromBlock(BPBlocks.gravel_slab)));
    }

    /**
     * getFullSlabName
     * @param p_150002_1_
     * @return
     */
    @Override
    public String func_150002_b(int p_150002_1_) {

        if (this == BPBlocks.gravel_slab) {
            return Refs.GRAVELSLAB_NAME;
        }
        if (this == BPBlocks.sand_slab) {
            return Refs.SANDSLAB_NAME;
        }

        return null;
    }
}
