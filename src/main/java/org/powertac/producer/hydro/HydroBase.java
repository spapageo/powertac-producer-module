/**
 * 
 */
package org.powertac.producer.hydro;

import org.powertac.common.enumerations.PowerType;
import org.powertac.producer.Producer;
import org.powertac.producer.utils.Curve;

/**
 * @author Spyros Papageorgiou
 *
 */
public abstract class HydroBase extends Producer{
	protected Curve inputFlow;
	protected double minFlow;
	protected double maxFlow;
	private static double waterDensity = 999.972;
	private static double g = 9.80665;
	protected Curve turbineEfficiency;
	protected double volume;
	protected double height;
	
	public HydroBase( String name,Curve inputFlow,double minFlow, double maxFlow,
			Curve turbineEfficiency,double initialVolume,double initialHeight,double capacity){
		//no dam production put both on run of the river
		super(name, PowerType.RUN_OF_RIVER_PRODUCTION, 24, capacity);
		this.inputFlow = inputFlow;
		this.minFlow = minFlow;
		this.maxFlow = maxFlow;
		this.turbineEfficiency = turbineEfficiency;
		this.volume = initialVolume;
		this.height = initialHeight;
	}
	
	protected double getOutput(int day){
		if(day < 0 || day > 366)
			throw new IllegalArgumentException();
		
		double waterFlow = getFlow(inputFlow.value(day));
		
		double turbEff = turbineEfficiency.value(waterFlow);
		
		double power = -getWaterPower(turbEff, waterFlow, height);
		
		updateVolume(inputFlow.value(day));
		updateHeigth();
		if(power > 0)
			throw new IllegalStateException("I fucked up");
		return power;
		
		
	}
	
	protected double getWaterPower(double turbineEfficiency, double flow, double heigth){
		if(flow >= minFlow && flow <= maxFlow)
			return turbineEfficiency*waterDensity*g*heigth*flow;
		else if(flow > maxFlow)
			return turbineEfficiency*waterDensity*g*heigth*maxFlow;
		else
			return 0;
	}
	
	protected abstract void updateVolume(double inputFlow);
	
	protected abstract void updateHeigth();
	
	protected abstract double getFlow(double inputFlow);
	
	

	/**
	 * @return the inputFlow
	 */
	public Curve getInputFlow() {
		return inputFlow;
	}

	/**
	 * @param inputFlow the inputFlow to set
	 */
	public void setInputFlow(Curve inputFlow) {
		this.inputFlow = inputFlow;
	}

	/**
	 * @return the minFlow
	 */
	public double getMinFlow() {
		return minFlow;
	}

	/**
	 * @param minFlow the minFlow to set
	 */
	public void setMinFlow(double minFlow) {
		this.minFlow = minFlow;
	}

	/**
	 * @return the maxFlow
	 */
	public double getMaxFlow() {
		return maxFlow;
	}

	/**
	 * @param maxFlow the maxFlow to set
	 */
	public void setMaxFlow(double maxFlow) {
		this.maxFlow = maxFlow;
	}

	/**
	 * @return the turbineEfficiency
	 */
	public Curve getTurbineEfficiency() {
		return turbineEfficiency;
	}

	/**
	 * @param turbineEfficiency the turbineEfficiency to set
	 */
	public void setTurbineEfficiency(Curve turbineEfficiency) {
		this.turbineEfficiency = turbineEfficiency;
	}

	/**
	 * @return the volume
	 */
	public double getVolume() {
		return volume;
	}

	/**
	 * @param volume the volume to set
	 */
	public void setVolume(double volume) {
		this.volume = volume;
	}
}
