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
 * This class models a solar farm consisting of several pv panels. It works 
 * by modeling the suns position to calculate the irradiance on the panel.
 * It also models the effect of clouds on the output.
 * 
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("solar-farm")
public class SolarFarm extends Producer
{

  private static final double CELCIUS_TO_KELVIN_CONS = 273.15;
  private static final double SOLAR_DEFAULT_COST_PER_KWH = 0.14;
  private static final int SOLAR_DEFAULT_PROFILE_HOURS = 24;

  @XStreamImplicit
  List<PvPanel> panelList = new ArrayList<PvPanel>();

  /**
   * @param name
   * @param powerType
   * @param profileHours
   * @param capacity
   */
  public SolarFarm ()
  {
    super("Solar farm", PowerType.SOLAR_PRODUCTION,
          SOLAR_DEFAULT_PROFILE_HOURS, 0);
    this.costPerKwh = SOLAR_DEFAULT_COST_PER_KWH;
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
        panel.getOutput(systemTime,
                        timezone,
                        weatherReport.getCloudCover(),
                        // FIX for celcius to kelvin
                        weatherReport.getTemperature() + CELCIUS_TO_KELVIN_CONS,
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
                        weatherForecastPrediction.getTemperature()
                                + CELCIUS_TO_KELVIN_CONS,
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
    initialize(name, PowerType.FOSSIL_PRODUCTION, SOLAR_DEFAULT_PROFILE_HOURS, upperPowerCap,
               IdGenerator.createId());
    for (PvPanel panel: panelList) {
      panel.setTimeslotLengthInMin(timeslotLengthInMin);
    }
    return this;
  }

}
