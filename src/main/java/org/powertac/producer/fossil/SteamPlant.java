/**
 * 
 */
package org.powertac.producer.fossil;

import java.util.Random;

import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.producer.Producer;
import org.powertac.producer.utils.Curve;

import static java.lang.Math.*;

/**
 * @author Doom
 * 
 */
public class SteamPlant extends Producer
{
  private double adjustmentSpeed;
  private double diviation;
  private double lastOutput;
  private Random rand;

  SteamPlant (String name, double adjustmentSpeed, double diviation,
              double capacity)
  {
    // Maybe change the profile hours
    super(name, PowerType.FOSSIL_PRODUCTION, 24, capacity);
    this.lastOutput = capacity;
    this.adjustmentSpeed = adjustmentSpeed;
    this.diviation = diviation;

    rand = new Random();
  }

  public double getOutput ()
  {
    Curve out = new Curve();
    out.add(0, lastOutput);
    double time =
      (preferredOutput - lastOutput)
              / (signum(preferredOutput - lastOutput) * adjustmentSpeed);
    out.add(time, preferredOutput);

    double outSum = 0.0;
    for (double t = 0.0; t < 60; t++) {
      outSum += out.value(t) + rand.nextGaussian() * diviation;
    }

    lastOutput = out.value(60.0);
    if (outSum > 0)
      throw new IllegalStateException("I fucked up");
    return outSum / 60.0;
  }

  /**
   * @return the adjustmentSpeed
   */
  public double getAdjustmentSpeed ()
  {
    return adjustmentSpeed;
  }

  /**
   * @param adjustmentSpeed
   *          the adjustmentSpeed to set
   */
  public void setAdjustmentSpeed (double adjustmentSpeed)
  {
    this.adjustmentSpeed = adjustmentSpeed;
  }

  /**
   * @return the diviation
   */
  public double getDiviation ()
  {
    return diviation;
  }

  /**
   * @param diviation
   *          the diviation to set
   */
  public void setDiviation (double diviation)
  {
    this.diviation = diviation;
  }

  @Override
  protected double getOutput (WeatherReport weatherReport)
  {
    return getOutput();
  }

  @Override
  protected double
    getOutput (int timeslotIndex,
               WeatherForecastPrediction weatherForecastPrediction)
  {
    return getOutput();
  }

}
