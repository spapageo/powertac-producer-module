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
 * Represents a fossil plant mainly based on steam or gas.
 * 
 * @author Doom
 * 
 */
@XStreamAlias("steam-plant")
public class SteamPlant extends Producer
{
  private static final double DEFAULT_FOSSIL_POWER_COST_PER_KWH = 0.1;
  private static final double DEFAULT_FOSSIL_CO2_EMISSIONS = 1.5;
  private static final double BOTTOM_THRESHOLD_MULT = 0.01;
  private static final int DEFAULT_FOSSIL_PROFILE_HOURS = 24;

  private double adjustmentSpeed;
  private double diviation;

  @XStreamOmitField
  private double lastOutput;

  /**
   * The only constructor for the fossil steam plant
   * 
   * @param adjustmentSpeed
   *          the maximum amount of output change in one minute
   * @param diviation
   *          the deviation of the plant output
   * @param capacity
   *          the plant capacity
   */
  public SteamPlant (double adjustmentSpeed, double diviation, double capacity)
  {
    // Maybe change the profile hours
    super("Steam plant", PowerType.FOSSIL_PRODUCTION,
          DEFAULT_FOSSIL_PROFILE_HOURS, capacity);

    if (adjustmentSpeed <= 0 || diviation <= 0)
      throw new IllegalArgumentException();
    this.lastOutput = capacity;
    this.adjustmentSpeed = adjustmentSpeed;
    this.diviation = diviation;
    this.co2Emissions = DEFAULT_FOSSIL_CO2_EMISSIONS;
    this.costPerKwh = DEFAULT_FOSSIL_POWER_COST_PER_KWH;

  }

  /**
   * Generate the the energy output for plant in kwh
   * 
   * @return the energy <= 0
   */
  public double getOutput ()
  {
    // We need to add at least 3 point to the curve for it to work
    Curve out = new Curve();
    // One
    out.add(0, lastOutput);

    // Check if lastOuput and preferredOutput aren't very close
    if (abs(preferredOutput - lastOutput) > BOTTOM_THRESHOLD_MULT
                                            * abs(upperPowerCap)) {
      // Calculate when will this plant reach the
      double time =
        (preferredOutput - lastOutput)
                / (signum(preferredOutput - lastOutput) * adjustmentSpeed);
      // Two
      out.add(time / 2,
              (signum(preferredOutput - lastOutput) * adjustmentSpeed) * time
                      / 2 + lastOutput);
      // Three
      out.add(time, preferredOutput);
    }
    else {
      // Two
      out.add(timeslotLengthInMin / 2, preferredOutput);
      // Three
      out.add(timeslotLengthInMin, preferredOutput);
    }

    double outSum = 0.0;
    for (double t = 0.0; t < timeslotLengthInMin; t++) {
      // gaussian sample
      outSum += -abs(out.value(t) + seed.nextGaussian() * diviation);
    }

    lastOutput = out.value(MINUTES_IN_HOUR);

    if (Double.isInfinite(outSum) || Double.isNaN(outSum) || outSum > 0) {
      String cause =
        String.format("PrefferedOutput: %f lastOutput: %f%n", preferredOutput,
                      lastOutput);
      throw new IllegalStateException("Invalid power. " + cause);
    }
    return outSum / MINUTES_IN_HOUR;
  }

  /**
   * This function is called after de-serialization
   */
  protected Object readResolve ()
  {
    this.lastOutput = this.upperPowerCap;
    this.name = "Steam plant";
    initialize(name, PowerType.FOSSIL_PRODUCTION, DEFAULT_FOSSIL_PROFILE_HOURS,
               upperPowerCap, IdGenerator.createId());
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
