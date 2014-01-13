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
package org.powertac.producer.hydro;

import org.powertac.common.IdGenerator;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.producer.utils.Curve;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("run-of-the-river")
public class RunOfRiver extends HydroBase
{

  public RunOfRiver (Curve inputFlow, double minFlow, double maxFlow,
                     Curve turbineEfficiency, double initialVolume,
                     double initialHeight, double staticLosses, double capacity)
  {
    super("Run of the river hydro plant", inputFlow, minFlow, maxFlow,
          turbineEfficiency, initialVolume, initialHeight, capacity,
          staticLosses);
    this.costPerKwh = 0.08;
  }

  @Override
  protected void updateVolume (double avInputFlow, double computedFlow)
  {
    // no need to do anything
  }

  @Override
  protected void updateHeigth ()
  {
    // no need to do anything
  }

  @Override
  protected double getFlow (double avInputFlow)
  {
    return avInputFlow;
  }

  @Override
  protected double getOutput (WeatherReport weatherReport)
  {
    return getOutput(this.timeService.getCurrentDateTime().getDayOfYear());
  }

  @Override
  protected double
    getOutput (int timeslotIndex,
               WeatherForecastPrediction weatherForecastPrediction)
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
    initialize(name, PowerType.RUN_OF_RIVER_PRODUCTION, 24, upperPowerCap,
               IdGenerator.createId());
    return this;
  }
}
