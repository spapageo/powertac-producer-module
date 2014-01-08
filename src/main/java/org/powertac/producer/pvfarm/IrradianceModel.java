/**
 * 
 */
package org.powertac.producer.pvfarm;

import static java.lang.Math.*;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * @author Spyros Papageorgiou
 * 
 */
final class IrradianceModel
{
  private IrradianceModel ()
  {
  }

  static private PolynomialSplineFunction spline = null;

  /**
   * Calculate sun direct irradiance
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
   * @return sun direct irradiance
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
   * @return sun diffusion irradiance
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
   *          percent of cloud cover
   * @param groundAlbedo
   *          ground reflective coefficient
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
   * @return
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

    return irradiance * tC
           / (1 - groundAlbedo * (0.068 + 0.00492 * cloudCoverage));
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
    return 1367.7 * (1 + 0.033 * cos(toRadians(2.0 * PI * day / 365.0)));
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
    return 1 / (cos(toRadians(90 - sunAltitudeAngle)) + 0.50572 * pow(6.07995 + sunAltitudeAngle,
                                                                      -1.6354));
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

    double xw =
      airmass * 4.93 * humidity * pow(E, 26.23 - 5416.0 / temperature)
              / temperature;
    return 0.29 * xw / (pow(1 + 14.15 * xw, 0.635) + 0.5925 * xw);
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

    double a0 =
      0.1082 * x0 / (1 + 13.86 * pow(x0, 0.805)) + 0.00658 * x0
              / (1 + pow(10.36 * x0, 3)) + 0.00218
              / (1 + 0.0042 * x0 + 3.23 * pow(10, -6) * pow(x0, 2));

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
    if (spline != null) {
      return spline.value(airmass);
    }
    else {
      double[] am =
        { 0.5, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0,
         10.0, 30.0, 40 };
      double[] Tr =
        { 0.9385, 0.8973, 0.8830, 0.8696, 0.8572, 0.8455, 0.8344, 0.7872,
         0.7673, 0.7493, 0.7328, 0.7177, 0.7037, 0.6907, 0.6108, 0.4364, 0.41 };

      spline = new SplineInterpolator().interpolate(am, Tr);

      return spline.value(airmass);

    }
    // return 0.9768 - 0.0874*airmass + 0.010607552 * pow(airmass,2) -
    // 8.46205 * pow(10,-4) * pow(airmass,3) + 3.57246 * pow(10,-5) *
    // pow(airmass,4)
    // - 6.0176*pow(10,-7)*pow(airmass,5);
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
    return 1 - 0.0043 * (90.0 - sunaltitude);
  }
}
