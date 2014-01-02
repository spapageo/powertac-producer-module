/**
 * 
 */
package com.spapageo.producer.hydro;

import com.spapageo.producer.utils.Curve;

/**
 * @author Spyros Papageorgiou
 *
 */
public class RunOfRiver extends HydroBase{

	public RunOfRiver(Curve inputFlow, double minFlow, double maxFlow,
			Curve turbineEfficiency, double initialVolume,
			double installedCapacity, double initialHeight) {
		super(inputFlow, minFlow, maxFlow, turbineEfficiency, initialVolume,
				installedCapacity, initialHeight);
	}

	@Override
	protected void updateVolume(double inputFlow) {}

	@Override
	protected void updateHeigth() {}

	@Override
	protected double getFlow(double inputFlow) {
		return inputFlow;
	}
}