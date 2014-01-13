/*******************************************************************************
 * Copyright 2014 Spyridon Papageorgiou
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
package org.powertac.producer.fossil;

import static java.lang.Math.signum;
import static org.junit.Assert.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.Competition;
import org.powertac.producer.utils.Curve;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thoughtworks.xstream.XStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
@DirtiesContext
public class SteamPlantTest
{

  @Test
  public void testGetOutput ()
  {
    Competition.newInstance("Fossil Plant test");
    SteamPlant plant = new SteamPlant(10000, 5000, -500000);
    assertEquals(-500000, plant.getOutput(), 10000);

    plant = new SteamPlant(10000, 5000, -500000);
    plant.setPreferredOutput(-200000);
    assertEquals(-200000, plant.getOutput(), 100000);
  }

  @Test
  public void dataGenerateXML () throws IOException
  {
    Competition.newInstance("Fossil Plant test");
    SteamPlant plant = new SteamPlant(10000, 5000, -500000);
    XStream xstream = new XStream();
    xstream.autodetectAnnotations(true);
    FileWriter fw = new FileWriter("data/steam-plant.xml");
    xstream.toXML(plant, fw);
    fw.close();
  }

  @Test
  public void dataGenerateOutputGraph () throws IOException
  {
    double adjustmentSpeed = 10000;
    double variance = 2000;
    double time =
      (200000 - 500000) / (signum(200000 - 500000) * adjustmentSpeed);

    Curve output = new Curve();
    output.add(0, 500000);
    output.add(time, 200000);
    output.add(time + 10, 200000);
    Random r = new Random();

    PrintWriter fw = new PrintWriter("data/dataFossilOutput.txt");
    for (int i = 0; i < 60; i++) {
      fw.printf("%d,%f%n", i, output.value(i - 5) + r.nextGaussian() * variance);
    }
    fw.close();
  }

}
