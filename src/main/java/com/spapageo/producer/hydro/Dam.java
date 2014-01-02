/**
 * 
 */
package com.spapageo.producer.hydro;

import com.spapageo.producer.utils.Curve;

/**
 * @author Spyros Papageorgiou
 *
 */
public class Dam extends HydroBase{

	private Curve volumeHeight;
	
	private Curve invCurveOut;
	
	private double nextOutput;
	
	public Dam(Curve inputFlow, double minFlow, double maxFlow,
			Curve turbineEfficiency, Curve volumeHeigth, double initialVolume, double installedCapacity) {
		super(inputFlow, minFlow, maxFlow, turbineEfficiency,initialVolume,installedCapacity,volumeHeigth.value(initialVolume));
		this.volumeHeight = volumeHeigth;
		
		calculateInvOut();
	}
	
	protected void calculateInvOut(){
		Curve c = new Curve();
		c.setCanBeNegative(false);
		//calculate inverse curve output function for unit height
		for(double flow = minFlow; flow <= maxFlow; flow += (maxFlow - minFlow)/10){
			double pow =  getWaterPower(this.turbineEfficiency.value(flow), flow,1);
			c.add(flow, pow);
		}
		
		invCurveOut = c.getInvertiblePart();
	}

	@Override
	protected void updateVolume(double inputFlow) {
		this.volume =+ inputFlow*3600;
	}

	@Override
	protected double getFlow(double inputFlow) {
		if(nextOutput == installedCapacity){
			return maxFlow;
		}else{
			return invCurveOut.value(nextOutput/height);
		}
	}

	@Override
	protected void updateHeigth() {
		height = volumeHeight.value(volume);
	}
	
	protected void setPrefferedOutput(double preferredOut){
		
	}

}
