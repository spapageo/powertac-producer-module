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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import org.junit.Test;
import org.powertac.producer.pvfarm.ElectricalModel;

public class ElectricalModelTest
{

  @Test
  public void testEfficienncyTemperatureIrradiance () throws IOException
  {
    File file = new File("data/dataEfficienncyTemperatureIrradiance.txt");
    file.createNewFile();
    PrintWriter pw =
      new PrintWriter("data/dataEfficienncyTemperatureIrradiance.txt");
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
        pw.printf(Locale.UK,"%f,%f,%f%n", irradiance, temperature, output);
        // System.out.println(temperature+","+panelTemp+","+irradiance);
      }
    }
    pw.close();
  }

}
