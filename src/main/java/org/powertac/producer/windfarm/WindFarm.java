/*******************************************************************************
 * Copyright 2014 Spyridon Papageorgiou
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
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("wind-farm")
public class WindFarm extends Producer
{

  public WindFarm ()
  {
    super("Wind farm", PowerType.WIND_PRODUCTION, 24, 0);
    this.costPerKwh = 0.08;
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
    if (Double.isInfinite(sumOutput) || Double.isNaN(sumOutput))
      throw new IllegalStateException("Power produced isn't a number.");
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
  protected Object readResolve ()
  {
    this.name = "Wind farm";
    initialize(name, PowerType.WIND_PRODUCTION, 24, upperPowerCap,
               IdGenerator.createId());
    for (WindTurbine wt: turbines) {
      wt.setRs(seed);
      wt.setTimeslotLengthInMin(timeslotLengthInMin);
    }
    return this;
  }
}
