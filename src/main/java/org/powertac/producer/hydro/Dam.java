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
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("dam")
public class Dam extends HydroBase
{

  private Curve volumeHeight;
  @XStreamOmitField
  private Curve invCurveOut;

  public Dam (Curve inputFlow, double minFlow, double maxFlow,
              Curve turbineEfficiency, Curve volumeHeigth,
              double initialVolume, double capacity, double staticLosses)
  {
    super("Dam", inputFlow, minFlow, maxFlow, turbineEfficiency, initialVolume,
          volumeHeigth.value(initialVolume), capacity, staticLosses);
    this.volumeHeight = volumeHeigth;
    this.costPerKwh = 0.03;
    calculateInvOut();
  }

  protected void calculateInvOut ()
  {
    Curve c = new Curve();
    // calculate inverse curve output function for unit height
    for (double flow = minFlow; flow <= maxFlow; flow +=
      (maxFlow - minFlow) / 10) {
      double pow =
        getWaterPower(this.staticLosses,
                      this.turbineEfficiency.value(flow / maxFlow), flow, 1)
                * timeslotLengthInMin / (1000 * 60);
      c.add(flow, pow);
    }

    invCurveOut = c.getInvertiblePart();
  }

  @Override
  protected void updateVolume (double avarageinputFlow, double computedFlow)
  {
    this.volume += (avarageinputFlow - computedFlow) * timeslotLengthInMin * 60;
  }

  @Override
  protected double getFlow (double avarageInputFlow)
  {
    if (height != 0)
      return invCurveOut.value(-preferredOutput / height);
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
    this.name = "Dam";
    initialize(name, PowerType.RUN_OF_RIVER_PRODUCTION, 24, upperPowerCap,
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
