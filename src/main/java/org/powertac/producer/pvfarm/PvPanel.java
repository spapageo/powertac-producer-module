/**
 * 
 */
package org.powertac.producer.pvfarm;

import java.util.Calendar;
import java.util.TimeZone;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("panel")
public class PvPanel
{
  private double panelArrea;
  private double panelLatitude;
  private double panelLongitude;
  private double panelAzimuth;
  private double panelTilt;
  private double panelEfficiency;
  private double groundAlbedo = 0.2;
  private double humidity = 0.2;
  private double ozoneLayerDepth = 3;
  private double pressure = 101325;
  private double Ta = 0.95;
  private double w0 = 0.95;
  private double p = 0.95;
  private double q = 0.33;
  private double r = -1.06;
  private double s = 0;
  private double m = 0;
  private double b = 0.0045;
  private double Tref = 313.15;
  private double staticLosses = 0.78;
  private double clearIndex = 0.2;
  private double capacity;
  @XStreamOmitField
  private int timeslotLengthInMin = 60;
  public PvPanel (double panelArrea, double panelLatitude,
                  double panelLongitude, double panelAzimuth, double panelTilt,
                  double referenceEfficiency, double capacity)
  {
    if (panelArrea <= 0 || panelLatitude > 90 || panelLatitude < -90
        || panelAzimuth > 360 || panelAzimuth < 0 || panelTilt > 90
        || panelTilt < 0 || referenceEfficiency < 0 || capacity > 0)
      throw new IllegalArgumentException();
    this.panelArrea = panelArrea;
    this.panelLatitude = panelLatitude;
    this.panelLongitude = panelLongitude;
    this.panelAzimuth = panelAzimuth;
    this.panelTilt = panelTilt;
    this.panelEfficiency = referenceEfficiency;
    this.capacity = capacity;
  }

  public double getOutput (long systemTime, TimeZone timezone,
                           double cloudcover, double temperature,
                           double windspeed)
  {
    // assuming duration of one hour
    // do the calculations once for every minute
    Calendar cal = Calendar.getInstance(timezone);

    cal.setTimeInMillis(systemTime);

    int timezoneOffset = cal.get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000);
    double sum = 0;

    for (int i = 0; i < timeslotLengthInMin; i++) {
      // calculate solar time
      double solarTime =
        SolarModel.getSolarTime(panelLongitude, timezoneOffset,
                                cal.getTimeInMillis());
      // calculate sun position
      double sunAltitude =
        SolarModel.getSunAltitudeAngle(solarTime, panelLatitude,
                                       cal.get(Calendar.DAY_OF_YEAR));
      sunAltitude =
        sunAltitude
                + SolarModel.getSunAltitudeCorrection(sunAltitude, pressure,
                                                      temperature);
      double sunAzimuth =
        SolarModel.getSunAzinuthAngle(sunAltitude,
                                      cal.get(Calendar.DAY_OF_YEAR),
                                      panelLatitude, solarTime);

      if (sunAltitude > 0) {
        // calculate irradiance
        double inci =
          SolarModel.getIncidenceAngle(sunAltitude, sunAzimuth, panelAzimuth,
                                       panelTilt);
        double airmass = IrradianceModel.getAirMass(sunAltitude);

        double T0 = IrradianceModel.getT0(airmass, ozoneLayerDepth);

        double Tr = IrradianceModel.getTr(airmass);

        double aw = IrradianceModel.getaw(airmass, humidity, temperature);

        double f = IrradianceModel.getf(sunAltitude);

        double solarConstanct =
          IrradianceModel.getIrradianceConstant(cal.get(Calendar.DAY_OF_YEAR));

        double dir =
          IrradianceModel.getDirectIrradiance(sunAltitude, solarConstanct, T0,
                                              Tr, aw, Ta);
        double dif =
          IrradianceModel.getDiffuseIrradiance(sunAltitude, solarConstanct, T0,
                                               Tr, aw, Ta, w0, f);

        dir =
          IrradianceModel.getCloudModifiedIrradiance(dir, cloudcover,
                                                     groundAlbedo, p, q, r, s,
                                                     m);
        dif =
          IrradianceModel.getCloudModifiedIrradiance(dif, cloudcover,
                                                     groundAlbedo, p, q, r, s,
                                                     m);

        double inputIrrad;
        if (inci <= 90) {
          inputIrrad =
            IrradianceModel.getIrradiancOnTiltedPlane(dir, dif, inci,
                                                      sunAltitude, panelTilt,
                                                      groundAlbedo);
        }
        else {
          inputIrrad =
            IrradianceModel.getIrradiancOnTiltedPlane(0, dif, inci,
                                                      sunAltitude, panelTilt,
                                                      groundAlbedo);
        }

        double panelTemperature =
          ElectricalModel.getPanelTemperature(temperature, windspeed,
                                              inputIrrad);
        double thermaLosCoeff =
          ElectricalModel.getThermalLossCoeff(b, panelTemperature, Tref);
        double reflectiveLosCoeff =
          ElectricalModel.getReflectiveLossCoeff(inci, clearIndex);

        double output =
          ElectricalModel.getElectricalOutput(panelEfficiency, staticLosses,
                                              thermaLosCoeff,
                                              reflectiveLosCoeff, inputIrrad,
                                              panelArrea);

        sum = sum + output;
      }
      cal.add(Calendar.MINUTE, 1);
    }

    return -sum / 60;
  }

  /**
   * @return the panelArrea
   */
  public double getPanelArrea ()
  {
    return panelArrea;
  }

  /**
   * @param panelArrea
   *          the panelArrea to set
   */
  public void setPanelArrea (double panelArrea)
  {
    this.panelArrea = panelArrea;
  }

  /**
   * @return the panelLatitude
   */
  public double getPanelLatitude ()
  {
    return panelLatitude;
  }

  /**
   * @param panelLatitude
   *          the panelLatitude to set
   */
  public void setPanelLatitude (double panelLatitude)
  {
    this.panelLatitude = panelLatitude;
  }

  /**
   * @return the panelLongitude
   */
  public double getPanelLongitude ()
  {
    return panelLongitude;
  }

  /**
   * @param panelLongitude
   *          the panelLongitude to set
   */
  public void setPanelLongitude (double panelLongitude)
  {
    this.panelLongitude = panelLongitude;
  }

  /**
   * @return the panelAzimuth
   */
  public double getPanelAzimuth ()
  {
    return panelAzimuth;
  }

  /**
   * @param panelAzimuth
   *          the panelAzimuth to set
   */
  public void setPanelAzimuth (double panelAzimuth)
  {
    this.panelAzimuth = panelAzimuth;
  }

  /**
   * @return the panelTilt
   */
  public double getPanelTilt ()
  {
    return panelTilt;
  }

  /**
   * @param panelTilt
   *          the panelTilt to set
   */
  public void setPanelTilt (double panelTilt)
  {
    this.panelTilt = panelTilt;
  }

  /**
   * @return the panelEfficiency
   */
  public double getPanelEfficiency ()
  {
    return panelEfficiency;
  }

  /**
   * @param panelEfficiency
   *          the panelEfficiency to set
   */
  public void setPanelEfficiency (double panelEfficiency)
  {
    this.panelEfficiency = panelEfficiency;
  }

  /**
   * @return the groundAlbedo
   */
  public double getGroundAlbedo ()
  {
    return groundAlbedo;
  }

  /**
   * @param groundAlbedo
   *          the groundAlbedo to set
   */
  public void setGroundAlbedo (double groundAlbedo)
  {
    this.groundAlbedo = groundAlbedo;
  }

  /**
   * @return the humidity
   */
  public double getHumidity ()
  {
    return humidity;
  }

  /**
   * @param humidity
   *          the humidity to set
   */
  public void setHumidity (double humidity)
  {
    this.humidity = humidity;
  }

  /**
   * @return the ozoneLayerDepth
   */
  public double getOzoneLayerDepth ()
  {
    return ozoneLayerDepth;
  }

  /**
   * @param ozoneLayerDepth
   *          the ozoneLayerDepth to set
   */
  public void setOzoneLayerDepth (double ozoneLayerDepth)
  {
    this.ozoneLayerDepth = ozoneLayerDepth;
  }

  /**
   * @return the pressure
   */
  public double getPressure ()
  {
    return pressure;
  }

  /**
   * @param pressure
   *          the pressure to set
   */
  public void setPressure (double pressure)
  {
    this.pressure = pressure;
  }

  /**
   * @return the ta
   */
  public double getTa ()
  {
    return Ta;
  }

  /**
   * @param ta
   *          the ta to set
   */
  public void setTa (double ta)
  {
    Ta = ta;
  }

  /**
   * @return the w0
   */
  public double getW0 ()
  {
    return w0;
  }

  /**
   * @param w0
   *          the w0 to set
   */
  public void setW0 (double w0)
  {
    this.w0 = w0;
  }

  /**
   * @return the p
   */
  public double getP ()
  {
    return p;
  }

  /**
   * @param p
   *          the p to set
   */
  public void setP (double p)
  {
    this.p = p;
  }

  /**
   * @return the q
   */
  public double getQ ()
  {
    return q;
  }

  /**
   * @param q
   *          the q to set
   */
  public void setQ (double q)
  {
    this.q = q;
  }

  /**
   * @return the r
   */
  public double getR ()
  {
    return r;
  }

  /**
   * @param r
   *          the r to set
   */
  public void setR (double r)
  {
    this.r = r;
  }

  /**
   * @return the s
   */
  public double getS ()
  {
    return s;
  }

  /**
   * @param s
   *          the s to set
   */
  public void setS (double s)
  {
    this.s = s;
  }

  /**
   * @return the m
   */
  public double getM ()
  {
    return m;
  }

  /**
   * @param m
   *          the m to set
   */
  public void setM (double m)
  {
    this.m = m;
  }

  /**
   * @return the b
   */
  public double getB ()
  {
    return b;
  }

  /**
   * @param b
   *          the b to set
   */
  public void setB (double b)
  {
    this.b = b;
  }

  /**
   * @return the tref
   */
  public double getTref ()
  {
    return Tref;
  }

  /**
   * @param tref
   *          the tref to set
   */
  public void setTref (double tref)
  {
    Tref = tref;
  }

  /**
   * @return the staticLosses
   */
  public double getStaticLosses ()
  {
    return staticLosses;
  }

  /**
   * @param staticLosses
   *          the staticLosses to set
   */
  public void setStaticLosses (double staticLosses)
  {
    this.staticLosses = staticLosses;
  }

  /**
   * @return the clearIndex
   */
  public double getClearIndex ()
  {
    return clearIndex;
  }

  /**
   * @param clearIndex
   *          the clearIndex to set
   */
  public void setClearIndex (double clearIndex)
  {
    this.clearIndex = clearIndex;
  }

  /**
   * @return the capacity
   */
  public double getCapacity ()
  {
    return capacity;
  }

  /**
   * @param capacity
   *          the capacity to set
   */
  public void setCapacity (double capacity)
  {
    this.capacity = capacity;
  }

  /**
   * @return the timeslotLengthInMin
   */
  public int getTimeslotLengthInMin ()
  {
    return timeslotLengthInMin;
  }

  /**
   * @param timeslotLengthInMin the timeslotLengthInMin to set
   */
  public void setTimeslotLengthInMin (int timeslotLengthInMin)
  {
    this.timeslotLengthInMin = timeslotLengthInMin;
  }
}
