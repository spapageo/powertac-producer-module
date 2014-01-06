package org.powertac.producer.pvfarm;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.junit.Test;
import org.powertac.producer.pvfarm.ElectricalModel;

public class ElectricalModelTest
{

  @Test
  public void testEfficienncyTemperatureIrradiance ()
    throws FileNotFoundException
  {
    PrintWriter pw =
      new PrintWriter(new File("dataEfficienncyTemperatureIrradiance.txt"));
    for (double irradiance = 200; irradiance < 1200; irradiance += 50) {
      for (double temperature = 273; temperature < 353; temperature += 5) {
        double panelTemp =
          ElectricalModel.getPanelTemperature(temperature, 3.0, irradiance);
        double reflectiveLosses =
          ElectricalModel.getReflectiveLossCoeff(0, 0.2);
        assertTrue(reflectiveLosses >= 0 && reflectiveLosses <= 1);
        double thermal =
          ElectricalModel.getThermalLossCoeff(0.0045, panelTemp, 293);
        assertTrue(thermal >= 0 && thermal <= 1);

        double output =
          ElectricalModel.getElectricalOutput(0.20, 0.75, thermal,
                                              reflectiveLosses, 1, 1);
        assertTrue(output >= 0 && output <= 1);
        pw.printf("%f,%f,%f%n", irradiance, temperature, output);
        // System.out.println(temperature+","+panelTemp+","+irradiance);
      }
    }
    pw.close();
  }

}
