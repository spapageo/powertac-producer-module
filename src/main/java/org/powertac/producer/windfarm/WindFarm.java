/**
 * 
 */
package org.powertac.producer.windfarm;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Spyros Papageorgiou
 *
 */
public class WindFarm {
	
	// The list of turbines in this farm
	private List<WindTurbine> turbines = new ArrayList<>();
	// The maximum/rated output of the farm
	private double ratedOutput;
	
	/**
	 * Adds the given wind turbine to the farm
	 * @param windTurbine The wind turbine object
	 */
	public void addWindTurbine(WindTurbine windTurbine){
		turbines.add(windTurbine);
	}
	
	/**
	 * Removes the given wind turbine from the wind turbines in this farm.
	 * 
	 * @param windTurbine The wind turbine object
	 * @return True if the removal was successful
	 */
	public boolean removeWindTurbine(WindTurbine windTurbine){
		return turbines.remove(windTurbine);
	}
	
//	/**
//	 * Executes the project simulation and return the power output for the given inputs
//	 * @param temperature
//	 * @param windSpeed
//	 * @return
//	 */
//	public double getPowerOutput(double temperature, double windSpeed){
//		double sumOutput = 0;
//		for(WindTurbine wt : turbines){
//			sumOutput += wt.getPowerOutput(temperature, windSpeed);
//		}
//		return sumOutput;
//	}
//	
//	/**
//	 * Estimates the power output of the model for the given inputs
//	 * @param temperature
//	 * @param windSpeed
//	 * @return
//	 */
//	public double predictPowerOutput(double temperature, double windSpeed){
//		double sumOutput = 0;
//		for(WindTurbine wt : turbines){
//			sumOutput += wt.predictPowerOutput(temperature, windSpeed);
//		}
//		return sumOutput;
//	}

	/**
	 * @return the ratedOutput
	 */
	public double getRatedOutput() {
		return ratedOutput;
	}

	/**
	 * @param ratedOutput the ratedOutput to set
	 */
	public void setRatedOutput(double ratedOutput) {
		this.ratedOutput = ratedOutput;
	}
}
