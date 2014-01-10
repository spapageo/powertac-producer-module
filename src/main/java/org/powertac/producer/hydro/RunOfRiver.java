/**
 * 
 */
package org.powertac.producer.hydro;

import org.powertac.common.IdGenerator;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.producer.utils.Curve;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("run-of-the-river")
public class RunOfRiver extends HydroBase
{

  public RunOfRiver (Curve inputFlow, double minFlow,
                     double maxFlow, Curve turbineEfficiency,
                     double initialVolume, double initialHeight,double staticLosses,
                     double capacity)
  {
    super("Run of the river hydro plant", inputFlow, minFlow, maxFlow,
          turbineEfficiency, initialVolume, initialHeight, capacity,staticLosses);
    this.costPerKwh = 0.08;
  }

  @Override
  protected void updateVolume (double avInputFlow, double computedFlow)
  {
    // no need to do anything
  }

  @Override
  protected void updateHeigth ()
  {
    // no need to do anything
  }

  @Override
  protected double getFlow (double avInputFlow)
  {
    return avInputFlow;
  }

  @Override
  protected double getOutput (WeatherReport weatherReport)
  {
    return getOutput(this.timeService.getCurrentDateTime().getDayOfYear());
  }

  @Override
  protected double
    getOutput (int timeslotIndex,
               WeatherForecastPrediction weatherForecastPrediction)
  {
    return getOutput(this.timeslotRepo.getTimeForIndex(timeslotIndex)
            .toDateTime().getDayOfYear());
  }

  /**
   * This function is called after de-serialization
   */
  protected Object readResolve(){
    this.name = "Run of the river hydro plant";
    initialize(name, PowerType.RUN_OF_RIVER_PRODUCTION, 24, upperPowerCap,
               IdGenerator.createId());
    return this;
  }
}
