/**
 * 
 */
package org.powertac.producer.hydro;

import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.producer.utils.Curve;

/**
 * @author Spyros Papageorgiou
 *
 */
public class Dam extends HydroBase{

	private Curve volumeHeight;
	
	private Curve invCurveOut;
	
	public Dam(String name,Curve inputFlow, double minFlow, double maxFlow,
			Curve turbineEfficiency, Curve volumeHeigth, double initialVolume, double capacity) {
		super(name,inputFlow, minFlow, maxFlow, turbineEfficiency,initialVolume,volumeHeigth.value(initialVolume),capacity);
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
		if(preferredOutput == upperPowerCap){
			return maxFlow;
		}else{
			return invCurveOut.value(preferredOutput/height);
		}
	}

	@Override
	protected void updateHeigth() {
		height = volumeHeight.value(volume);
	}

	@Override
	protected double getOutput(WeatherReport weatherReport) {
		return getOutput(this.timeService.getCurrentDateTime().getDayOfYear());
	}

	@Override
	protected double getOutput(int timeslotIndex,
			WeatherForecastPrediction weatherForecastPrediction) {
		return getOutput(this.timeslotService.getTimeForIndex(timeslotIndex).toDateTime().getDayOfYear());
	}

}
