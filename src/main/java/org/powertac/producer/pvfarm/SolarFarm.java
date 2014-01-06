/**
 * 
 */
package org.powertac.producer.pvfarm;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.producer.Producer;

/**
 * @author Spyros Papageorgiou
 * 
 */
public class SolarFarm extends Producer
{

  List<PvPanel> panelList = new ArrayList<>();

  /**
   * @param name
   * @param powerType
   * @param profileHours
   * @param capacity
   */
  public SolarFarm (String name)
  {
    super(name, PowerType.SOLAR_PRODUCTION, 24, 0);
  }

  /**
   * Add a panel to this farm
   * 
   * @param panel
   */
  public void addPanel (PvPanel panel)
  {
    panelList.add(panel);
    upperPowerCap += panel.getCapacity();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.powertac.producer.Producer#getOutput(org.powertac.common.WeatherReport
   * )
   */
  @Override
  protected double getOutput (WeatherReport weatherReport)
  {
    double powerSum = 0;
    long systemTime =
      timeslotService.getTimeForIndex(weatherReport.getTimeslotIndex())
              .getMillis();
    TimeZone timezone =
      timeslotService.getTimeForIndex(weatherReport.getTimeslotIndex())
              .getZone().toTimeZone();
    for (PvPanel panel: panelList) {
      powerSum +=
        panel.getOutput(systemTime, timezone, weatherReport.getCloudCover(),
                        weatherReport.getTemperature(),
                        weatherReport.getWindSpeed());
    }
    return powerSum;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.powertac.producer.Producer#getOutput(int,
   * org.powertac.common.WeatherForecastPrediction)
   */
  @Override
  protected double
    getOutput (int timeslotIndex,
               WeatherForecastPrediction weatherForecastPrediction)
  {
    double powerSum = 0;
    long systemTime =
      timeslotService.getTimeForIndex(timeslotIndex).getMillis();
    TimeZone timezone =
      timeslotService.getTimeForIndex(timeslotIndex).getZone().toTimeZone();
    for (PvPanel panel: panelList) {
      powerSum +=
        panel.getOutput(systemTime, timezone,
                        weatherForecastPrediction.getCloudCover(),
                        weatherForecastPrediction.getTemperature(),
                        weatherForecastPrediction.getWindSpeed());
    }
    return powerSum;
  }

}
