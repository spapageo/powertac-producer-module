package org.powertac.producer.pvfarm;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.junit.Test;
import org.powertac.producer.pvfarm.IrradianceModel;
import org.powertac.producer.pvfarm.SolarModel;

public class IrradianceModelTest
{

  @Test
  public void testGetIrradianceConstant ()
  {
    for (int i = 1; i < 367; i++)
      assertTrue((Math.abs(IrradianceModel.getIrradianceConstant(i) - 1367.7) / 1367.7) < 0.04);
  }

  @Test
  public void testClearSkyModelCoefficients () throws FileNotFoundException
  {
    PrintWriter pw = new PrintWriter(new File("dataSkyModelCoefficients.txt"));
    for (double airmass = 1.0; airmass < 38.0; airmass += 0.2) {
      double Tr = IrradianceModel.getTr(airmass);
      double T0 = IrradianceModel.getT0(airmass, 3);
      double aw = IrradianceModel.getaw(airmass, 0.2, 300);
      assertTrue(String.valueOf(T0 * Tr - aw), T0 * Tr - aw <= 1);
      assertTrue(String.valueOf(T0 * Tr - aw), T0 * Tr - aw >= 0);
      assertTrue(String.valueOf(T0), T0 <= 1);
      assertTrue(String.valueOf(T0), T0 >= 0);
      assertTrue("Airmass: " + airmass, 1 - Tr <= 1);
      assertTrue("Airmass: " + airmass, 1 - Tr >= 0);
      pw.printf("%f,%f,%f,%f,%n", airmass, T0 * Tr - aw, T0, 1 - Tr);
    }

    for (double temperature = 280; temperature < 320; temperature++) {
      double Tr = IrradianceModel.getTr(1);
      double T0 = IrradianceModel.getT0(1, 3);
      double aw = IrradianceModel.getaw(1, 0.5, temperature);
      assertTrue(T0 * Tr - aw <= 1 && T0 * Tr - aw >= 0);
    }
    pw.close();
  }

  @Test
  public void testGetAirMass () throws FileNotFoundException
  {
    PrintWriter pw = new PrintWriter(new File("dataGetAirMass.txt"));
    for (double g = 0; g < 91; g++) {
      double data = IrradianceModel.getAirMass(g);
      pw.printf("%f,%f%n", g, data);
      if (g == 0.0) {
        assertEquals(38, data, 0.5);
      }
      else if (g == 90.0) {
        assertEquals(1, data, 0.01);
      }
    }
    pw.close();
  }

  @Test
  public void testIrradianceModel () throws FileNotFoundException
  {
    PrintWriter pw = new PrintWriter(new File("dataIrradianceModel.txt"));
    for (double g = 0; g < 24; g += 0.1) {
      double sunAltitude = SolarModel.getSunAltitudeAngle(g, 45, 180);
      if (sunAltitude >= 0) {
        double airmass = IrradianceModel.getAirMass(sunAltitude);
        double direct =
          IrradianceModel
                  .getDirectIrradiance(sunAltitude,
                                       1367.7,
                                       IrradianceModel.getT0(airmass, 3),
                                       IrradianceModel.getTr(airmass),
                                       IrradianceModel.getaw(airmass, 0.1, 300),
                                       0.98);
        double diffuse =
          IrradianceModel.getDiffuseIrradiance(sunAltitude, 1367.7,
                                               IrradianceModel
                                                       .getT0(airmass, 3),
                                               IrradianceModel.getTr(airmass),
                                               IrradianceModel.getaw(airmass,
                                                                     0.1, 300),
                                               0.98, 0.98, IrradianceModel
                                                       .getf(sunAltitude));
        assertTrue(direct >= 0 && diffuse >= 0);
        pw.printf("%f,%f,%f%n", g, direct, diffuse);
      }
    }
    pw.close();
  }

  @Test
  public void testGetIrradianceOnTiltedPlane ()
  {
    double direct = 800;
    double diffuse = 40;
    double sa = 60;
    double pa = 30;
    double max =
      IrradianceModel.getIrradiancOnTiltedPlane(direct, diffuse, SolarModel
              .getIncidenceAngle(sa, 180, 180, pa), sa, pa, 0.2);
    for (pa = 0; pa <= 90; pa++) {
      double incid = SolarModel.getIncidenceAngle(sa, 180, 180, pa);
      double inc =
        IrradianceModel.getIrradiancOnTiltedPlane(direct, diffuse, incid, sa,
                                                  pa, 0.2);
      assertTrue(String.valueOf(incid), inc <= (max + 1));
    }
  }

  @Test
  public void testDailyIrradianceOnTitledPlane () throws FileNotFoundException
  {

    PrintWriter pw =
      new PrintWriter(new File("dataIrradianceOnTiltedPlane.txt"));
    PrintWriter pw2 =
      new PrintWriter(new File("dataIrradianceOnTiltedPlane2.txt"));

    int day = 180;
    double panelLatitude = 60;
    for (double tilt = 0; tilt <= 90; tilt += 1) {
      double outsum = 0;
      double outsum2 = 0;
      for (double time = 0; time < 24; time += 1) {
        double sunaltitude =
          SolarModel.getSunAltitudeAngle(time, panelLatitude, day);
        if (sunaltitude >= 0) {
          double sunazimuth =
            SolarModel
                    .getSunAzinuthAngle(sunaltitude, day, panelLatitude, time);

          double inci =
            SolarModel.getIncidenceAngle(sunaltitude, sunazimuth, 180, tilt);
          double airmass = IrradianceModel.getAirMass(sunaltitude);

          double T0 = IrradianceModel.getT0(airmass, 3);
          double Tr = IrradianceModel.getTr(airmass);
          double aw = IrradianceModel.getaw(airmass, 0.2, 300);
          double f = IrradianceModel.getf(sunaltitude);

          double dir =
            IrradianceModel.getDirectIrradiance(sunaltitude, 1367, T0, Tr, aw,
                                                0.95);
          double dif =
            IrradianceModel.getDiffuseIrradiance(sunaltitude, 1367, T0, Tr, aw,
                                                 0.95, 0.95, f);
          dir =
            IrradianceModel.getCloudModifiedIrradiance(dir, 0, 0.2, 0.95, 0.33,
                                                       -1.06, 0, 0);
          dif =
            IrradianceModel.getCloudModifiedIrradiance(dif, 0, 0.2, 0.95, 0.33,
                                                       -1.06, 0, 0);

          double res;
          if (inci <= 90) {
            res =
              IrradianceModel.getIrradiancOnTiltedPlane(dir, dif, inci,
                                                        sunaltitude, tilt, 0.2);
          }
          else {
            res =
              IrradianceModel.getIrradiancOnTiltedPlane(0, dif, inci,
                                                        sunaltitude, tilt, 0.2);
            // System.out.println("Incidence:" + inci +"for tilt " +
            // tilt + " and hour " + time);
            // System.out.println("Sun's posistion: " + sunaltitude
            // + "," + sunazimuth);
          }

          outsum = outsum + res / 1000;
          outsum2 = outsum2 + (dir + dif) / 1000;
          pw2.printf("%f,%f%n", tilt * 24 + time, res);

        }
        else {
          pw2.printf("%f,%f%n", tilt * 24 + time, 0.0);
        }
      }
      pw.printf("%f,%f,%f%n", tilt, outsum, outsum2);
    }
    pw.close();
    pw2.close();
  }

  @Test
  public void dataYearIrradiance () throws FileNotFoundException
  {

    PrintWriter pw = new PrintWriter(new File("dataYearIrradiance.txt"));

    double panelLatitude = 45;
    for (int day = 1; day <= 365; day += 1) {
      double outsum = 0;
      for (double time = 0; time < 24; time += 1) {
        double sunaltitude =
          SolarModel.getSunAltitudeAngle(time, panelLatitude, day);
        if (sunaltitude >= 0) {
          // double sunazimuth =
          // SolarModel.getSunAzinuthAngle(sunaltitude, day,
          // panelLatitude, time);

          // double inci = SolarModel.getIncidenceAngle(sunaltitude,
          // sunazimuth, 180 , tilt);
          double airmass = IrradianceModel.getAirMass(sunaltitude);

          double T0 = IrradianceModel.getT0(airmass, 3);
          double Tr = IrradianceModel.getTr(airmass);
          double aw = IrradianceModel.getaw(airmass, 0.2, 300);
          double f = IrradianceModel.getf(sunaltitude);

          double dir =
            IrradianceModel.getDirectIrradiance(sunaltitude, 1367, T0, Tr, aw,
                                                0.95);
          double dif =
            IrradianceModel.getDiffuseIrradiance(sunaltitude, 1367, T0, Tr, aw,
                                                 0.95, 0.95, f);
          dir =
            IrradianceModel.getCloudModifiedIrradiance(dir, 0, 0.2, 0.95, 0.33,
                                                       -1.06, 0, 0);
          dif =
            IrradianceModel.getCloudModifiedIrradiance(dif, 0, 0.2, 0.95, 0.33,
                                                       -1.06, 0, 0);

          double res = dir + dif;
          // if(inci <= 90){
          // res = IrradianceModel.getIrradiancOnTiltedPlane(dir, dif,
          // inci, sunaltitude, tilt, 0.2);
          // }else{
          // res = IrradianceModel.getIrradiancOnTiltedPlane(0, dif,
          // inci, sunaltitude, tilt, 0.2);
          // }

          outsum = outsum + res / 1000;

        }
      }
      pw.printf("%d,%f%n", day, outsum);

    }
    pw.close();
  }

  @Test
  public void dataYearIrradianceOnTitledPlane () throws FileNotFoundException
  {

    PrintWriter pw =
      new PrintWriter(new File("dataYearIrradianceOnTitledPlane.txt"));
    PrintWriter debug = new PrintWriter(new File("debug.txt"));

    double panelLatitude = 45;
    double panelTit = 80;
    for (int day = 1; day <= 365; day += 1) {
      double outsum = 0;
      double outsum2 = 0;
      for (double time = 0; time < 24; time += 0.1) {
        double sunaltitude =
          SolarModel.getSunAltitudeAngle(time, panelLatitude, day);
        if (sunaltitude >= 0) {
          double sunazimuth =
            SolarModel
                    .getSunAzinuthAngle(sunaltitude, day, panelLatitude, time);

          double inci =
            SolarModel
                    .getIncidenceAngle(sunaltitude, sunazimuth, 180, panelTit);
          double airmass = IrradianceModel.getAirMass(sunaltitude);

          double T0 = IrradianceModel.getT0(airmass, 3);
          double Tr = IrradianceModel.getTr(airmass);
          double aw = IrradianceModel.getaw(airmass, 0.2, 300);
          double f = IrradianceModel.getf(sunaltitude);

          double dir =
            IrradianceModel.getDirectIrradiance(sunaltitude, 1367, T0, Tr, aw,
                                                0.95);
          double dif =
            IrradianceModel.getDiffuseIrradiance(sunaltitude, 1367, T0, Tr, aw,
                                                 0.95, 0.95, f);
          dir =
            IrradianceModel.getCloudModifiedIrradiance(dir, 0, 0.2, 0.95, 0.33,
                                                       -1.06, 0, 0);
          dif =
            IrradianceModel.getCloudModifiedIrradiance(dif, 0, 0.2, 0.95, 0.33,
                                                       -1.06, 0, 0);

          double res = dir + dif;
          if (inci <= 90) {
            res =
              IrradianceModel.getIrradiancOnTiltedPlane(dir, dif, inci,
                                                        sunaltitude, panelTit,
                                                        0.2);
          }
          else {
            res =
              IrradianceModel.getIrradiancOnTiltedPlane(0, dif, inci,
                                                        sunaltitude, panelTit,
                                                        0.2);
          }

          outsum = outsum + res / 10000;
          outsum2 = outsum2 + (dir + dif) / 10000;

          if (day == 113 || day == 114) {
            debug.printf("%d,%f,%f,%f,%f,%f,%f,%f%n", day, time, dir, dif, res,
                         inci, sunazimuth, sunaltitude, airmass);
          }

        }
      }
      pw.printf("%d,%f,%f%n", day, outsum, outsum2);

    }
    pw.close();
    debug.close();
  }

  @Test
  public void dataTrSpline () throws FileNotFoundException
  {
    double[] airmass =
      { 0.5, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0,
       10.0, 30.0, 40 };
    double[] Tr =
      { 0.9385, 0.8973, 0.8830, 0.8696, 0.8572, 0.8455, 0.8344, 0.7872, 0.7673,
       0.7493, 0.7328, 0.7177, 0.7037, 0.6907, 0.6108, 0.4364, 0.41 };

    assert (Tr.length == airmass.length);

    PolynomialSplineFunction spline =
      new SplineInterpolator().interpolate(airmass, Tr);
    PrintWriter pw = new PrintWriter(new File("dataTrSpline.txt"));
    for (double i = 0.5; i < 40.0; i += 0.1) {
      pw.printf("%f,%f%n", i, spline.value(i));
    }
    pw.close();
  }
}
