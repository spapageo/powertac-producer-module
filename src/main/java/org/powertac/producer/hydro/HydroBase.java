/**
 * 
 */
package org.powertac.producer.hydro;

import org.powertac.common.enumerations.PowerType;
import org.powertac.producer.Producer;
import org.powertac.producer.utils.Curve;

/**
 * @author Spyros Papageorgiou
 * 
 */
public abstract class HydroBase extends Producer
{
  protected Curve inputFlow;
  protected double minFlow;
  protected double maxFlow;
  private static double waterDensity = 999.972;
  private static double g = 9.80665;
  protected Curve turbineEfficiency;
  protected double volume;
  protected double height;
  protected double staticLosses;

  public HydroBase (String name, Curve inputFlow, double minFlow,
                    double maxFlow, Curve turbineEfficiency,
                    double initialVolume, double initialHeight, double capacity,
                    double staticLosses)
  {
    // no dam production put both on run of the river
    super(name, PowerType.RUN_OF_RIVER_PRODUCTION, 24, capacity);
    if(inputFlow == null || minFlow < 0 || maxFlow < minFlow ||
            turbineEfficiency == null || initialVolume < 0 || initialHeight < 0 ||
            staticLosses < 0 || staticLosses > 1)
      throw new IllegalArgumentException();
    this.inputFlow = inputFlow;
    this.minFlow = minFlow;
    this.maxFlow = maxFlow;
    this.turbineEfficiency = turbineEfficiency;
    this.volume = initialVolume;
    this.height = initialHeight;
    this.staticLosses = staticLosses;
  }

  protected double getOutput (int day)
  {
    if (day < 0 || day > 366)
      throw new IllegalArgumentException();

    double waterFlow = getFlow(inputFlow.value(day));

    double turbEff = turbineEfficiency.value(waterFlow/maxFlow);

    //make the power into kwh
    double power = -getWaterPower(staticLosses,turbEff, waterFlow, height)
            * timeslotLengthInMin /(60 * 1000);

    updateVolume(inputFlow.value(day),waterFlow);
    updateHeigth();
    if (power > 0)
      throw new IllegalStateException("Positive power");
    return power;

  }

  protected double getWaterPower (double staticLosseCoef, double turbineEff,
                                  double flow, double heigth)
  {
    if (flow >= minFlow && flow <= maxFlow)
      return staticLosseCoef * turbineEff * waterDensity * g * heigth * flow;
    else if (flow > maxFlow)
      return staticLosseCoef * turbineEff * waterDensity * g * heigth * maxFlow;
    else
      return 0;
  }

  protected abstract void updateVolume (double avInputFlow, double turbineFlow);

  protected abstract void updateHeigth ();

  protected abstract double getFlow (double avInputFlow);

  /**
   * @return the inputFlow
   */
  public Curve getInputFlow ()
  {
    return inputFlow;
  }

  /**
   * @return the minFlow
   */
  public double getMinFlow ()
  {
    return minFlow;
  }

  /**
   * @return the maxFlow
   */
  public double getMaxFlow ()
  {
    return maxFlow;
  }

  /**
   * @return the turbineEfficiency
   */
  public Curve getTurbineEfficiency ()
  {
    return turbineEfficiency;
  }

  /**
   * @return the volume
   */
  public double getVolume ()
  {
    return volume;
  }

  /**
   * @return the height
   */
  public double getHeight ()
  {
    return height;
  }

  /**
   * @param inputFlow the inputFlow to set
   */
  public void setInputFlow (Curve inputFlow)
  {
    if(inputFlow == null)
      throw new IllegalArgumentException();
    this.inputFlow = inputFlow;
  }

  /**
   * @param minFlow the minFlow to set
   */
  public void setMinFlow (double minFlow)
  {
    if(minFlow < 0 || minFlow >= maxFlow)
      throw new IllegalArgumentException();
    this.minFlow = minFlow;
  }

  /**
   * @param maxFlow the maxFlow to set
   */
  public void setMaxFlow (double maxFlow)
  {
    if(maxFlow < 0 || maxFlow <= minFlow)
      throw new IllegalArgumentException();
    this.maxFlow = maxFlow;
  }

  /**
   * @param turbineEfficiency the turbineEfficiency to set
   */
  public void setTurbineEfficiency (Curve turbineEfficiency)
  {
    if(turbineEfficiency == null)
      throw new IllegalArgumentException();
    this.turbineEfficiency = turbineEfficiency;
  }

  /**
   * @param volume the volume to set
   */
  public void setVolume (double volume)
  {
    if(volume < 0)
      throw new IllegalArgumentException();
    this.volume = volume;
  }

  /**
   * @param height the height to set
   */
  public void setHeight (double height)
  {
    if(height < 0)
      throw new IllegalArgumentException();
    this.height = height;
  }
}
