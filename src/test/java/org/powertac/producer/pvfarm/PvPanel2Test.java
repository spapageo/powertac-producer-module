package org.powertac.producer.pvfarm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

public class PvPanel2Test
{

  @Test
  public void getOutput () throws FileNotFoundException
  { 
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/pv_model.txt");
    PvPanel panel = new PvPanel(1.407, 41.88, -87.6278, 180, 30, 0.15, -200);
    panel.setStaticLosses(0.7);
    
    double panelLongitude = panel.getPanelLongitude();
    double panelLatitude = panel.getPanelLatitude();
    double pressure = panel.getPressure();
    double panelAzimuth = panel.getPanelAzimuth();
    double panelTilt = panel.getPanelTilt();
    double ozoneLayerDepth = panel.getOzoneLayerDepth();
    double humidity = panel.getHumidity();
    double groundAlbedo = panel.getGroundAlbedo();
    double Tref = panel.getTref();
    double panelArea = panel.getPanelArea();
    double panelEfficiency = panel.getPanelEfficiency();
    double b = panel.getB();
    double staticLosses = panel.getStaticLosses();
    double Ta = panel.getTa();
    double w0 = panel.getW0();
    double clearIndex = panel.getClearIndex();
   
    double p = panel.getP();
    double q = panel.getQ();
    double r = panel.getR();
    double s = panel.getS();
    double m = panel.getM();
    
    double cloudcover = 0;
    double windspeed = 4;
    double temperature = 300;
    
    // do the calculations once for every minute
    TimeZone tz = TimeZone.getTimeZone("America/Chicago");
    Calendar cal = Calendar.getInstance(tz);

    cal.set(Calendar.DAY_OF_MONTH, 25);
    cal.set(Calendar.MONTH, Calendar.JUNE);
    cal.set(Calendar.YEAR, 2012);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE,0);
    cal.set(Calendar.SECOND,0);
    cal.set(Calendar.MILLISECOND,0);


    while (cal.get(Calendar.DAY_OF_MONTH) == 25) {
      // calculate solar time
      double solarTime =
        SolarModel.getSolarTime(panelLongitude,cal);
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

        double hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE)/60.0;
        pw.printf(Locale.ENGLISH,"%f,%f%n",hour/24,output);
      }else{
        double hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE)/60.0;
        pw.printf(Locale.ENGLISH,"%f,%f%n",hour/24,0.0);
      }
      cal.add(Calendar.MINUTE, 1);
    }
    
    pw.close();
  }
}
