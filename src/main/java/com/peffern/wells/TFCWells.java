package com.peffern.wells;

import com.dunk.tfc.api.TFCItems;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * Wells addon for TFC.
 * Adds a well block that can be placed over a hole and will accumulate water.
 * Rightclick with a bucket to attach it.
 * Rightclick again to lower, and again to raise. if the well had water, it will fill the bucket.
 * @author peffern
 *
 */
@Mod(modid = TFCWells.MODID, name = TFCWells.MODNAME, version = TFCWells.VERSION, dependencies = "required-after:" + "terrafirmacraftplus" + ";")
public class TFCWells 
{
	/** Mod instance Singleton */
	@Instance(TFCWells.MODID)
	public static TFCWells instance;
	
	@SidedProxy(clientSide = "com.peffern.wells.ClientProxy", serverSide = "com.peffern.wells.CommonProxy")
	public static CommonProxy proxy;

	/** Mod ID String */
	public static final String MODID = "tfcwells";
	
	/** Mod Name */
	public static final String MODNAME = "TFC Wells";
	
	/** Mod Version */
	public static final String VERSION = "1.1";
	
	/** The Well Block*/
	public static Block well;
	
	/** hidden well rope item */
	protected static Item wellRope;
	
	/**
	 * Mod setup and item registration
	 * @param e initialization event
	 */
	@EventHandler
	public void init(FMLInitializationEvent e)
	{
		//setup blocks
		well = new BlockWell().setBlockName("Well").setHardness(10);
		
		wellRope = new Item()
		{
			@Override
			public void registerIcons(IIconRegister registerer)
			{
				this.itemIcon = registerer.registerIcon(TFCWells.MODID + ":" + "wellRope");
			}
		}.setUnlocalizedName("wellHiddenRopeDummyItem");
		GameRegistry.registerItem(wellRope, wellRope.getUnlocalizedName());
		
		//registration
		proxy.registerTileEntities();
		GameRegistry.registerBlock(well, ItemBlockWell.class, "Well");
		
		//crafting
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(well,1), "ABA","CDC","AEA",'A',"plankWood",'B',"itemSaw",'C',"ingotIron",'D',TFCItems.rope, 'E', TFCItems.woodenBucketEmpty));

		FMLCommonHandler.instance().bus().register(new CraftingHandler());
		
	}
	
	
	
	
}
