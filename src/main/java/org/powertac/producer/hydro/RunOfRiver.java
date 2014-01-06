/**
 * 
 */
package org.powertac.producer.hydro;

import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.producer.utils.Curve;

/**
 * @author Spyros Papageorgiou
 * 
 */
public class RunOfRiver extends HydroBase
{

  public RunOfRiver (String name, Curve inputFlow, double minFlow,
                     double maxFlow, Curve turbineEfficiency,
                     double initialVolume, double installedCapacity,
                     double initialHeight, double capacity)
  {
    super(name, inputFlow, minFlow, maxFlow, turbineEfficiency, initialVolume,
          initialHeight, capacity);
  }

  @Override
  protected void updateVolume (double inputFlow)
  {
    // no need to do anything
  }

  @Override
  protected void updateHeigth ()
  {
    // no need to do anything
  }

  @Override
  protected double getFlow (double inputFlow)
  {
    return inputFlow;
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
    return getOutput(this.timeslotService.getTimeForIndex(timeslotIndex)
            .toDateTime().getDayOfYear());
  }
}
