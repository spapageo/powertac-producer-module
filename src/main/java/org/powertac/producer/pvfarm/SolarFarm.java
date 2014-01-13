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
package org.powertac.producer.pvfarm;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

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
@XStreamAlias("solar-farm")
public class SolarFarm extends Producer
{

  @XStreamImplicit
  List<PvPanel> panelList = new ArrayList<>();

  /**
   * @param name
   * @param powerType
   * @param profileHours
   * @param capacity
   */
  public SolarFarm ()
  {
    super("Solar farm", PowerType.SOLAR_PRODUCTION, 24, 0);
    this.costPerKwh = 0.14;
  }

  /**
   * Add a panel to this farm
   * 
   * @param panel
   */
  public void addPanel (PvPanel panel)
  {
    if (panel == null)
      return;
    panelList.add(panel);
    upperPowerCap += panel.getCapacity();
    panel.setTimeslotLengthInMin(timeslotLengthInMin);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.powertac.producer.Producer#getOutput(org.powertac.common.WeatherReport
   * )
   */
  @Override
  protected double getOutput (WeatherReport weatherReport)
  {
    double powerSum = 0;
    long systemTime =
      timeslotRepo.getTimeForIndex(weatherReport.getTimeslotIndex())
              .getMillis();
    TimeZone timezone =
      timeslotRepo.getTimeForIndex(weatherReport.getTimeslotIndex()).getZone()
              .toTimeZone();
    for (PvPanel panel: panelList) {
      powerSum +=
        panel.getOutput(systemTime, timezone, weatherReport.getCloudCover(),
                        // FIX for celcius to kelvin
                        weatherReport.getTemperature() + 273.15,
                        weatherReport.getWindSpeed());
    }
    if (Double.isInfinite(powerSum) || Double.isNaN(powerSum))
      throw new IllegalStateException("Power produced isn't a number");
    return powerSum;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.powertac.producer.Producer#getOutput(int,
   * org.powertac.common.WeatherForecastPrediction)
   */
  @Override
  protected double
    getOutput (int timeslotIndex,
               WeatherForecastPrediction weatherForecastPrediction,
               double previousOutput)
  {
    double powerSum = 0;
    long systemTime = timeslotRepo.getTimeForIndex(timeslotIndex).getMillis();
    TimeZone timezone =
      timeslotRepo.getTimeForIndex(timeslotIndex).getZone().toTimeZone();
    for (PvPanel panel: panelList) {
      powerSum +=
        panel.getOutput(systemTime, timezone,
                        weatherForecastPrediction.getCloudCover(),
                        // FIX for celcius to kelvin
                        weatherForecastPrediction.getTemperature() + 273.15,
                        weatherForecastPrediction.getWindSpeed());
    }
    return powerSum;
  }

  /**
   * This function is called after de-serialization
   */
  protected Object readResolve ()
  {
    this.name = "Solar farm";
    initialize(name, PowerType.FOSSIL_PRODUCTION, 24, upperPowerCap,
               IdGenerator.createId());
    for (PvPanel panel: panelList) {
      panel.setTimeslotLengthInMin(timeslotLengthInMin);
    }
    return this;
  }

}
