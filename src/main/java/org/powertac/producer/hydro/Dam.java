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
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * This class models a hydro dam. It uses a volume-height graph to calculate
 * the height of the dam. An an inverse power and efficiency curve the get the
 * preferred output.
 * 
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("dam")
public class Dam extends HydroBase
{
  private static final int UNIT_HEIGHT = 1;
  private static final int DEFAULT_DAM_MODELLING_DURATION = 24;
  private static final double DEFAULT_DAM_COST_PER_KWH = 0.03;

  // a volume-height graph volumeHeight.value(volume) = height
  private Curve volumeHeight;
  @XStreamOmitField
  private Curve invCurveOut;

  /**
   * Construct a {@link Dam} with the below arguments.
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
   * @param volumeHeigth
   *          the volume heigth-graph
   * @param initialVolume
   *          the initial dam volume
   * @param capacity
   *          the plant rated capacity < 0
   * @param staticLosses
   *          the static plant losses < 0
   */
  public Dam (Curve inputFlow, double minFlow, double maxFlow,
              Curve turbineEfficiency, Curve volumeHeigth,
              double initialVolume, double capacity, double staticLosses)
  {
    super("Dam", inputFlow, minFlow, maxFlow, turbineEfficiency, initialVolume,
          volumeHeigth.value(initialVolume), capacity, staticLosses);
    this.volumeHeight = volumeHeigth;
    this.costPerKwh = DEFAULT_DAM_COST_PER_KWH;

    calculateInvOut();
  }

  /**
   * Calculate the inverse output graph for unit height so that we know for
   * which flow we get the output required.
   */
  protected void calculateInvOut ()
  {
    Curve c = new Curve();
    // calculate inverse curve output function for unit height
    for (double flow = minFlow; flow <= maxFlow; flow +=
      STEP * (maxFlow - minFlow)) {
      double pow =
        getWaterPower(this.staticLosses,
                      this.turbineEfficiency.value(flow / maxFlow), flow,
                      UNIT_HEIGHT)
                * timeslotLengthInMin
                / (WATT_IN_KILOWATT * MINUTES_IN_HOUR);
      c.add(flow, pow);
    }

    invCurveOut = c.getInvertiblePart();
  }

  @Override
  protected void updateVolume (double avarageinputFlow, double computedFlow)
  {
    this.volume +=
      (avarageinputFlow - computedFlow) * timeslotLengthInMin
              * SECONDS_IN_MINUTE;
  }

  @Override
  protected double getFlow (double avarageInputFlow)
  {
    if (height != 0)
      return invCurveOut.value(-preferredOutput * MINUTES_IN_HOUR
                               / (height * timeslotLengthInMin));
    else
      return avarageInputFlow;
  }

  @Override
  protected void updateHeigth ()
  {
    height = volumeHeight.value(volume);
  }

  @Override
  protected double getOutput (WeatherReport weatherReport)
  {
    return getOutput(this.timeService.getCurrentDateTime().getDayOfYear());
  }

  @Override
  protected double
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
    this.name = "Dam";
    initialize(name, PowerType.RUN_OF_RIVER_PRODUCTION,
               DEFAULT_DAM_MODELLING_DURATION, upperPowerCap,
               IdGenerator.createId());
    calculateInvOut();
    return this;
  }

  /**
   * @return the volumeHeight
   */
  public Curve getVolumeHeight ()
  {
    return volumeHeight;
  }

  /**
   * @return the invCurveOut
   */
  public Curve getInvCurveOut ()
  {
    return invCurveOut;
  }

  /**
   * @param volumeHeight
   *          the volumeHeight to set
   */
  public void setVolumeHeight (Curve volumeHeight)
  {
    if (volumeHeight == null)
      throw new IllegalArgumentException();
    this.volumeHeight = volumeHeight;
  }
}
