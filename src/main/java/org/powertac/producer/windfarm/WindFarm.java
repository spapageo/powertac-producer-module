/**
 * 
 */
package org.powertac.producer.windfarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.powertac.common.IdGenerator;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.producer.Producer;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("wind-farm")
public class WindFarm extends Producer
{

  public WindFarm ()
  {
    super("Wind farm",PowerType.WIND_PRODUCTION, 24, 0);
  }

  // The list of turbines in this farm
  @XStreamImplicit
  private List<WindTurbine> turbines = new ArrayList<>();

  /**
   * Adds the given wind turbine to the farm
   * 
   * @param windTurbine
   *          The wind turbine object
   */
  public void addWindTurbine (WindTurbine windTurbine)
  {
    windTurbine.setRs(seed);
    turbines.add(windTurbine);
    windTurbine.setTimeslotLengthInMin(timeslotLengthInMin);
    this.upperPowerCap += windTurbine.getRatedOutput();
  }

  /**
   * Removes the given wind turbine from the wind turbines in this farm.
   * 
   * @param windTurbine
   *          The wind turbine object
   * @return True if the removal was successful
   */
  public boolean removeWindTurbine (WindTurbine windTurbine)
  {
    if (turbines.remove(windTurbine)) {
      upperPowerCap -= windTurbine.getRatedOutput();
      return true;
    }
    else {
      return false;
    }
  }
  
  /**
   * Returns the wind turbines in the farm.
   * @return
   */
  public List<WindTurbine> getTurbineList(){
    return Collections.unmodifiableList(turbines);
  }

  /**
   * Executes the project simulation and return the power output for the given
   * inputs
   * 
   * @param temperature
   * @param windSpeed
   * @return
   */
  public double getPowerOutput (double temperature, double windSpeed)
  {
    double sumOutput = 0;
    for (WindTurbine wt: turbines) {
      sumOutput += wt.getPowerOutput(temperature, windSpeed);
    }
    return sumOutput;
  }

  @Override
  protected double getOutput (WeatherReport weatherReport)
  {
    return getPowerOutput(weatherReport.getTemperature(),
                          weatherReport.getWindSpeed());
  }

  @Override
  protected double
    getOutput (int timeslotIndex,
               WeatherForecastPrediction weatherForecastPrediction)
  {
    return getPowerOutput(weatherForecastPrediction.getTemperature(),
                          weatherForecastPrediction.getWindSpeed());
  }

  /**
   * This function is called after de-serialization
   */
  protected Object readResolve(){
    this.name = "Wind farm";
    initialize(name, PowerType.WIND_PRODUCTION, 24, upperPowerCap,
               IdGenerator.createId());
    for(WindTurbine wt: turbines){
      wt.setRs(seed);
      wt.setTimeslotLengthInMin(timeslotLengthInMin);
    }
    return this;
  }
}
