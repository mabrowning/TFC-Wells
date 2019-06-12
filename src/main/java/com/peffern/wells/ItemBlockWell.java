package com.peffern.wells;

import com.dunk.tfc.Items.ItemBlocks.ItemTerraBlock;
import com.dunk.tfc.api.Enums.EnumSize;
import com.dunk.tfc.api.Enums.EnumWeight;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

/**
 * Item block for well, specifies size and weight
 * @author peffern
 *
 */
public class ItemBlockWell extends ItemTerraBlock
{
	public ItemBlockWell(Block b)
	{
		super(b);
	}
	
	public EnumSize getSize(ItemStack is)
	{
		return EnumSize.LARGE;
	}
	
	public EnumWeight getWeight(ItemStack is)
	{
		return EnumWeight.HEAVY;
	}
}
