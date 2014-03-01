/*******************************************************************************
 * Copyright 2014 Spyros Papageorgiou
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
 * This producer models a wind farm composed by individuals wind turbines. It
 * uses power curve to model its turbine and also model wind shear and
 * turbulence effects
 * 
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("wind-farm")
public class WindFarm extends Producer
{

  private static final double DEFAULT_WIND_FARM_COST_PER_KWH = 0.08;
  private static final int DEFAULT_WIND_FARM_PROFILE_HOURS = 24;
  private static final double CELCIUS_TO_KELVIN = 273.15;

  /**
   * Constructs an empty wind farm.
   */
  public WindFarm ()
  {
    super("Wind farm", PowerType.WIND_PRODUCTION,
          DEFAULT_WIND_FARM_PROFILE_HOURS, 0);
    this.costPerKwh = DEFAULT_WIND_FARM_COST_PER_KWH;
  }

  // The list of turbines in this farm
  @XStreamImplicit
  private List<WindTurbine> turbines = new ArrayList<WindTurbine>();

  /**
   * Adds the given wind turbine to the farm
   * 
   * @param windTurbine
   *          The wind turbine object
   */
  public void addWindTurbine (WindTurbine windTurbine)
  {
    if (windTurbine == null)
      return;
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
   * 
   * @return
   */
  public List<WindTurbine> getTurbineList ()
  {
    return Collections.unmodifiableList(turbines);
  }

  /**
   * Calculate the total output of this farm by calling the the getOutput method
   * on each wind turbine.
   * 
   * @param temperature
   * @param windSpeed
   * @return
   */
  public double getPowerOutput (double temperature, double windSpeed)
  {
    double sumOutput = 0;
    for (WindTurbine wt: turbines) {
      if (Math.abs(sumOutput) < Math.abs(preferredOutput))
        sumOutput += wt.getPowerOutput(temperature, windSpeed);
      else
        break;
    }
    if (Double.isInfinite(sumOutput) || Double.isNaN(sumOutput)) {
      String arguments =
        String.format("Wind: %f Temperature: %f%n", windSpeed, temperature);
      throw new IllegalStateException("Power produced isn't a number. "
                                      + arguments);
    }
    return sumOutput;
  }

  @Override
  public double getOutput (WeatherReport weatherReport)
  {
    return getPowerOutput(weatherReport.getTemperature() + CELCIUS_TO_KELVIN,
                          weatherReport.getWindSpeed());
  }

  @Override
  public double
    getOutput (int timeslotIndex,
               WeatherForecastPrediction weatherForecastPrediction,
               double previousOutput)
  {
    return getPowerOutput(weatherForecastPrediction.getTemperature()
                                  + CELCIUS_TO_KELVIN,
                          weatherForecastPrediction.getWindSpeed());
  }

  /**
   * This function is called after de-serialization
   */
  protected Object readResolve ()
  {
    this.name = "Wind farm";
    initialize(name, PowerType.WIND_PRODUCTION,
               DEFAULT_WIND_FARM_PROFILE_HOURS, upperPowerCap,
               IdGenerator.createId());
    for (WindTurbine wt: turbines) {
      wt.setRs(seed);
      wt.setTimeslotLengthInMin(timeslotLengthInMin);
    }
    return this;
  }
}
