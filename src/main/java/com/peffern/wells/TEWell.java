package com.peffern.wells;


import com.dunk.tfc.Core.TFC_Climate;
import com.dunk.tfc.Core.TFC_Core;
import com.dunk.tfc.Items.ItemLooseRock;
import com.dunk.tfc.TileEntities.NetworkTileEntity;
import com.dunk.tfc.api.TFCFluids;
import com.dunk.tfc.api.TFCItems;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import static com.peffern.wells.WellState.*;

public class TEWell extends NetworkTileEntity
{
	
	/** Well refill rate (in mB/tick) */
	private float rate = 0;
	/** Well buffer size (in mB) */
	private float size = 0;
	/** Current well contents (in mB) */
	private float contents = 0;
	/** Current well depth (in blocks) */
	private int depth = 0;
	/** Well State*/
	protected WellState wellState = READY;
	/** Internal ItemStack */
	protected ItemStack item = null;
	/** Current validation counter (in ticks) */
	private int validationCheck = 60;
	/** avoid rendering too early */
	private boolean shouldRender = false;

	/**
	 * Handle refilling and validation
	 */
	@Override
	public void updateEntity()
	{
		if(!worldObj.isRemote)
		{
			
			//fill the well
			if(contents < size)
			{
				contents += Math.min(rate, size-contents);
			}
			else if(contents > size)
				contents = size;
			
			//every time the validation counter hits 0
			if (validationCheck <= 0)
			{
				shouldRender = true;
				//verify the hole is still good (also updates the depth)
				if (((BlockWell) worldObj.getBlock(xCoord, yCoord, zCoord)).canBlockStay(worldObj, xCoord, yCoord, zCoord))
				{
					//recalculate the properties
					size = calcBuffer();
					rate = calcRefill();
					
					//reset the counter
					validationCheck = 60;
					
					/*NBTTagCompound nbt = new NBTTagCompound();
					writeToNBT(nbt);
					broadcastPacketInRange(createDataPacket(nbt));
					this.worldObj.func_147479_m(xCoord, yCoord, zCoord);*/


				}
				else //hole broke so break the well
				{
					worldObj.setBlockToAir(xCoord, yCoord, zCoord);
					this.dropItem(worldObj, xCoord,yCoord,zCoord);
					worldObj.spawnEntityInWorld(new EntityItem(worldObj, xCoord, yCoord, zCoord, new ItemStack(TFCWells.well, 1)));
				}
				
				
			}
			else //decrement the counter
				validationCheck--;
			
			
		}
		
	}
	
	/**
	 * Handle player rightclick interaction
	 * @param world world context
	 * @param player player doing the clicking
	 * @param block the well Block
	 * @return
	 */
	protected void handleInteraction(World world, EntityPlayer player, BlockWell block)
	{
		//get the tile entity and read the contents to the player
		//detect bucket
		switch(wellState)
		{
			//entry point to well flowchart
			case READY:
			{
				ItemStack eq = player.getCurrentEquippedItem();
				if(eq != null && (eq.getItem() instanceof IFluidContainerItem || FluidContainerRegistry.isEmptyContainer(eq)) && !player.isSneaking())
				{
					//load well with fluid container
					wellState = LOADED;
					item = eq.copy();
					player.inventory.decrStackSize(player.inventory.currentItem, 1);
				}
				else
				{
					//display well contents
					ChatComponentTranslation cct1 = new ChatComponentTranslation("gui.Well.prefix");
					ChatComponentTranslation cct2 = new ChatComponentTranslation(" "+Integer.toString(new Float(getContents()).intValue())+" ");
					ChatComponentTranslation cct3 = new ChatComponentTranslation("gui.Well.suffix");
					TFC_Core.sendInfoMessage(player, cct1.appendSibling(cct2).appendSibling(cct3));
				}
				break;
			}
			case LOADED: //lowering well bucket
			{
				if(player.isSneaking())
				{
					//unload
					TFC_Core.giveItemToPlayer(item, player);
					item = null;
					wellState = READY;
				}
				else
				{
					//lower and try to fill
					wellState = EMPTY_DOWN;
					int contents = (int)getContents();
					FluidStack cs = new FluidStack(TFCFluids.FRESHWATER,contents);
					//try 2 ways to fill: 1
					if(item.getItem() instanceof IFluidContainerItem)
					{
						
						FluidStack fs = ((IFluidContainerItem)item.getItem()).getFluid(item);
						if(fs == null || fs.isFluidEqual(cs));
						{
							int amt = ((IFluidContainerItem)item.getItem()).fill(item, cs, false);
							//fill the bucket
							if(drain(amt))
							{
								((IFluidContainerItem)item.getItem()).fill(item, cs, true);
								wellState = FULL_DOWN;
							}
						}
					}
					//try 2
					else if(FluidContainerRegistry.isEmptyContainer(item))
					{
						ItemStack out = FluidContainerRegistry.fillFluidContainer(cs, item);
						if(out != null)
						{
							int amount = FluidContainerRegistry.getContainerCapacity(out);
							//fill the bucket
							if(drain(amount))
							{
								item = out;
								wellState = FULL_DOWN;
							}
						}
					}
				}
				break;
			}
			//raise the empty bucket
			case EMPTY_DOWN:
			{
				wellState = EMPTY_UP;
				break;
			}
			//raise the full bucket
			case FULL_DOWN:
			{
				wellState = FULL_UP;
				break;
			}
			//give the player the item back
			case EMPTY_UP:
			{
				TFC_Core.giveItemToPlayer(item, player);
				item = null;
				wellState = READY;
				break;
			}
			//give the item back
			case FULL_UP:
			{
				TFC_Core.giveItemToPlayer(item, player);
				item = null;
				wellState = READY;
				break;
			}
		}
		
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
				
	}
	
	/**
	 * Calculates the well refill (mB/tick) rate
	 * @return refill rate
	 */
	private float calcRefill()
	{
		
		World world = getWorldObj();
		int x = xCoord;
		int y = yCoord;
		int z = zCoord;
		float rain = TFC_Climate.getRainfall(world,x,y,z);
		float evt = TFC_Climate.getCacheManager(world).getEVTLayerAt(x, z).floatdata1;
		String rock = ((ItemLooseRock)TFCItems.looseRock).metaNames[TFC_Climate.getRockLayer(world, x, y, z, TFC_Core.getRockLayerFromHeight(world, x, y, z)).data1];
		return WellMath.computeFlowRate(evt, rain, rock);
	}
	
	/**
	 * Calculates the well buffer size (mB)
	 * @return buffer size
	 */
	public float calcBuffer()
	{
		int y = yCoord;
		int depth = getDepth();
		return WellMath.computeBufferSize(y, depth);
	}
	
	/**
	 * Get the current well contents (mB)
	 * @return well contents
	 */
	public float getContents()
	{
		return contents;
	}
	
	/**
	 * Gets the current well depth (blocks)
	 * @return well depth
	 */
	public int getDepth()
	{
		return depth;
	}
	
	/**
	 * Changes the well depth in calculations
	 * @param d new depth
	 */
	public void updateDepth(int d)
	{
		depth = d;
	}
	
	/**
	 * Attempts to drain some liquid from the well contents
	 * @param amount amount to drain
	 * @return if draining was successful
	 */
	public boolean drain(float amount)
	{
		if (contents >= amount)
		{
			contents -= amount;
			return true;
		}
		else
			return false;
	}
	
	/**
	 * Attempts to add some liquid to the contents
	 * @param amount amount to add
	 */
	public void add(float amount)
	{
		contents += amount;
	}
	
	/**
	 * Drops the item currently contained in the well
	 * @param world the world
	 * @param x the x coordinate to drop the item at
	 * @param y the y coordinate to drop the item at
	 * @param z the z coordinate to drop the item at
	 */
	public void dropItem(World world, int x, int y, int z)
	{
		if(item != null)
		{
			world.spawnEntityInWorld(new EntityItem(world,x,y,z,item));
			item = null;
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		//write well spec to nbt
		nbttagcompound.setFloat("contents", contents);
		nbttagcompound.setFloat("rate", rate);
		nbttagcompound.setFloat("size", size);
		nbttagcompound.setInteger("depth", depth);
		if(item != null)
		{
			NBTTagCompound itemTag = item.writeToNBT(new NBTTagCompound());
			nbttagcompound.setTag("item", itemTag);

		}
		nbttagcompound.setInteger("state", wellState.ordinal());
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);
		//read well spec from nbt
		if(nbttagcompound.hasKey("item"))
		{
			item = ItemStack.loadItemStackFromNBT(nbttagcompound.getCompoundTag("item"));
		}
		else
			item = null;
		if(nbttagcompound.hasKey("contents"))
			contents = nbttagcompound.getFloat("contents");
		if(nbttagcompound.hasKey("rate"))
			rate = nbttagcompound.getFloat("rate");
		if(nbttagcompound.hasKey("size"))
			size = nbttagcompound.getFloat("size");
		if(nbttagcompound.hasKey("depth"))
			depth = nbttagcompound.getInteger("depth");
		if(nbttagcompound.hasKey("state"))
			wellState = WellState.values()[nbttagcompound.getInteger("state")];
	}
	
	@Override
	public void handleInitPacket(NBTTagCompound nbttagcompound) 
	{
	}

	@Override
	public void handleDataPacket(NBTTagCompound nbt) 
	{
	}

	@Override
	public void createInitNBT(NBTTagCompound nbt) 
	{
		
	}
	
	@Override
	public Packet getDescriptionPacket() {
		 NBTTagCompound nbtTag = new NBTTagCompound();
		 this.writeToNBT(nbtTag);
		 return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, nbtTag);
		 }
		 @Override
		 public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
		 readFromNBT(packet.func_148857_g());
		 }
}
