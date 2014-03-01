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

import java.util.Calendar;

import static org.powertac.producer.Producer.MILLISECONDS_IN_SECOND;
import static org.powertac.producer.Producer.MINUTES_IN_HOUR;
import static org.powertac.producer.Producer.SECONDS_IN_MINUTE;
import static org.powertac.producer.pvfarm.SolarFarm.*;
/**
 * 
 * This is a helper class that contains functions that help model the sun's
 * position on the sky.
 * 
 * @author Doom
 * 
 */
final class SolarModel
{
  private static final double EOT_CONS1 = 0.258;
  private static final double EOT_CONS2 = 7.416;
  private static final double EOT_CONS3 = 3.648;
  private static final double EOT_CONS4 = 9.228;

  private static final int LOGITUDE_CORRECTION_MULT_CONS = 15;

  private static final int AZIMUTH_MULT_CONS1 = 15;

  private static final int ALTITUDE_CORRECTION_THRESHOLD = 15;

  private static final double ALTITUDE_CORRECTION_CONS1 = 0.00452;
  private static final double ALTITUDE_CORRECTION_CONS2 = 0.1594;
  private static final double ALTITUDE_CORRECTION_CONS3 = 0.0196;
  private static final double ALTITUDE_CORRECTION_CONS4 = 0.00002;
  private static final double ALTITUDE_CORRECTION_CONS5 = 0.505;
  private static final double ALTITUDE_CORRECTION_CONS6 = 0.0845;

  private static final double SUN_DIVERGENCE_CONS_1 = 9.1;
  private static final double SUN_DIVERGENCE_CONS_2 = 5.4;
  private static final double SUN_DIVERGENCE_CONS_3 = 26.0;
  private static final double SUN_DIVERGENCE_CONS_4 = 23.2559;
  private static final double SUN_DIVERGENCE_CONS_5 = 0.3915;
  private static final double SUN_DIVERGENCE_CONS_6 = 0.1764;
  private static final double SUN_DIVERGENCE_CONS_7 = 0.3948;
  
  private static final double FULL_CIRCLE = 360.0;

  private SolarModel ()
  {
    // This should never be called
  }

  /**
   * Calculate the suns azimuth angle during the day
   * 
   * @param sunElevation
   *          the sun altitude angle in degrees
   * @param day
   *          the day of the year between 1-366
   * @param panelLatitude
   *          The panel latitude in degrees
   * @param solartime
   *          The local solar time in hours 0-23.99999
   * @return The suns azimuth angle in degrees
   */
  protected static double getSunAzinuthAngle (double sunElevation, int day,
                                              double panelLatitude,
                                              double solartime)
  {
    // assert(sunElevation >= 0 && sunElevation <= 90);
    assert (day >= 1 && day <= 366);
    assert (panelLatitude >= -90 && panelLatitude <= 90);
    assert (solartime >= 0 && solartime < 24);

    double part1 =
      sin(toRadians(getSunDivergance(day))) * cos(toRadians(panelLatitude));
    double part2 =
      cos(toRadians(getSunDivergance(day))) * sin(toRadians(panelLatitude))
              * cos(toRadians((12 - solartime) * AZIMUTH_MULT_CONS1));
    double res = (part1 - part2) / cos(toRadians(sunElevation));

    //Used to fix rounding errors
    if (res > 1) {
      res = 1;
    }
    else if (res < -1) {
      res = -1;
    }

    if (solartime <= 12)
      // return 180 - toDegrees(acos(res));
      return toDegrees(acos(res));
    else
      // return 180 + toDegrees(acos(res));
      return FULL_CIRCLE - toDegrees(acos(res));
  }

  /**
   * Calculates the suns divergance/declination at solar noon as a function
   * the day of the year
   * 
   * @param day
   *          the day of the year between 1-366
   * @return the declination angle in degrees
   */
  protected static double getSunDivergance (int day)
  {
    // assert (day >= 1 && day <= 366);
    double part1 =
      SUN_DIVERGENCE_CONS_4
              * cos(toRadians(day * FULL_CIRCLE / DAYS_IN_A_YEAR
                              + SUN_DIVERGENCE_CONS_1));
    double part2 =
      SUN_DIVERGENCE_CONS_5
              * cos(toRadians(2 * day * FULL_CIRCLE / DAYS_IN_A_YEAR
                              + SUN_DIVERGENCE_CONS_2));
    double part3 =
      SUN_DIVERGENCE_CONS_6
              * cos(toRadians(3 * day * FULL_CIRCLE / DAYS_IN_A_YEAR
                              + SUN_DIVERGENCE_CONS_3));
    return SUN_DIVERGENCE_CONS_7 - part1 - part2 - part3;
  }

  /**
   * Calculates the a correction to the suns altitude angle due to refraction
   * in the atmosphere
   * 
   * @param altitudeAngle
   * @param pressure
   *          Air pressure in Pa
   * @param temperature
   *          Ambient temperature in Kelvin
   * @return The difference from the given altitude angle
   */
  protected static double getSunAltitudeCorrection (double altitudeAngle,
                                                    double pressure,
                                                    double temperature)
  {
    // assert (altitudeAngle >= 0 && altitudeAngle <= 90);
    // assert (pressure >= 0);
    // assert (temperature >= 0);

    // Since we take inputs in SI units and the equation requires pressure
    // in mbar we have to convert the value
    // 1 bar = 100000 Pa
    double p = pressure / 100.0;

    if (altitudeAngle >= ALTITUDE_CORRECTION_THRESHOLD) {
      return ALTITUDE_CORRECTION_CONS1 * p
             / (temperature * tan(toRadians(altitudeAngle)));
    }
    else {
      double part1 =
        ALTITUDE_CORRECTION_CONS2 + ALTITUDE_CORRECTION_CONS3 * altitudeAngle
                + ALTITUDE_CORRECTION_CONS4 * pow(altitudeAngle, 2);
      double part2 =
        ALTITUDE_CORRECTION_CONS5 * altitudeAngle + ALTITUDE_CORRECTION_CONS6
                * pow(altitudeAngle, 2);
      double part3 = (1 + part2) * temperature;
      return part1 * p / part3;
    }
  }

  /**
   * Calculate the sun angle/altitude angle
   * 
   * @param solartime
   *          The local solar time in hours 0-23.99999
   * @param panelLatitude
   *          The panel latitude in degrees
   * @param day
   *          the day of the year between 1-366
   * 
   * @return The suns altitude angle in degrees
   */
  protected static double getSunAltitudeAngle (double solarTime,
                                               double panelLatitude, int day)
  {
    // assert (day >= 1 && day <= 366);
    // assert (panelLatitude >= -90 && panelLatitude <= 90);
    // assert (solarTime >= 0 && solarTime < 24);

    return toDegrees(asin(cos(toRadians((12 - solarTime) * 15))
                          * cos(toRadians(panelLatitude))
                          * cos(toRadians(getSunDivergance(day)))
                          + sin(toRadians(panelLatitude))
                          * sin(toRadians(getSunDivergance(day)))));
  }

  /**
   * Calculate the incidence angle of the sun rays on a panel
   * 
   * @param sunAltitude
   *          Suns altitude angle in degrees
   * @param sunAzimuth
   *          Suns azimuth angle in degrees
   * @param panelAzimuth
   *          Panels azimuth/facing angle in degrees
   * @param panelAltitude
   *          Panels tilt/altitude angle in degrees
   * @return The incidence angle in degrees
   */
  protected static double getIncidenceAngle (double sunAltitude,
                                             double sunAzimuth,
                                             double panelAzimuth,
                                             double panelAltitude)
  {
    // assert(sunAltitude >= 0 && sunAltitude <= 90);
    // assert (sunAzimuth >= 0 && sunAzimuth <= 360);
    // assert (panelAzimuth >= 0 && panelAzimuth <= 360);
    // assert (panelAltitude >= 0 && panelAltitude <= 90);

    return toDegrees(acos(cos(toRadians(sunAltitude))
                          * sin(toRadians(panelAltitude))
                          * cos(toRadians(panelAzimuth - sunAzimuth))
                          + sin(toRadians(sunAltitude))
                          * cos(toRadians(panelAltitude))));
  }

  /**
   * Calculate the solar time
   * 
   * @param longitude
   *          longitude of the location
   * @param timeZone
   *          time zone offset from UTC
   * @param localTime
   *          local time in milliseconds
   * @return The local solar time in hours from zero to 23.99999999
   */
  protected static double getSolarTime (double longitude, Calendar cal)
  {
     assert (longitude >= -180 && longitude <= 180);

     int timezoneOffset = cal.get(Calendar.ZONE_OFFSET) /
             (MINUTES_IN_HOUR * SECONDS_IN_MINUTE * MILLISECONDS_IN_SECOND);

    int day = cal.get(Calendar.DAY_OF_YEAR);
    double hours = cal.get(Calendar.HOUR_OF_DAY);
    double minutes = cal.get(Calendar.MINUTE);
    double seconds = cal.get(Calendar.SECOND);
    double daylightsavings =
      cal.get(Calendar.DST_OFFSET) / (MILLISECONDS_IN_SECOND * SECONDS_IN_MINUTE
              * MINUTES_IN_HOUR);

    assert (longitude >= -180 && longitude <= 180);

    double hms =
      hours + minutes / MINUTES_IN_HOUR + seconds / (MINUTES_IN_HOUR
              * SECONDS_IN_MINUTE);
    double EOT = equationOfTime(day);

    double correctedLongitude = longitudeCorrection(timezoneOffset, longitude);

    double result = solarTime(hms, daylightsavings, EOT, correctedLongitude);
    if (result < 0.0) {
      return HOURS_IN_DAY + result;
    }
    else if (result > HOURS_IN_DAY) {
      return result - HOURS_IN_DAY;
    }
    else {
      return result;
    }
  }

  private static double equationOfTime (int day)
  {
    double X = toRadians((FULL_CIRCLE * (day - 1)) / DAYS_IN_A_YEAR);
    return EOT_CONS1 * cos(X) - EOT_CONS2 * sin(X) - EOT_CONS3 * cos(2 * X)
           - EOT_CONS4 * sin(2 * X);
  }

  private static double longitudeCorrection (double timezone, double longitude)
  {
    return (LOGITUDE_CORRECTION_MULT_CONS * timezone - longitude)
           / LOGITUDE_CORRECTION_MULT_CONS;
  }

  private static double solarTime (double hms, double dayligthSaving,
                                   double EOT, double correctedLongitude)
  {
    return hms + (EOT / MINUTES_IN_HOUR) - correctedLongitude - dayligthSaving;
  }
}
