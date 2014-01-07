package org.powertac.producer.windfarm;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.junit.Test;
import org.powertac.producer.utils.Curve;
import org.powertac.producer.windfarm.WindTurbine;

import com.thoughtworks.xstream.XStream;

public class WindTurbineTest
{

  @Test
  public void dataCalculateWindAtAltitude () throws FileNotFoundException
  {

    PrintWriter pw =
            new PrintWriter(new File("dataCalculateWindAtAltitude.txt"));

    double[] zlist = { 0.01, 1, 2 };
    double refAltitude = 20;
    double refSpeed = 10;
    double f = WindTurbine.calulcatef(22);

    for (double altitude = 5; altitude <= 500; altitude++) {
      double out[] = new double[zlist.length];
      int i = 0;
      for (double z0: zlist) {
        double ua =
                WindTurbine.calculateUasterisk(refSpeed, refAltitude, f, z0, 0.4);

        out[i] = WindTurbine.calculateWindAtAltitude(altitude, z0, ua, f, 0.4);
        i++;
      }
      pw.printf("%f,%f,%f,%f%n", altitude, out[0], out[1], out[2]);
    }
    pw.close();
  }

  @Test
  public void dataCalculateStd () throws FileNotFoundException
  {

    PrintWriter pw = new PrintWriter(new File("dataCalculateStd.txt"));

    double[] zlist = { 0.01, 1, 2 };
    double refAltitude = 20;
    double refSpeed = 10;
    double f = WindTurbine.calulcatef(22);

    for (double altitude = 1; altitude <= 500; altitude++) {
      double out[] = new double[zlist.length];
      int i = 0;
      for (double z0: zlist) {
        double ua =
                WindTurbine.calculateUasterisk(refSpeed, refAltitude, f, z0, 0.4);

        out[i] = WindTurbine.calculateStd(f, ua, altitude, z0);
        i++;
      }
      pw.printf("%f,%f,%f,%f%n", altitude, out[0], out[1], out[2]);
    }
    pw.close();
  }

  @Test
  public void dataGetPowerOutput () throws FileNotFoundException
  {
    double[] x =
          {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25 };
    double[] y =
          { 0, -66, -166, -288, -473, -709, -1000, -1316, -1651, -1860, -1968, -2000,
            -2000, -2000, -2000, -2000, -2000, -2000, -2000, -2000, -2000, -2000, -2000 };

    Curve c = new Curve(x, y);
    c.setCustomLastValue(0);
    WindTurbine wt =
            new WindTurbine( 22, 0.01,-2000, 80, c);
    PrintWriter pw = new PrintWriter(new File("dataGetPowerOutput.txt"));

    for (double speed = 0; speed < 35; speed += 0.1) {
      double f = WindTurbine.calulcatef(22);
      double ua =
              WindTurbine.calculateUasterisk(speed, wt.getRefAltitude(), f,
                                             wt.getSurfaceRoughness(), 0.4);
      double correctedHourlySpeed =
              WindTurbine.calculateWindAtAltitude(wt.getHubHeigth(),
                                                  wt.getSurfaceRoughness(), ua, f,
                                                  0.4);

      for (int i = 0; i < 10; i++) {
        double power = -wt.getPowerOutput(298.15, speed);
        assertTrue(power + " " + speed,power <= 0);
        assertTrue(power != Double.NEGATIVE_INFINITY);
        pw.printf("%f,%f,%f%n", correctedHourlySpeed,
                  power,c.value(correctedHourlySpeed));
      }
    }

    pw.close();

  }

  @Test
  public void testSerialize(){
    double[] x =
          { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25 };
    double[] y =
          { 0, -66, -166, -288, -473, -709, -1000, -1316, -1651, -1860, -1968, -2000,
            -2000, -2000, -2000, -2000, -2000, -2000, -2000, -2000, -2000, -2000, -2000 };

    Curve c = new Curve(x, y);
    c.setCustomLastValue(0);
    WindTurbine wt = new WindTurbine(22, 0.01, -2000, 80, c);
    XStream xstream = new XStream();
    xstream.autodetectAnnotations(true);
    String out = xstream.toXML(wt);
    wt = (WindTurbine) xstream.fromXML(out);
    assertTrue(wt.getRs() == null);
  }

}
