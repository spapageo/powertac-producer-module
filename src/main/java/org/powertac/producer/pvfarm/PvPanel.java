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
package org.powertac.producer.pvfarm;

import java.util.Calendar;
import java.util.TimeZone;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import static org.powertac.producer.pvfarm.SolarFarm.*;

/**
 * This class models a pv panel. It works 
 * by modeling the suns position to calculate the irradiance on the panel.
 * It also models the effect of clouds on the output.
 * 
 * @author Spyros Papageorgiou
 * 
 */
@XStreamAlias("panel")
public class PvPanel
{
  private static final double DEFAULT_M = 0;
  private static final double DEFAULT_S = 0;
  private static final double DEFAULT_R = -1.06;
  private static final double DEFAULT_Q = 0.33;
  private static final double DEFAULT_P = 0.95;
  private static final double DEFAULT_PANEL_CLEARNESS_INDEX = 0.2;
  private static final double DEFAULT_PANEL_REFERENCE_TEMP = 313.15;
  private static final double DEFAULT_EFFICIENCY_DEGRATION_WITH_TEMP = 0.0045;
  private static final int DEFAULT_OZONE_COMPRESSED_DEPTH = 3;
  private static final double DEFAULT_HUMIDITY = 0.2;
  private static final double DEFAULT_STATIC_LOSSES = 0.78;
  private static final double DEFAULT_GROUND_ALBEDO = 0.2;
  private static final double DEFAULT_AEROSOL_FERLACTION_DEPTH_RATIO = 0.95;
  private static final int DEFAULT_PRESSURE = 101325;
  private static final double DEFAULT_AEROSOL_ABSORPTION_COEF = 0.95;
  // The panel area in m^2
  private double panelArea;
  //The panel latitude in degrees
  private double panelLatitude;
  //The panel longitude in degrees
  private double panelLongitude;
  //The panel azimuth in degrees
  private double panelAzimuth;
  //The panel tilt in degrees
  private double panelTilt;
  //The panel efficiency in 
  private double panelEfficiency;
  // The static system losses > 0 & <= 1
  private double staticLosses = DEFAULT_STATIC_LOSSES;
  // The ground reflection coefficient
  private double groundAlbedo = DEFAULT_GROUND_ALBEDO;
  // The humidity at the pv panel site
  private double humidity = DEFAULT_HUMIDITY;
  //The ozone layer compressed depth in mm
  private double ozoneLayerDepth = DEFAULT_OZONE_COMPRESSED_DEPTH;
  // Air pressure in Pa at the pv panel site
  private double pressure = DEFAULT_PRESSURE;
  // Aerosol absorption coefficient
  private double Ta = DEFAULT_AEROSOL_ABSORPTION_COEF;
  // The ration of reflective to total depth of propagation due to aerosol
  private double w0 = DEFAULT_AEROSOL_FERLACTION_DEPTH_RATIO;
  // These are coefficient used to estimate the cloud cover absorption 
  // coefficient
  private double p = DEFAULT_P;
  private double q = DEFAULT_Q;
  private double r = DEFAULT_R;
  private double s = DEFAULT_S;
  private double m = DEFAULT_M;
  // The rate at which the extra temperature degrades efficiency
  private double b = DEFAULT_EFFICIENCY_DEGRATION_WITH_TEMP;
  // The reference panel temperature in kelvin.
  private double Tref = DEFAULT_PANEL_REFERENCE_TEMP;
  // The clearness index of the panel. Used to calculate the reflective losses
  private double clearIndex = DEFAULT_PANEL_CLEARNESS_INDEX;
  // The pv panels nominal capacity in kw < 0
  private double capacity;
  //default timeslot length for which energy is calculated
  @XStreamOmitField
  private int timeslotLengthInMin = MINUTES_IN_HOUR;

  /**
   * Construct an instance of a Pv panel.
   * @param panelArrea the panel area in m^2
   * @param panelLatitude panel latitude in degrees
   * @param panelLongitude panel longitude in degrees
   * @param panelAzimuth panel azimuth in degrees
   * @param panelTilt panel tilt in degrees
   * @param referenceEfficiency the panel reference efficiency
   * @param capacity
   */
  public PvPanel (double panelArrea, double panelLatitude,
                  double panelLongitude, double panelAzimuth, double panelTilt,
                  double referenceEfficiency, double capacity)
  {
    if (panelArrea <= 0 || panelLatitude > 90 || panelLatitude < -90
        || panelAzimuth > 360 || panelAzimuth < 0 || panelTilt > 90
        || panelTilt < 0 || referenceEfficiency < 0 || capacity > 0
        || panelLongitude <= -180 || panelLongitude >= 180
        || referenceEfficiency > 1)
      throw new IllegalArgumentException();
    this.panelArea = panelArrea;
    this.panelLatitude = panelLatitude;
    this.panelLongitude = panelLongitude;
    this.panelAzimuth = panelAzimuth;
    this.panelTilt = panelTilt;
    this.panelEfficiency = referenceEfficiency;
    this.capacity = capacity;
  }

  /**
   * Calculate the energy output of this panel in kwh
   * @param systemTime
   * @param timezone
   * @param cloudcover
   * @param temperature
   * @param windspeed
   * @return
   */
  public double getOutput (long systemTime, TimeZone timezone,
                           double cloudcover, double temperature,
                           double windspeed)
  {

    // do the calculations once for every minute
    Calendar cal = Calendar.getInstance(timezone);

    cal.setTimeInMillis(systemTime);

    double sum = 0;

    for (int i = 0; i < timeslotLengthInMin; i++) {
      // calculate solar time
      double solarTime = SolarModel.getSolarTime(panelLongitude,cal);
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
                                              panelArea);

        sum = sum + output;
      }
      cal.add(Calendar.MINUTE, 1);
    }

    if (Double.isInfinite(sum) || Double.isNaN(sum))
      throw new IllegalStateException("Power produced isn't a number");
    return -sum / (WATT_IN_KILOWATT * MINUTES_IN_HOUR);
  }

  /**
   * @return the panelArrea
   */
  public double getPanelArea ()
  {
    return panelArea;
  }

  /**
   * @param panelArea
   *          the panelArrea to set
   */
  public void setPanelArea (double panelArea)
  {
    if (panelArea < 0)
      throw new IllegalArgumentException();
    this.panelArea = panelArea;
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
    if (panelLatitude > 90 || panelLatitude < -90)
      throw new IllegalArgumentException();
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
    if (panelLongitude <= -180 || panelLongitude >= 180)
      throw new IllegalArgumentException();
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
    if (panelAzimuth < 0 || panelAzimuth > 360)
      throw new IllegalArgumentException();
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
    if (panelTilt < -90 || panelTilt > 90)
      throw new IllegalArgumentException();
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
    if (panelEfficiency < 0 || panelEfficiency > 1)
      throw new IllegalArgumentException();
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
    if (groundAlbedo < 0)
      throw new IllegalArgumentException();
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
    if (humidity < 0 || humidity > 1)
      throw new IllegalArgumentException();
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
    if (ozoneLayerDepth < 0)
      throw new IllegalArgumentException();
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
    if (pressure < 0)
      throw new IllegalArgumentException();
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
    if (ta < 0)
      throw new IllegalArgumentException();
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
    if (tref <= 0)
      throw new IllegalArgumentException();
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
    if (staticLosses <= 0 || staticLosses > 1)
      throw new IllegalArgumentException();
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
    if (capacity <= 0)
      throw new IllegalArgumentException();
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
   * @param timeslotLengthInMin
   *          the timeslotLengthInMin to set
   */
  public void setTimeslotLengthInMin (int timeslotLengthInMin)
  {
    if (timeslotLengthInMin <= 0)
      throw new IllegalArgumentException();
    this.timeslotLengthInMin = timeslotLengthInMin;
  }
}
