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

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import static org.powertac.producer.pvfarm.SolarFarm.*;

/**
 * A helper class that contains the functions needed to model the irradiance
 * that reaches a pv panel
 * 
 * @author Spyros Papageorgiou
 * 
 */
final class IrradianceModel
{

  private static final double VAPOR_ABSORBTION_CONS_1 = 0.29;
  private static final double VAPOR_ABSORBTION_CONS_2 = 14.15;
  private static final double VAPOR_ABSORBTION_CONS_3 = 0.635;
  private static final double VAPOR_ABSORBTION_CONS_4 = 0.5925;

  private static final double COMPRESSD_VAPOR_CONS_1 = 4.93;
  private static final double COMPRESSD_VAPOR_CONS_2 = 26.23;
  private static final double COMPRESSD_VAPOR_CONS_3 = 5416.0;

  private static final double IRRADIANCE_CONSTANT_COEFF = 0.033;
  private static final double SPACE_MEAN_IRRADIANCE = 1367.7;

  private static final double AIRMASS_CALC_CONST_1 = 0.50572;
  private static final double AIRMASS_CALC_CONST_2 = 6.07995;
  private static final double AIRMASS_CALC_CONST_3 = -1.6354;

  private static final double ATMOSPHERE_ALBEDO_RATE = 0.00492;
  private static final double ATMOSPHERE_ALBEDO_CONST = 0.068;

  private static final double OZONE_ABSORBTION_CONS_1 = 0.1082;
  private static final double OZONE_ABSORBTION_CONS_2 = 13.86;
  private static final double OZONE_ABSORBTION_CONS_3 = 0.805;
  private static final double OZONE_ABSORBTION_CONS_4 = 0.00658;
  private static final double OZONE_ABSORBTION_CONS_5 = 10.36;
  private static final int OZONE_ABSORBTION_CONS_6 = 3;
  private static final double OZONE_ABSORBTION_CONS_7 = 0.00218;
  private static final double OZONE_ABSORBTION_CONS_8 = 0.0042;
  private static final double OZONE_ABSORBTION_CONS_9 = 3.23;
  private static final int OZONE_ABSORBTION_CONS_10 = 10;
  private static final int OZONE_ABSORBTION_CONS_11 = -6;
  private static final int OZONE_ABSORBTION_CONS_12 = 2;

  private static final double SCATTER_RATION_CONS = 0.0043;

  private static double[] airmassValues = { 0.5, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0,
                                           3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0,
                                           10.0, 30.0, 40 };
  private static double[] RayleighScatterValues = { 0.9385, 0.8973, 0.8830,
                                                   0.8696, 0.8572, 0.8455,
                                                   0.8344, 0.7872, 0.7673,
                                                   0.7493, 0.7328, 0.7177,
                                                   0.7037, 0.6907, 0.6108,
                                                   0.4364, 0.41 };

  static private PolynomialSplineFunction spline = new SplineInterpolator()
          .interpolate(airmassValues, RayleighScatterValues);

  private IrradianceModel ()
  {
    // This should never be called
  }

  /**
   * Calculate sun direct irradiance
   * 
   * @param sunaltitude
   *          sun altitude angle in degrees < 90 & > -90
   * @param solarconstant
   *          solar irradiance constant in watt/m^2 > 0
   * @param T0
   *          the absorption coefficient of the ozone laye > 0
   * @param Tr
   *          the absorption coefficient due to the Rayleigh scattering > 0
   * @param aw
   *          the absorption coefficient of the water vapor > 0
   * @param Ta
   *          the absorption coefficient of aerosols > 0
   * @return sun direct irradiance in watt/m^2
   */
  protected static double getDirectIrradiance (double sunaltitude,
                                               double solarconstant, double T0,
                                               double Tr, double aw, double Ta)
  {

    return solarconstant * cos(toRadians(90 - sunaltitude)) * ((T0 * Tr) - aw)
           * Ta;
  }

  /**
   * Calculate sun diffuse irradiance
   * 
   * @param sunaltitude
   *          sun altitude angle in degrees
   * @param solarconstant
   *          solar irradiance constant
   * @param T0
   *          the absorption coefficient of the ozone laye
   * @param Tr
   *          the absorption coefficient due to the Rayleigh scattering
   * @param aw
   *          the absorption coefficient of the water vapor
   * @param Ta
   *          the absorption coefficient of aerosols
   * @param w0
   *          the aerosol albedo
   * @param f
   *          the ration of forward to total scattering
   * @return sun diffusion irradiance in watt/m^2
   */
  protected static double getDiffuseIrradiance (double sunaltitude,
                                                double solarconstant,
                                                double T0, double Tr,
                                                double aw, double Ta,
                                                double w0, double f)
  {
    assert (sunaltitude >= 0 && sunaltitude <= 90);
    assert (T0 >= 0 && T0 <= 1);
    assert (Tr >= 0 && Tr <= 1);
    assert (aw >= 0 && aw <= 1);
    assert (Ta >= 0 && Ta <= 1);
    assert (w0 >= 0 && w0 <= 1);
    assert (f >= 0 && f <= 1);
    assert (solarconstant > 0);

    double dr =
      solarconstant * cos(toRadians(90 - sunaltitude)) * T0 * (1 - Tr) / 2;
    double da =
      solarconstant * cos(toRadians(90 - sunaltitude)) * (T0 * Tr - aw)
              * (1 - Ta) * w0 * f;
    return dr + da;

  }

  /**
   * Calculate the cloud modified irradiance
   * 
   * @param irradiance
   *          input irradiance
   * @param cloudCoverage
   *          percent of cloud cover >= 0 & <=1
   * @param groundAlbedo
   *          ground reflective coefficient > 0
   * @param p
   *          cloud coefficient
   * @param q
   *          cloud coefficient
   * @param r
   *          cloud coefficient
   * @param s
   *          cloud coefficient
   * @param m
   *          cloud coefficient
   * @return irradiance in watt/m^2
   */
  protected static double getCloudModifiedIrradiance (double irradiance,
                                                      double cloudCoverage,
                                                      double groundAlbedo,
                                                      double p, double q,
                                                      double r, double s,
                                                      double m)
  {
    assert (irradiance >= 0);
    assert (cloudCoverage >= 0 && cloudCoverage <= 1);
    assert (groundAlbedo >= 0);

    double tC;
    if (m != 0) {
      tC =
        p + q * cloudCoverage + r * pow(cloudCoverage, 2) + s
                * pow(cloudCoverage, m);
    }
    else {
      tC = p + q * cloudCoverage + r * pow(cloudCoverage, 2);
    }

    double atmosphereAlbedo =
      ATMOSPHERE_ALBEDO_CONST + ATMOSPHERE_ALBEDO_RATE * cloudCoverage;
    return irradiance * tC / (1 - groundAlbedo * atmosphereAlbedo);
  }

  /**
   * Calculate the irradiance on a titled plane
   * 
   * @param directirradiance
   *          the sun's direct irradiance
   * @param diffuseirradiance
   *          the sun's diffuse irradiance
   * @param incidenceAngle
   *          the direct irrradiance incidence angle in degrees
   * @param sunAltitude
   *          the sun's altitude angle in degrees
   * @param panelAltitude
   *          the panel tilt angle in degrees
   * @param cloudCover
   *          the percentage of cloud cover
   * @return the perceived irradiance in W/m^2
   */
  protected static double getIrradiancOnTiltedPlane (double directirradiance,
                                                     double diffuseirradiance,
                                                     double incidenceAngle,
                                                     double sunAltitude,
                                                     double panelAltitude,
                                                     double groundalbedo)
  {
    assert (sunAltitude >= 0 && sunAltitude <= 90);
    assert (directirradiance >= 0);
    assert (diffuseirradiance >= 0);
    assert (incidenceAngle >= 0 && sunAltitude <= 90);
    assert (panelAltitude >= 0 && panelAltitude <= 90);
    assert (groundalbedo >= 0);

    return directirradiance * cos(toRadians(incidenceAngle))
           / cos(toRadians(90 - sunAltitude)) + diffuseirradiance
           * (1 + cos(toRadians(panelAltitude))) / 2
           + (directirradiance + diffuseirradiance)
           * (1 - cos(toRadians(panelAltitude))) * (groundalbedo) / 2;
  }

  /**
   * Calculate the irradiance constant based on the day of the year
   * 
   * @param day
   *          The day of the year 1-366
   * @return The solar constant in W/m^2
   */
  protected static double getIrradianceConstant (int day)
  {
    return SPACE_MEAN_IRRADIANCE
           * (1 + IRRADIANCE_CONSTANT_COEFF
                  * cos(toRadians(2.0 * PI * day / DAYS_IN_A_YEAR)));
  }

  /**
   * Calculate the air mass
   * 
   * @param sunAltitudeAngle
   *          the suns altitutde angle in degrees
   * @return the air mass
   */
  protected static double getAirMass (double sunAltitudeAngle)
  {
    assert (sunAltitudeAngle >= 0 && sunAltitudeAngle <= 90);
    return 1 / (cos(toRadians(90 - sunAltitudeAngle)) + AIRMASS_CALC_CONST_1
                                                        * pow(AIRMASS_CALC_CONST_2
                                                                      + sunAltitudeAngle,
                                                              AIRMASS_CALC_CONST_3));
  }

  /**
   * Calculate the absorption coefficient of the water vapor
   * 
   * @param airmass
   *          The relative path length of the sun rays
   * @param humidity
   *          relative humidity <= 1
   * @param temperature
   *          temperature in kelvin
   * @return the absorption coefficient <= 1
   */
  protected static double getaw (double airmass, double humidity,
                                 double temperature)
  {
    assert (airmass >= 0.9 && airmass <= 40);
    assert (humidity >= 0 && humidity <= 1);
    assert (temperature >= 0);

    // this is the compressed depth of the water vapor in the atmosphere
    double part1 =
      pow(E, COMPRESSD_VAPOR_CONS_2 - COMPRESSD_VAPOR_CONS_3 / temperature);

    double xw =
      airmass * COMPRESSD_VAPOR_CONS_1 * humidity * part1 / temperature;

    double part2 =
      pow(1 + VAPOR_ABSORBTION_CONS_2 * xw, VAPOR_ABSORBTION_CONS_3);

    return VAPOR_ABSORBTION_CONS_1 * xw
           / (part2 + VAPOR_ABSORBTION_CONS_4 * xw);
  }

  /**
   * Calculate the absorption coefficient of the ozone layer
   * 
   * @param airmass
   *          The relative path length of the sun rays
   * @param m0
   *          the compressed height of the ozone layer in mm
   * @return the absorption coefficient <= 1
   */
  protected static double getT0 (double airmass, double m0)
  {
    assert (m0 >= 1 && m0 <= 4);
    assert (airmass >= 0.9 && airmass <= 40);
    double x0 = airmass * m0;

    double part1 =
      OZONE_ABSORBTION_CONS_1
              * x0
              / (1 + OZONE_ABSORBTION_CONS_2 * pow(x0, OZONE_ABSORBTION_CONS_3));
    double part2 =
      OZONE_ABSORBTION_CONS_4
              * x0
              / (1 + pow(OZONE_ABSORBTION_CONS_5 * x0, OZONE_ABSORBTION_CONS_6));
    double part3 =
      OZONE_ABSORBTION_CONS_7
              / (1 + OZONE_ABSORBTION_CONS_8 * x0 + OZONE_ABSORBTION_CONS_9
                                                    * pow(OZONE_ABSORBTION_CONS_10,
                                                          OZONE_ABSORBTION_CONS_11)
                                                    * pow(x0,
                                                          OZONE_ABSORBTION_CONS_12));
    // the absorption coefficient
    double a0 = part1 + part2 + part3;

    return 1 - a0;
  }

  /**
   * Calculate the absorption coefficient due to the Rayleigh scattering
   * 
   * @param airmass
   *          The relative path length of the sun rays
   * @return the absorption coefficient <= 1
   */
  protected static double getTr (double airmass)
  {
    assert (airmass >= 0.9 && airmass <= 40);
    return spline.value(airmass);
  }

  /**
   * Calculate the ratio of forward to total scattering
   * 
   * @param sunaltitude
   *          the sun altitude angle in degrees
   * @return the ratio <= 1
   */
  protected static double getf (double sunaltitude)
  {
    assert (sunaltitude >= 0 && sunaltitude <= 90);
    return 1 - SCATTER_RATION_CONS * (90.0 - sunaltitude);
  }
}
