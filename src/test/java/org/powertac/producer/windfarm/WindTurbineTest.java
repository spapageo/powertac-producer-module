package org.powertac.producer.windfarm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.junit.Test;
import org.powertac.common.RandomSeed;
import org.powertac.producer.utils.Curve;
import org.powertac.producer.windfarm.WindTurbine;

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

    for (double altitude = 5; altitude <= 500; altitude++) {
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
      { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
       21, 22, 23, 24, 25 };
    double[] y =
      { 0, 0, 0, 66, 166, 288, 473, 709, 1000, 1316, 1651, 1860, 1968, 2000,
       2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000 };

    Curve c = new Curve(x, y);
    c.setCanBeNegative(false);
    c.setCustomLastValue(0);
    WindTurbine wt =
      new WindTurbine(new RandomSeed("WindTurbineTest", 1, " "), 22, 0.01,
                      -2000, 80, c);
    PrintWriter pw = new PrintWriter(new File("dataGetPowerOutput.txt"));

    for (double speed = 1; speed < 25; speed += 0.1) {
      double f = WindTurbine.calulcatef(22);
      double ua =
        WindTurbine.calculateUasterisk(speed, wt.getRefAltitude(), f,
                                       wt.getSurfaceRoughness(), 0.4);
      double correctedHourlySpeed =
        WindTurbine.calculateWindAtAltitude(wt.getHubHeigth(),
                                            wt.getSurfaceRoughness(), ua, f,
                                            0.4);

      for (int i = 0; i < 10; i++) {
        pw.printf("%f,%f,%f%n", correctedHourlySpeed,
                  -wt.getPowerOutput(298.15, speed),
                  c.value(correctedHourlySpeed));
      }
    }

    pw.close();

  }

}
