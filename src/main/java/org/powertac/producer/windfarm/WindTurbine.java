/**
 * 
 */
package org.powertac.producer.windfarm;

import org.powertac.common.IdGenerator;
import org.powertac.common.RandomSeed;
import org.powertac.producer.utils.Curve;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import static java.lang.Math.*;

/**
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("turbine")
public class WindTurbine
{

  private double latitude;

  // The altitude for which the input speed is given
  private double refAltitude = 10;

  // The roughness of the surface near the turbine
  private double surfaceRoughness;

  // The air density at sea level at 15 C
  private static double standardAirDensity = 1.225;

  private double kappa = 0.4;

  // The maximum/rated output
  private double ratedOutput;

  // The hub height
  private double hubHeigth;

  // The power curve for 15 C at sea level
  private Curve powerCurve;

  @XStreamOmitField
  private RandomSeed rs;

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

  public double getPowerOutput (double temperature, double avrHourlyWindSpeed)
  {
    if(rs == null)
      throw new IllegalStateException("No seed was set");
    else
      rs = new RandomSeed("Wind turbine" + IdGenerator.createId(), 0,
                          "Simulation");
    
    double sumPowerOutput = 0;

    // Get the wind speed at the height of the turbine hub
    double f = calulcatef(latitude);
    double ua =
      calculateUasterisk(avrHourlyWindSpeed, refAltitude, f, surfaceRoughness,
                         kappa);
    //ua can't be negative since it represents, wind speed 
    if(ua < 0)
      ua = 0;
    double correctedHourlySpeed =
      calculateWindAtAltitude(hubHeigth, surfaceRoughness, ua, f, kappa);
    
    double std;
    
    if(correctedHourlySpeed > 1)
      std = calculateStd(f, ua, hubHeigth, surfaceRoughness);
    else
      std = 0.1 * correctedHourlySpeed;
    
    for (int i = 0; i < 60; i++) {
      sumPowerOutput +=
        calculateAirDensity(temperature, hubHeigth)
                * powerCurve.value(sampleGaussian(std, correctedHourlySpeed))
                / standardAirDensity;
    }

    return -sumPowerOutput / 60;
  }

  public double sampleGaussian (double std, double mean)
  {
    return rs.nextGaussian() * std + mean;
  }

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

  protected double calculateAirDensity (double temperature, double altitude)
  {
    // po (1-Lh/T0)^ (gM/RL)
    double p0 = 101325;
    double L = 0.0065;
    double g = 9.80665;
    double M = 0.0289644;
    double R = 8.31447;
    double T0 = 288.15;
    // calculate pressure as a function of altitude
    double p = p0 * pow(1 - (L * altitude) / T0, (g * M) / (R * L));
    // calculate the air density from the pressure and the temperature
    return (p * M) / (R * temperature);
  }

  protected static double calculateWindAtAltitude (double newAltitude,
                                                   double surfaceRoughness,
                                                   double uasterisk, double f,
                                                   double kappa)
  {

    return (log(newAltitude / surfaceRoughness) * uasterisk + 34.5 * f
                                                              * newAltitude)
           / kappa;
  }

  protected static double calculateUasterisk (double inputwindspeed,
                                              double altitude, double f,
                                              double surfaceRoughness,
                                              double kappa)
  {
    return (inputwindspeed * kappa - 34.5 * f * altitude)
           / log(altitude / surfaceRoughness);
  }

  protected static double calulcatef (double latitude)
  {
    return 2 * 7.2 * pow(10, -5) * sin(toRadians(abs(latitude)));
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
    return standardAirDensity;
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
   * @param rs the rs to set
   */
  public void setRs (RandomSeed rs)
  {
    this.rs = rs;
  }

  /**
   * @param latitude the latitude to set
   */
  public void setLatitude (double latitude)
  {
    this.latitude = latitude;
  }

  /**
   * @param surfaceRoughness the surfaceRoughness to set
   */
  public void setSurfaceRoughness (double surfaceRoughness)
  {
    this.surfaceRoughness = surfaceRoughness;
  }

  /**
   * @param ratedOutput the ratedOutput to set
   */
  public void setRatedOutput (double ratedOutput)
  {
    this.ratedOutput = ratedOutput;
  }

  /**
   * @param hubHeigth the hubHeigth to set
   */
  public void setHubHeigth (double hubHeigth)
  {
    this.hubHeigth = hubHeigth;
  }

  /**
   * @param powerCurve the powerCurve to set
   */
  public void setPowerCurve (Curve powerCurve)
  {
    this.powerCurve = powerCurve;
  }

}
