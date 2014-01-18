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

import static java.lang.Math.*;

/**
 * Helper class that provides static functions related to the electrical model
 * of pv panel.
 * 
 * @author Spyros papageorgiou
 * 
 */
final class ElectricalModel
{
  private static final double PANEL_TEMPERATURE_MODEL_COEFF_4 = 4.3;
  private static final double PANEL_TEMPERATURE_MODEL_COEFF_3 = 1.528;
  private static final double PANEL_TEMPERATURE_MODEL_COEFF_2 = 0.028;
  private static final double PANEL_TEMPERATURE_MODEL_COEFF_1 = 0.943;

  private ElectricalModel ()
  {
    // Should't be called ever
  }

  /**
   * Calculate the panel electrical output
   * 
   * @param referenceConvertionCoeff
   *          the reference efficiency of the panel > 0 & < 1
   * @param staticLosses
   *          the static losses of the panel > 0 & < 1
   * @param thermalLossesCoeff
   *          the thermal losses coefficient > 0 & < 1
   * @param reflectiveLosses
   *          the reflective losses coefficient > 0 & < 1
   * @param irradiance
   *          the input irradiance in w/m^2
   * @param panelArrea
   *          the panel arrea in m^2
   * @return the power output >= 0 in watt
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
   * Calculates the reflective losses
   * 
   * @param incidenceAngle
   *          Incidence angle
   * @param clearnessindex
   *          clearness index 0.2-0.3
   * @return the reflective losses > 0 & <= 1
   */
  protected static double getReflectiveLossCoeff (double incidenceAngle,
                                                  double clearnessindex)
  {
    return (1 - pow(E, -cos(toRadians(incidenceAngle)) / clearnessindex))
           / (1 - pow(E, -1 / clearnessindex));
  }

  /**
   * Calculate the panel temperature
   * 
   * @param ambientTemp
   *          The ambient temperature
   * @param windspeed
   *          The wind speed
   * @param irradiance
   *          The input irradiance
   * @return the panel temperature
   */
  protected static double getPanelTemperature (double ambientTemp,
                                               double windspeed,
                                               double irradiance)
  {
    return PANEL_TEMPERATURE_MODEL_COEFF_1 * ambientTemp
           + PANEL_TEMPERATURE_MODEL_COEFF_2 * irradiance
           - PANEL_TEMPERATURE_MODEL_COEFF_3 * windspeed
           + PANEL_TEMPERATURE_MODEL_COEFF_4;
  }

  /**
   * Calculate the thermal losses coefficient
   * 
   * @param b
   *          the rate at which the extra temperature degrades efficiency
   * @param Tpanel
   *          panel temperature in Kelvin
   * @param Tref
   *          reference temperature of the panel in Kelvin
   * @param irradiance
   *          input irradiance in watt/m^2
   * @return the losses coefficient > 0 & <= 1
   */
  protected static double getThermalLossCoeff (double b, double Tpanel,
                                               double Tref)
  {
    if (Tpanel > Tref)
      return 1 - b * (Tpanel - Tref);
    else
      return 1;
  }
}
