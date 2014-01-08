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
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("dam")
public class Dam extends HydroBase
{

  private Curve volumeHeight;
  @XStreamOmitField
  private Curve invCurveOut;

  public Dam (Curve inputFlow, double minFlow, double maxFlow,
              Curve turbineEfficiency, Curve volumeHeigth,
              double initialVolume, double capacity,double staticLosses)
  {
    super("Dam", inputFlow, minFlow, maxFlow, turbineEfficiency, initialVolume,
          volumeHeigth.value(initialVolume), capacity, staticLosses);
    this.volumeHeight = volumeHeigth;

    calculateInvOut();
  }

  protected void calculateInvOut ()
  {
    Curve c = new Curve();
    // calculate inverse curve output function for unit height
    for (double flow = minFlow; flow <= maxFlow; flow +=
      (maxFlow - minFlow) / 10) {
      double pow = getWaterPower(this.staticLosses,
                                 this.turbineEfficiency.value(flow/maxFlow),
                                 flow, 1)*timeslotLengthInMin/(1000*60);
      c.add(flow, pow);
    }

    invCurveOut = c.getInvertiblePart();
  }

  @Override
  protected void updateVolume (double inputFlow,double computedFlow)
  {
    this.volume += ( inputFlow - computedFlow) * 3600;
  }

  @Override
  protected double getFlow (double inputFlow)
  {
    if (preferredOutput == upperPowerCap) {
      return maxFlow;
    }
    else {
      return invCurveOut.value(-preferredOutput / height);
    }
  }

  @Override
  protected void updateHeigth ()
  {
    height = volumeHeight.value(volume);
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

  /**
   * This function is called after de-serialization
   */
  protected Object readResolve(){
    this.name = "Dam";
    initialize(name, PowerType.RUN_OF_RIVER_PRODUCTION, 24, upperPowerCap,
               IdGenerator.createId());
    calculateInvOut();
    return this;
  }

  /**
   * @return the volumeHeight
   */
  public Curve getVolumeHeight ()
  {
    return volumeHeight;
  }

  /**
   * @return the invCurveOut
   */
  public Curve getInvCurveOut ()
  {
    return invCurveOut;
  }

  /**
   * @param volumeHeight the volumeHeight to set
   */
  public void setVolumeHeight (Curve volumeHeight)
  {
    this.volumeHeight = volumeHeight;
  }
}
