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
package org.powertac.producer.pvfarm;

import static java.lang.Math.*;

/**
 * 
 */

/**
 * @author Spyros papageorgiou
 * 
 */
final class ElectricalModel
{
  private ElectricalModel ()
  {
  }

  /**
   * Calculate the panel electrical output
   * 
   * @param referenceConvertionCoeff
   *          the reference efficiency of the panel
   * @param staticLosses
   *          the static losses of the panel
   * @param thermalLossesCoeff
   *          the thermal losses coefficient
   * @param reflectiveLosses
   *          the reflective losses coeeficient
   * @param irradiance
   *          the input irradiance
   * @param panelArrea
   *          the panel arrea
   * @return the power output
   */
  protected static double getElectricalOutput (double referenceConvertionCoeff,
                                               double staticLosses,
                                               double thermalLossesCoeff,
                                               double reflectiveLosses,
                                               double irradiance,
                                               double panelArrea)
  {
    return irradiance * panelArrea * referenceConvertionCoeff * staticLosses
           * thermalLossesCoeff;
  }

  /**
   * calculate the reflective losses
   * 
   * @param incidanceanle
   *          incidance angle
   * @param clearnessindex
   *          clearness index 0.2-0.3
   * @return the reflective losses
   */
  protected static double getReflectiveLossCoeff (double incidanceanle,
                                                  double clearnessindex)
  {
    return (1 - pow(E, -cos(toRadians(incidanceanle)) / clearnessindex))
           / (1 - pow(E, -1 / clearnessindex));
  }

  /**
   * Calculate the panel temperature
   * 
   * @param ambientTemp
   *          The ambient temperature
   * @param windspeed
   *          The windspeed
   * @param irradiance
   *          The input irradiance
   * @return the panel temperature
   */
  protected static double getPanelTemperature (double ambientTemp,
                                               double windspeed,
                                               double irradiance)
  {
    return 0.943 * ambientTemp + 0.028 * irradiance - 1.528 * windspeed + 4.3;
  }

  /**
   * Calculate the thermal losses coefficient
   * 
   * @param b
   * @param Tpanel
   *          panel temperature
   * @param Tref
   *          reference temperatur of the panel
   * @param gamma
   * @param irradiance
   *          input irradiance
   * @return
   */
  protected static double getThermalLossCoeff (double b, double Tpanel,
                                               double Tref)
  {
    assert (b > 0);
    assert (Tpanel > 0);
    assert (Tref > 0);
    if (Tpanel > Tref)
      return 1 - b * (Tpanel - Tref);
    else
      return 1;
  }
}
