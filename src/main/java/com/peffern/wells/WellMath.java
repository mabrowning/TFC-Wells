package com.peffern.wells;

import java.util.HashMap;
import java.util.Map;

import com.dunk.tfc.api.Constant.Global;

/**
 * Utility class for all well environmental calculations
 * @author peffern
 *
 */
public final class WellMath 
{
	/**
	 * Prevent instantiation
	 */
	private WellMath()
	{
	}
	
	/** Unmodified flow rate */
	public static final float BASE_FLOW_RATE = 10f;

	/** Table of rock types to porosity fractions */
	private static final Map<String,Float> ROCK_POROSITY_TABLE;
	
	/** Porosity data */
	static
	{		
		//Porosity Data provided by ADVerb1997
		Map<String,Float> POROSITY_TABLE = new HashMap<String,Float>();
		POROSITY_TABLE.put(		"Chalk",			0.132000f		);
		POROSITY_TABLE.put(		"Chert",			0.060333f		);
		POROSITY_TABLE.put(		"Claystone",		0.273652f		);
		POROSITY_TABLE.put(		"Conglomerate",		0.081333f		);
		POROSITY_TABLE.put(		"Dolomite",			0.025000f		);
		POROSITY_TABLE.put(		"Limestone",		0.113000f		);
		POROSITY_TABLE.put(		"Rock Salt",		0.019967f		);
		POROSITY_TABLE.put(		"Shale",			0.160600f		);
		POROSITY_TABLE.put(		"Gneiss",			0.010000f		);
		POROSITY_TABLE.put(		"Marble",			0.012500f		);
		POROSITY_TABLE.put(		"Phyllite",			0.250000f		);
		POROSITY_TABLE.put(		"Quartzite",		0.003000f		);
		POROSITY_TABLE.put(		"Schist",			0.023000f		);
		POROSITY_TABLE.put(		"Slate",			0.022000f		);
		POROSITY_TABLE.put(		"Diorite",			0.015000f		);
		POROSITY_TABLE.put(		"Gabbro",			0.015000f		);
		POROSITY_TABLE.put(		"Granite",			0.020500f		);
		POROSITY_TABLE.put(		"Andesite",			0.125000f		);
		POROSITY_TABLE.put(		"Basalt",			0.055000f		);
		POROSITY_TABLE.put(		"Dacite",			0.002500f		);
		POROSITY_TABLE.put(		"Rhyolite",			0.050000f		);
		
		
		//Set the minimum rock porosity to 0.1 times the maximum rock porosity and interpolate the other values
		float max = 0;
		float min = 0;
		for(Float foo : POROSITY_TABLE.values())
		{
			float f = foo.floatValue();
			if (f > max)
				max = f;
			if (f < min || min == 0)
				min = f;
		}
		
		float ratio = 0.1f;
		
		ROCK_POROSITY_TABLE = new HashMap<String,Float>();
		for(Map.Entry<String,Float> e : POROSITY_TABLE.entrySet())
		{
			ROCK_POROSITY_TABLE.put(e.getKey(), ((max-max*ratio)/(max-min)*(e.getValue() - max)+(max)));
		}
				
	}
	
	/**
	 * Get the porosity fraction for a given rock name
	 * @param rockName the name of the rock to query
	 * @return the porosity fraction (0-1)
	 */
	public static float getRockPorosityMod(String rockName)
	{
		//get from the table
		Float ret = ROCK_POROSITY_TABLE.get(rockName);
		if (ret != null)
			return ret.floatValue();
		else //default to granite
			return ROCK_POROSITY_TABLE.get("Granite").floatValue();
	}
	
	/**
	 * Get the EVT ratio well modifier (0-1).
	 * Log scales from 0.5 (EVT=16) to 1 (EVT=0.125)
	 * @param evt evt value
	 * @return modifier fraction
	 */
	public static float getEVTMod(float evt)
	{
		//log scale EVT on the range 1 (for minimum evt) to 1/2 (for maximum evt)
		double s = Math.exp(Math.log(2d)/7d);
		double e = (double)evt;
		double v = 1d/(Math.pow(s, 3+Math.log(e)/Math.log(2)));
		return (float)v;
	}
	
	/**
	 * Get the rainfall well modifier (0-1).
	 * Log scales from 0.5(Rainfall=62.5) to 1(Rainfall=8000)
	 * @param rainfall rainfall value
	 * @return modifier fraction
	 */
	public static float getRainfallMod(float rainfall)
	{
		//log scale rain on the range 1 (for maximum rain) to 1/2 (for minimum rain)
		double s = Math.exp(Math.log(2d)/7d);
		double r = (double)rainfall;
		double v = 1d/(Math.pow(s,3-Math.log(r/1000d)/Math.log(2)));
		return (float)v;
	}
	
	/**
	 * Computes the total flow rate in mB/tick for a given set of environment parameters
	 * @param evt evt value
	 * @param rainfall rainfall value
	 * @param rock rock type
	 * @return
	 */
	public static float computeFlowRate(float evt, float rainfall, String rock)
	{
		float rockMod = WellMath.getRockPorosityMod(rock);
		float rainMod = WellMath.getRainfallMod(rainfall);
		float evtMod = WellMath.getEVTMod(evt);		
		float modTotal = rockMod*rainMod*evtMod;
		//restrict outlier values
		if (modTotal < 0.01f)
			modTotal = 0.01f;
		if(modTotal > 0.25f)
			modTotal = 0.25f;
		return BASE_FLOW_RATE*modTotal;
	}
	
	/**
	 * Computes the size of a well in mB based on its height and depth
	 * @param y the height of the top of the well
	 * @param depth the depth of the well
	 * @return well size (blocks)
	 */
	public static float computeBufferSize(int y, int depth)
	{
		int bottom = y - depth;
		//don't fill above sea level
		int v = Math.min(y, Global.SEALEVEL) - bottom;
		//don't have negative size
		if (v < 0) 
			v = 0;
		return (float)v * 1000f;
	}
}
