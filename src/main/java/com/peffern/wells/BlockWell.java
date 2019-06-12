package com.peffern.wells;

import com.dunk.tfc.Blocks.BlockTerraContainer;
import com.dunk.tfc.Core.TFCTabs;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The well block itself. Maintains that it is over an appropriately shaped hole.
 * @author peffern
 *
 */
public class BlockWell extends BlockTerraContainer
{
	/** Textures */
	protected IIcon emptyIcon;
	protected static IIcon ropeIcon;
	
	/**
	 * Constructs a BlockWell
	 */
	public BlockWell()
	{
		super(Material.wood);
		this.setCreativeTab(TFCTabs.TFC_DEVICES);
	}
	
	/**
	 * On rightclicking the welll
	 */
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityplayer, int par6, float par7, float par8, float par9)
	{
		if (!world.isRemote)
		{
			//break block if invalid
			if(!canBlockStay(world,x,y,z))
			{
				world.setBlockToAir(x, y, z);
				dropBlockAsItem(world, x, y, z, new ItemStack(this, 1));
				return false;
			}
			else
			{
				//handle TE and update texture
				TEWell te = (TEWell)world.getTileEntity(x, y, z);
				te.handleInteraction(world, entityplayer, this);
				return true;
			}
			

			
		}
		return false;
	}
	
	/**
	 * Make sure the well is in a valid position.
	 * SIDE EFFECT: also tells the tile entity how deep the hole is
	 */
	@Override
	public boolean canBlockStay(World world, int x, int y, int z)
	{
		//start below the well and look down until we fall out of the world
		for(int h = y-1; h > 0; h--)
		{
			//if we find a solid block, we're at the bottom of the hole
			if(!world.isAirBlock(x,h,z))
			{
				if(world.getBlock(x,h,z).isOpaqueCube())
				{
					TEWell te = (TEWell)world.getTileEntity(x, y, z);
					//update the depth to be the new value
					if(te != null)
						te.updateDepth(y-h-1);
					return true;
				}
				else
					return false; //if we find a torch or something, not only are we not done, the well is now ruined
			}
			else //we are at an air block below the well
			{
				//TEMPORARY check all four sides to make sure they're not air
				if(!world.getBlock(x-1,h,z).isOpaqueCube() || !world.getBlock(x+1,h,z).isOpaqueCube() || !world.getBlock(x, h, z-1).isOpaqueCube() || !world.getBlock(x,h,z+1).isOpaqueCube())
				{
					//if the air, the hole is open and the well fails
					return false;
				}
			}
		}
		return false;
	
	}
	
	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int  z)
	{
		return canBlockStay(world,x,y,z);
	}
	
	
	@Override
	public void registerBlockIcons(IIconRegister iconRegisterer)
	{
		super.registerBlockIcons(iconRegisterer);
		this.emptyIcon = iconRegisterer.registerIcon(TFCWells.MODID + ":" + "Well Empty");
		ropeIcon = iconRegisterer.registerIcon(TFCWells.MODID + ":" + "Well Rope");
		this.blockIcon = this.emptyIcon;
	}
	
	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityliving, ItemStack is)
	{
		if(!canBlockStay(world,x,y,z))
		{
			world.setBlockToAir(x, y, z);
			dropBlockAsItem(world, x, y, z, new ItemStack(this, 1));
		}
	}
	
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
	{
		if(!canBlockStay(world,x,y,z))
		{
			world.setBlockToAir(x, y, z);
			dropBlockAsItem(world, x, y, z, new ItemStack(this, 1));
		}
		
	}
	
	@Override
	public void onBlockHarvested (World world, int i, int j, int k, int l, EntityPlayer player)
	{
		TEWell te = (TEWell)world.getTileEntity(i, j, k);
		te.dropItem(world,i,j,k);
		super.onBlockHarvested(world, i, j, k, l, player);
		
	}
	
	@Override
	public TileEntity createNewTileEntity(World world, int meta)
	{
		return new TEWell();
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z)
	{
		return super.getCollisionBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}

	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}
	
	@Override
	public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 player, Vec3 view)
	{
		return super.collisionRayTrace(world, x, y, z, player, view);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess bAccess, int x, int y, int z, int side)
	{
		return true;
	}
	
	@Override
	protected void dropBlockAsItem(World world, int x, int y, int z, ItemStack is)
	{
		super.dropBlockAsItem(world, x, y, z, is);
	}
}
