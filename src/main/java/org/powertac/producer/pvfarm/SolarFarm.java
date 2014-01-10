/**
 * 
 */
package org.powertac.producer.pvfarm;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.powertac.common.IdGenerator;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.producer.Producer;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("solar-farm")
public class SolarFarm extends Producer
{

  @XStreamImplicit
  List<PvPanel> panelList = new ArrayList<>();

  /**
   * @param name
   * @param powerType
   * @param profileHours
   * @param capacity
   */
  public SolarFarm ()
  {
    super("Solar farm", PowerType.SOLAR_PRODUCTION, 24, 0);
    this.costPerKwh = 0.14;
  }

  /**
   * Add a panel to this farm
   * 
   * @param panel
   */
  public void addPanel (PvPanel panel)
  {
    if(panel == null)
      return;
    panelList.add(panel);
    upperPowerCap += panel.getCapacity();
    panel.setTimeslotLengthInMin(timeslotLengthInMin);
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
      timeslotRepo.getTimeForIndex(weatherReport.getTimeslotIndex())
              .getMillis();
    TimeZone timezone =
      timeslotRepo.getTimeForIndex(weatherReport.getTimeslotIndex())
              .getZone().toTimeZone();
    for (PvPanel panel: panelList) {
      powerSum +=
        panel.getOutput(systemTime, timezone, weatherReport.getCloudCover(),
                        weatherReport.getTemperature(),
                        weatherReport.getWindSpeed());
    }
    if(Double.isInfinite(powerSum) || Double.isNaN(powerSum))
      throw new IllegalStateException("Power produced isn't a number");
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
      timeslotRepo.getTimeForIndex(timeslotIndex).getMillis();
    TimeZone timezone =
      timeslotRepo.getTimeForIndex(timeslotIndex).getZone().toTimeZone();
    for (PvPanel panel: panelList) {
      powerSum +=
        panel.getOutput(systemTime, timezone,
                        weatherForecastPrediction.getCloudCover(),
                        weatherForecastPrediction.getTemperature(),
                        weatherForecastPrediction.getWindSpeed());
    }
    return powerSum;
  }

  /**
   * This function is called after de-serialization
   */
  protected Object readResolve ()
  {
    this.name = "Solar farm";
    initialize(name, PowerType.FOSSIL_PRODUCTION, 24, upperPowerCap,
               IdGenerator.createId());
    for(PvPanel panel : panelList){
      panel.setTimeslotLengthInMin(timeslotLengthInMin);
    }
    return this;
  }

}
