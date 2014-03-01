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
package org.powertac.producer.windfarm;

import org.powertac.common.IdGenerator;
import org.powertac.common.RandomSeed;
import org.powertac.producer.utils.Curve;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import static java.lang.Math.*;
import static org.powertac.producer.windfarm.WindFarm.*;

/**
 * Models a wind turbine using its power curve. Also, it models wind shear and
 * turbulence deviation.
 * 
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("turbine")
public class WindTurbine
{
  private static final double EARTH_ROTATIONAL_SPEED = 7.2 * pow(10, -5);
  private static final double UASTERISK_CONS = 34.5;
  // The air density at sea level at 15 C
  private static double STANDARD_AIR_DENSITY = 1.225;
  private static final double DEFAULT_REFERENCE_ALTITUDE = 10;
  private static final double WIND_SHEAR_DEFAULT_KAPPA = 0.4;

  // The turbines latitude in degrees
  private double latitude;
  // The altitude for which the input speed is given
  private double refAltitude = DEFAULT_REFERENCE_ALTITUDE;
  // The roughness of the surface near the turbine
  private double surfaceRoughness;
  // Used to calculate the wind shear effect
  private double kappa = WIND_SHEAR_DEFAULT_KAPPA;
  // The maximum/rated output < 0
  private double ratedOutput;
  // The hub height
  private double hubHeigth;
  // The power curve for 15 C at sea level
  private Curve powerCurve;

  @XStreamOmitField
  private RandomSeed rs;
  @XStreamOmitField
  private int timeslotLengthInMin = MINUTES_IN_HOUR;

  /**
   * Construct a wind turbine from the given arguments.
   * 
   * @param latitude
   *          the turbine's latitude in degrees
   * @param surfaceRoughness
   *          the surface roughness 0~2
   * @param ratedOutput
   *          the rated output
   * @param hubHeigth
   *          the height of the turbine hub
   * @param powerCurve
   *          the power curve of the turbine,
   *          must contain only negative power values
   */
  public WindTurbine (double latitude, double surfaceRoughness,
                      double ratedOutput, double hubHeigth, Curve powerCurve)
  {
    if (ratedOutput >= 0 || latitude < -90 || latitude > 90
        || surfaceRoughness <= 0)
      throw new IllegalArgumentException();
    this.surfaceRoughness = surfaceRoughness;
    this.ratedOutput = ratedOutput;
    this.hubHeigth = hubHeigth;
    this.powerCurve = powerCurve;
    this.latitude = latitude;
  }

  /**
   * Calculate the power output of this turbine
   * 
   * @param temperature
   *          the ambient temperature
   * @param avrHourlyWindSpeed
   *          the average hourly wind speed
   * @return
   */
  public double getPowerOutput (double temperature, double avrHourlyWindSpeed)
  {
    if (rs == null)
      rs =
        new RandomSeed("Wind turbine" + IdGenerator.createId(), 0, "Simulation");

    double sumPowerOutput = 0;

    // Get the wind speed at the height of the turbine hub
    double f = calulcatef(latitude);
    double ua =
      calculateUasterisk(avrHourlyWindSpeed, refAltitude, f, surfaceRoughness,
                         kappa);
    // ua can't be negative since it represents, wind speed
    if (ua < 0)
      ua = 0;
    double correctedHourlySpeed =
      calculateWindAtAltitude(hubHeigth, surfaceRoughness, ua, f, kappa);

    double std;

    // change the std formula for low speed values
    if (correctedHourlySpeed > 1)
      std = calculateStd(f, ua, hubHeigth, surfaceRoughness);
    else
      std = 0.1 * correctedHourlySpeed;

    for (int i = 0; i < timeslotLengthInMin; i++) {
      sumPowerOutput +=
        calculateAirDensity(temperature, hubHeigth)
                * powerCurve.value(sampleGaussian(std, correctedHourlySpeed))
                / STANDARD_AIR_DENSITY;
    }

    return sumPowerOutput / MINUTES_IN_HOUR;
  }

  /**
   * Return a random number from a normal distribution with the std and mean
   * that are given
   * 
   * @param std
   * @param mean
   * @return
   */
  public double sampleGaussian (double std, double mean)
  {
    return abs(rs.nextGaussian() * std + mean);
  }

  /**
   * Calculates the turbulence standard deviation
   * 
   * @param f
   * @param ua
   * @param altitude
   * @param z0
   * @return
   */
  protected static double calculateStd (double f, double ua, double altitude,
                                        double z0)
  {

    double h = 1 - 6 * f * altitude / ua;
    double p = pow(h, 16);

    double std =
      7.5 * h * pow((0.538 + 0.09 * log(altitude / z0)), p) * ua
              / (1 + 0.156 * log(ua / (f * z0)));
    return std;
  }

  /**
   * Calculate the air density as a function of height and temperature
   * 
   * @param temperature
   * @param altitude
   * @return
   */
  protected double calculateAirDensity (double temperature, double altitude)
  {
    // po (1-Lh/T0)^ (gM/RL)

    // standard air pressure
    double p0 = 101325;
    // rate of temperature decrease with height
    double L = 0.0065;
    // gravity accelaration
    double g = 9.80665;
    // Air specific mass
    double M = 0.0289644;
    // noble gas constant
    double R = 8.31447;
    // reference temperature in kelvin
    double T0 = 288.15;
    // calculate pressure as a function of altitude
    double p = p0 * pow(1 - (L * altitude) / T0, (g * M) / (R * L));
    // calculate the air density from the pressure and the temperature
    return (p * M) / (R * temperature);
  }

  /**
   * Calculate the wind speed at a given altitude
   * 
   * @param newAltitude
   *          the altitude at which to calculate the speed
   * @param surfaceRoughness
   *          the surface roughness index
   * @param uasterisk
   *          The friction wind speed
   * @param f
   * @param kappa
   * @return
   */
  protected static double calculateWindAtAltitude (double newAltitude,
                                                   double surfaceRoughness,
                                                   double uasterisk, double f,
                                                   double kappa)
  {

    return (log(newAltitude / surfaceRoughness) * uasterisk + UASTERISK_CONS
                                                              * f * newAltitude)
           / kappa;
  }

  /**
   * Calculated the friction wind speed u*
   * 
   * @param inputwindspeed
   * @param altitude
   * @param f
   * @param surfaceRoughness
   * @param kappa
   * @return
   */
  protected static double calculateUasterisk (double inputwindspeed,
                                              double altitude, double f,
                                              double surfaceRoughness,
                                              double kappa)
  {
    return (inputwindspeed * kappa - UASTERISK_CONS * f * altitude)
           / log(altitude / surfaceRoughness);
  }

  /**
   * Calculate the f parameter used in the calculation of the friction wind
   * speed
   * 
   * @param latitude
   * @return
   */
  protected static double calulcatef (double latitude)
  {
    return 2 * EARTH_ROTATIONAL_SPEED * sin(toRadians(abs(latitude)));
  }

  /**
   * @return the ratedOutput
   */
  public double getRatedOutput ()
  {
    return ratedOutput;
  }

  /**
   * @return the hubHeigth
   */
  public double getHubHeigth ()
  {
    return hubHeigth;
  }

  /**
   * @return the curve
   */
  public Curve getPowerCurve ()
  {
    return powerCurve;
  }

  /**
   * @return the refAltitude
   */
  public double getRefAltitude ()
  {
    return refAltitude;
  }

  /**
   * @param refAltitude
   *          the refAltitude to set
   */
  public void setRefAltitude (double refAltitude)
  {
    if (refAltitude <= 0)
      throw new IllegalArgumentException("Negative of zero reference altitude.");
    this.refAltitude = refAltitude;
  }

  /**
   * @return the surfaceRoughness
   */
  public double getSurfaceRoughness ()
  {
    return surfaceRoughness;
  }

  /**
   * @return the standardAirDensity
   */
  public double getStandardAirDensity ()
  {
    return STANDARD_AIR_DENSITY;
  }

  /**
   * @return the kappa
   */
  public double getKappa ()
  {
    return kappa;
  }

  /**
   * @param kappa
   *          the kappa to set
   */
  public void setKappa (double kappa)
  {
    if (kappa <= 0)
      throw new IllegalArgumentException("Negative of zero kappa");
    this.kappa = kappa;
  }

  /**
   * @return the latitude
   */
  public double getLatitude ()
  {
    return latitude;
  }

  /**
   * @return the rs
   */
  public RandomSeed getRs ()
  {
    return rs;
  }

  /**
   * @param rs
   *          the rs to set
   */
  public void setRs (RandomSeed rs)
  {
    this.rs = rs;
  }

  /**
   * @param latitude
   *          the latitude to set
   */
  public void setLatitude (double latitude)
  {
    this.latitude = latitude;
  }

  /**
   * @param surfaceRoughness
   *          the surfaceRoughness to set
   */
  public void setSurfaceRoughness (double surfaceRoughness)
  {
    this.surfaceRoughness = surfaceRoughness;
  }

  /**
   * @param ratedOutput
   *          the ratedOutput to set
   */
  public void setRatedOutput (double ratedOutput)
  {
    this.ratedOutput = ratedOutput;
  }

  /**
   * @param hubHeigth
   *          the hubHeigth to set
   */
  public void setHubHeigth (double hubHeigth)
  {
    this.hubHeigth = hubHeigth;
  }

  /**
   * @param powerCurve
   *          the powerCurve to set
   */
  public void setPowerCurve (Curve powerCurve)
  {
    this.powerCurve = powerCurve;
  }

  /**
   * @return the timeslotLengthInMin
   */
  public int getTimeslotLengthInMin ()
  {
    return timeslotLengthInMin;
  }

  /**
   * @param timeslotLengthInMin
   *          the timeslotLengthInMin to set
   */
  public void setTimeslotLengthInMin (int timeslotLengthInMin)
  {
    this.timeslotLengthInMin = timeslotLengthInMin;
  }

}
