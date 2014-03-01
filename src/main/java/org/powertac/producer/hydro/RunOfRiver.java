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

import org.powertac.common.IdGenerator;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.producer.utils.Curve;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * This models a run of the river plant with virtually no capacity and static
 * height.
 * 
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("run-of-the-river")
public class RunOfRiver extends HydroBase
{

  private static final double DEFAULT_RUNOFRIVER_COST_PER_KWH = 0.08;
  private static final int DEFAULT_RUNOFRIVER_PROFILE_HOURS = 24;

  /**
   * The only {@link RunOfRiver} constructor. It constructs an instance with
   * the following argument.
   * 
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
  public RunOfRiver (Curve inputFlow, double minFlow, double maxFlow,
                     Curve turbineEfficiency, double initialVolume,
                     double initialHeight, double staticLosses, double capacity)
  {
    super("Run of the river hydro plant", inputFlow, minFlow, maxFlow,
          turbineEfficiency, initialVolume, initialHeight, capacity,
          staticLosses);
    this.costPerKwh = DEFAULT_RUNOFRIVER_COST_PER_KWH;
  }

  @Override
  protected void updateVolume (double avInputFlow, double computedFlow)
  {
    // no need to do anything the volume is static
  }

  @Override
  protected void updateHeigth ()
  {
    // no need to do anything the height is static
  }

  @Override
  protected double getFlow (double avInputFlow)
  {
    // All the flow passes through to the turbines
    return avInputFlow;
  }

  @Override
  public double getOutput (WeatherReport weatherReport)
  {
    return getOutput(this.timeService.getCurrentDateTime().getDayOfYear());
  }

  @Override
  public double
    getOutput (int timeslotIndex,
               WeatherForecastPrediction weatherForecastPrediction,
               double previousOutput)
  {
    return getOutput(this.timeslotRepo.getTimeForIndex(timeslotIndex)
            .toDateTime().getDayOfYear());
  }

  /**
   * This function is called after de-serialization
   */
  protected Object readResolve ()
  {
    this.name = "Run of the river hydro plant";
    initialize(name, PowerType.RUN_OF_RIVER_PRODUCTION,
               DEFAULT_RUNOFRIVER_PROFILE_HOURS, upperPowerCap,
               IdGenerator.createId());
    return this;
  }
}
