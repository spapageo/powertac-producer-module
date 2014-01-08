/**
 * 
 */
package org.powertac.producer.fossil;

import org.powertac.common.IdGenerator;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.producer.Producer;
import org.powertac.producer.utils.Curve;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import static java.lang.Math.*;

/**
 * @author Doom
 * 
 */
@XStreamAlias("steam-plant")
public class SteamPlant extends Producer
{
  private double adjustmentSpeed;
  private double diviation;
  @XStreamOmitField
  private double lastOutput;

  public SteamPlant (double adjustmentSpeed, double diviation,
                     double capacity)
  {
    // Maybe change the profile hours
    super("Steam plant", PowerType.FOSSIL_PRODUCTION,
          24, capacity);
    if(adjustmentSpeed <= 0 || diviation <= 0 )
      throw new IllegalArgumentException();
    this.lastOutput = capacity;
    this.adjustmentSpeed = adjustmentSpeed;
    this.diviation = diviation;

  }

  public double getOutput ()
  {
    Curve out = new Curve();
    out.add(0, lastOutput);
    if(abs(preferredOutput - lastOutput) > 0.01*abs(upperPowerCap)){
      double time =
              (preferredOutput - lastOutput)
              / (signum(preferredOutput - lastOutput) * adjustmentSpeed);
      out.add(time/2, (signum(preferredOutput - lastOutput) * adjustmentSpeed)
              * time / 2 + lastOutput);
      out.add(time, preferredOutput);
    }else{
      out.add(timeslotLengthInMin/2, preferredOutput);
      out.add(timeslotLengthInMin, preferredOutput);
    }
    
    double outSum = 0.0;
    for (double t = 0.0; t < timeslotLengthInMin; t++) {
      outSum += out.value(t) + seed.nextGaussian() * diviation;
    }

    lastOutput = out.value(60.0);
    if (Double.isInfinite(outSum) || Double.isNaN(outSum))
      throw new IllegalStateException("Invalid power");
    return outSum / 60.0;
  }

  /**
   * This function is called after de-serialization
   */
  protected Object readResolve(){
    this.lastOutput = this.upperPowerCap;
    this.name = "Steam plant";
    initialize(name, PowerType.FOSSIL_PRODUCTION, 24, upperPowerCap,
               IdGenerator.createId());
    return this;
  }

  /**
   * @return the adjustmentSpeed
   */
  public double getAdjustmentSpeed ()
  {
    return adjustmentSpeed;
  }

  /**
   * @return the diviation
   */
  public double getDiviation ()
  {
    return diviation;
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

  /**
   * @param adjustmentSpeed the adjustmentSpeed to set
   */
  public void setAdjustmentSpeed (double adjustmentSpeed)
  {
    if(adjustmentSpeed <= 0)
      throw new IllegalArgumentException();
    this.adjustmentSpeed = adjustmentSpeed;
  }

  /**
   * @param diviation the diviation to set
   */
  public void setDiviation (double diviation)
  {
    if(diviation <= 0 )
      throw new IllegalArgumentException();
    this.diviation = diviation;
  }

}
