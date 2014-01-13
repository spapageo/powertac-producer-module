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
  
  public SteamPlant (double adjustmentSpeed, double diviation, double capacity)
  {
    // Maybe change the profile hours
    super("Steam plant", PowerType.FOSSIL_PRODUCTION, 24, capacity);
    if (adjustmentSpeed <= 0 || diviation <= 0)
      throw new IllegalArgumentException();
    this.lastOutput = capacity;
    this.adjustmentSpeed = adjustmentSpeed;
    this.diviation = diviation;
    this.co2Emissions = 1.5;
    this.costPerKwh = 0.1;

  }

  public double getOutput ()
  {
    Curve out = new Curve();
    out.add(0, lastOutput);
    if (abs(preferredOutput - lastOutput) > 0.01 * abs(upperPowerCap)) {
      double time =
        (preferredOutput - lastOutput)
                / (signum(preferredOutput - lastOutput) * adjustmentSpeed);
      out.add(time / 2,
              (signum(preferredOutput - lastOutput) * adjustmentSpeed) * time
                      / 2 + lastOutput);
      out.add(time, preferredOutput);
    }
    else {
      out.add(timeslotLengthInMin / 2, preferredOutput);
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
  protected Object readResolve ()
  {
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
               WeatherForecastPrediction weatherForecastPrediction,
               double previousOutput)
  {
    double savedOutput = this.lastOutput;
    this.lastOutput = previousOutput;
    double power = getOutput();
    this.lastOutput = savedOutput;
    return power;
  }

  /**
   * @param adjustmentSpeed
   *          the adjustmentSpeed to set
   */
  public void setAdjustmentSpeed (double adjustmentSpeed)
  {
    if (adjustmentSpeed <= 0)
      throw new IllegalArgumentException();
    this.adjustmentSpeed = adjustmentSpeed;
  }

  /**
   * @param diviation
   *          the diviation to set
   */
  public void setDiviation (double diviation)
  {
    if (diviation <= 0)
      throw new IllegalArgumentException();
    this.diviation = diviation;
  }

}
