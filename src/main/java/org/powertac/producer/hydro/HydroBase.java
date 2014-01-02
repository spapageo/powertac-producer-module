/**
 * 
 */
package org.powertac.producer.hydro;

import org.powertac.producer.utils.Curve;

/**
 * @author Spyros Papageorgiou
 *
 */
public abstract class HydroBase {
	protected Curve inputFlow;
	protected double minFlow;
	protected double maxFlow;
	protected double waterDensity = 999.972;
	protected double g = 9.80665;
	protected Curve turbineEfficiency;
	protected double volume;
	protected double installedCapacity;
	protected double height;
	
	public HydroBase( Curve inputFlow,double minFlow, double maxFlow,Curve turbineEfficiency,double initialVolume,double installedCapacity,double initialHeight){
		this.inputFlow = inputFlow;
		this.minFlow = minFlow;
		this.maxFlow = maxFlow;
		this.turbineEfficiency = turbineEfficiency;
		this.volume = initialVolume;
		this.height = initialHeight;
	};
	
	protected double getOutput(int day){
		if(day < 0 || day > 366)
			throw new IllegalArgumentException();
		
		
		
		double waterFlow = getFlow(inputFlow.value(day));
		
		double turbEff = turbineEfficiency.value(waterFlow);
		
		double power = getWaterPower(turbEff, waterFlow, height);
		
		updateVolume(inputFlow.value(day));
		updateHeigth();
		
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
	 * @return the waterDensity
	 */
	public double getWaterDensity() {
		return waterDensity;
	}

	/**
	 * @param waterDensity the waterDensity to set
	 */
	public void setWaterDensity(double waterDensity) {
		this.waterDensity = waterDensity;
	}

	/**
	 * @return the g
	 */
	public double getG() {
		return g;
	}

	/**
	 * @param g the g to set
	 */
	public void setG(double g) {
		this.g = g;
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

	/**
	 * @return the installedCapacity
	 */
	public double getInstalledCapacity() {
		return installedCapacity;
	}

	/**
	 * @param installedCapacity the installedCapacity to set
	 */
	public void setInstalledCapacity(double installedCapacity) {
		this.installedCapacity = installedCapacity;
	}
}
