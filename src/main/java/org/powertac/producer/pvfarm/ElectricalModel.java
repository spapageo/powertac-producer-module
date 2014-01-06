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
