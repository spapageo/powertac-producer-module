package org.powertac.producer.windfarm;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Random;

import org.junit.Test;
import org.powertac.producer.utils.Curve;

public class WindTurbineTest2
{
  @Test
  public void dataGetPowerOutput () throws IOException
  {
    double[] x =
      { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
        21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
    double[] y =
      { 0, -4, -45, -128, -248, -364, -497, -792, -1203, -2000, -2262,
       -2337, -2451, -2560, -2640, -2751, -2765, -2817, -2850, -2950, -2900, -2900,
       -2900, -2900, -2990, -2900, -2900, -2990, -2900, -2900, -2990, -2900};

    Curve c = new Curve(x, y);
    WindTurbine wt = new WindTurbine(22, 2, -3000, 10, c);
    wt.setRefAltitude(10);
    
    File file = new File("data/turbinemodel.txt");
    file.createNewFile();
    PrintWriter pw = new PrintWriter(new File("data/turbinemodel.txt"));

    Random r = new Random();
    //wt.setKappa(2);
    for (double speed = 0; speed < 20.01; speed += 1) {
//      double f = WindTurbine.calulcatef(22);
//      double ua =
//        WindTurbine.calculateUasterisk(speed, wt.getRefAltitude(), f,
//                                       wt.getSurfaceRoughness(), 0.4);
//      double correctedHourlySpeed =
//        WindTurbine.calculateWindAtAltitude(wt.getHubHeigth(),
//                                            wt.getSurfaceRoughness(), ua, f,
//                                            0.4);

      for (int i = 0; i < 100; i++) {
        double power = wt.getPowerOutput(r.nextDouble()*30 + 253.15, speed);
        pw.printf(Locale.UK,"%f,%f%n", speed, -power);
      }
    }

    pw.close();

  }
}
