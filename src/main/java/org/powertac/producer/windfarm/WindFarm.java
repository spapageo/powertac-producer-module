/**
 * 
 */
package org.powertac.producer.windfarm;

import java.util.ArrayList;
import java.util.List;

import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.producer.Producer;

/**
 * @author Spyros Papageorgiou
 * 
 */
public class WindFarm extends Producer
{

  public WindFarm (String name)
  {
    super(name, PowerType.WIND_PRODUCTION, 24, 0);
  }

  // The list of turbines in this farm
  private List<WindTurbine> turbines = new ArrayList<>();
  // The maximum/rated output of the farm
  private double ratedOutput;

  /**
   * Adds the given wind turbine to the farm
   * 
   * @param windTurbine
   *          The wind turbine object
   */
  public void addWindTurbine (WindTurbine windTurbine)
  {
    turbines.add(windTurbine);
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

  /**
   * @return the ratedOutput
   */
  public double getRatedOutput ()
  {
    return ratedOutput;
  }

  /**
   * @param ratedOutput
   *          the ratedOutput to set
   */
  public void setRatedOutput (double ratedOutput)
  {
    this.ratedOutput = ratedOutput;
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
}
