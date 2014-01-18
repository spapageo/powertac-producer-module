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
package org.powertac.producer.hydro;

import org.powertac.common.enumerations.PowerType;
import org.powertac.producer.Producer;
import org.powertac.producer.utils.Curve;

/**
 * This is the base class for the Dam and RunofRiver classes. Used to
 * encapsulate common behavior.
 * 
 * @author Spyros Papageorgiou
 * 
 */
public abstract class HydroBase extends Producer
{
  private static final int DEFAULT_HYDRO_PROFILE_HOURS = 24;
  private static final double WATER_DENSITY = 999.972;
  private static final double G = 9.80665;

  // The input flow graph for every day of the year
  protected Curve inputFlow;
  // The minimum flow needed to operate the turbines
  protected double minFlow;
  // The maximum flow needed to operate the turbines
  protected double maxFlow;
  // The turbine efficiency graph as a function of the percentage of the maxFlow
  protected Curve turbineEfficiency;
  // The plants current stored water volume
  protected double volume;
  // The plants current height difference between the input and output
  protected double height;
  // The plants height losses
  protected double staticLosses;

  /**
   * The only HydroBase constructor.
   * 
   * @param name
   *          the plants name
   * @param inputFlow
   *          the input flow graph from a whole year
   * @param minFlow
   *          the minimal flow needed to operate the turbines
   * @param maxFlow
   *          the maximum flow that the turbines can handle
   * @param turbineEfficiency
   *          the turbine efficiency graph as a function of the
   *          percentage of maximum flow
   * @param initialVolume
   *          the initial dam volume
   * @param initialHeight
   * @param capacity
   *          the plant rated capacity < 0
   * @param staticLosses
   *          the static plant losses < 0
   */
  public HydroBase (String name, Curve inputFlow, double minFlow,
                    double maxFlow, Curve turbineEfficiency,
                    double initialVolume, double initialHeight,
                    double capacity, double staticLosses)
  {
    // no dam production put both on run of the river
    super(name, PowerType.RUN_OF_RIVER_PRODUCTION, DEFAULT_HYDRO_PROFILE_HOURS,
          capacity);
    if (inputFlow == null || minFlow < 0 || maxFlow < minFlow
        || turbineEfficiency == null || initialVolume < 0 || initialHeight < 0
        || staticLosses < 0 || staticLosses > 1)
      throw new IllegalArgumentException();
    this.inputFlow = inputFlow;
    this.minFlow = minFlow;
    this.maxFlow = maxFlow;
    this.turbineEfficiency = turbineEfficiency;
    this.volume = initialVolume;
    this.height = initialHeight;
    this.staticLosses = staticLosses;
  }

  /**
   * Calculate the hydro plants output based on the day of the year
   * 
   * @param day
   *          day of the year
   * @return the energy output in kwh < 0
   */
  protected double getOutput (int day)
  {
    if (day < 0 || day > 366)
      throw new IllegalArgumentException();

    // Calculate the water flow of the turbine based on the one the enters
    // the plant
    double waterFlow = getFlow(inputFlow.value(day));

    // Calculate the turbine efficiency
    double turbEff = turbineEfficiency.value(waterFlow / maxFlow);

    // convert the power into kwh
    double power =
      -getWaterPower(staticLosses, turbEff, waterFlow, height)
              * timeslotLengthInMin / (MINUTES_IN_HOUR * WATT_IN_KILOWATT);

    // Update the facility's volume based on input and output flow
    updateVolume(inputFlow.value(day), waterFlow);
    // Update the facility's height
    updateHeigth();
    if (power > 0 || Double.isInfinite(power) || Double.isNaN(power))
      throw new IllegalStateException("Invalid Power");
    return power;

  }

  /**
   * Calculate the water power that is captured by the turbines
   * and converted to electricity
   * 
   * @param staticLosseCoef
   *          the plants static losses > 0 & <= 1
   * @param turbineEff
   *          the turbine efficiency > 0 & <= 1
   * @param flow
   *          the water flow > 0
   * @param heigth
   *          the height difference between input and output > 0
   * @return
   */
  protected double getWaterPower (double staticLosseCoef, double turbineEff,
                                  double flow, double heigth)
  {
    if (flow >= minFlow && flow <= maxFlow)
      return staticLosseCoef * turbineEff * WATER_DENSITY * G * heigth * flow;
    else if (flow > maxFlow)
      return staticLosseCoef * turbineEff * WATER_DENSITY * G * heigth
             * maxFlow;
    else
      return 0;
  }

  /**
   * This function update the hydro plants volume after one timeslot has passed
   * 
   * @param avInputFlow
   *          input flow
   * @param turbineFlow
   *          turbine or output flow
   */
  protected abstract void updateVolume (double avInputFlow, double turbineFlow);

  /**
   * Update the hydro plant's height difference between input and output
   */
  protected abstract void updateHeigth ();

  /**
   * Get the output/turbine flow
   * 
   * @param avInputFlow
   *          average input flow > 0
   * @return the turbine/output flow > 0
   */
  protected abstract double getFlow (double avInputFlow);

  /**
   * @return the inputFlow
   */
  public Curve getInputFlow ()
  {
    return inputFlow;
  }

  /**
   * @return the minFlow
   */
  public double getMinFlow ()
  {
    return minFlow;
  }

  /**
   * @return the maxFlow
   */
  public double getMaxFlow ()
  {
    return maxFlow;
  }

  /**
   * @return the turbineEfficiency
   */
  public Curve getTurbineEfficiency ()
  {
    return turbineEfficiency;
  }

  /**
   * @return the volume
   */
  public double getVolume ()
  {
    return volume;
  }

  /**
   * @return the height
   */
  public double getHeight ()
  {
    return height;
  }

  /**
   * @param inputFlow
   *          the inputFlow to set
   */
  public void setInputFlow (Curve inputFlow)
  {
    if (inputFlow == null)
      throw new IllegalArgumentException();
    this.inputFlow = inputFlow;
  }

  /**
   * @param minFlow
   *          the minFlow to set
   */
  public void setMinFlow (double minFlow)
  {
    if (minFlow < 0 || minFlow >= maxFlow)
      throw new IllegalArgumentException();
    this.minFlow = minFlow;
  }

  /**
   * @param maxFlow
   *          the maxFlow to set
   */
  public void setMaxFlow (double maxFlow)
  {
    if (maxFlow < 0 || maxFlow <= minFlow)
      throw new IllegalArgumentException();
    this.maxFlow = maxFlow;
  }

  /**
   * @param turbineEfficiency
   *          the turbineEfficiency to set
   */
  public void setTurbineEfficiency (Curve turbineEfficiency)
  {
    if (turbineEfficiency == null)
      throw new IllegalArgumentException();
    this.turbineEfficiency = turbineEfficiency;
  }

  /**
   * @param volume
   *          the volume to set
   */
  public void setVolume (double volume)
  {
    if (volume < 0)
      throw new IllegalArgumentException();
    this.volume = volume;
  }

  /**
   * @param height
   *          the height to set
   */
  public void setHeight (double height)
  {
    if (height < 0)
      throw new IllegalArgumentException();
    this.height = height;
  }
}
