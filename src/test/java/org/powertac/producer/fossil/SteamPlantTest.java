package org.powertac.producer.fossil;

import static org.junit.Assert.*;

import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.Competition;
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
  public void testGetOutput()
  {
    Competition.newInstance("Fossil Plant test");
    SteamPlant plant = new SteamPlant(10000, 5000, -500000);
    assertEquals(-500000,plant.getOutput(), 10000);
    
    plant = new SteamPlant(10000, 5000, -500000);
    plant.setPreferredOutput(-200000);
    assertEquals(-200000,plant.getOutput(), 100000);
  }
  
  @Test
  public void dataGenerateXML() throws IOException
  {
    Competition.newInstance("Fossil Plant test");
    SteamPlant plant = new SteamPlant(10000, 5000, -500000);
    XStream xstream = new XStream();
    xstream.autodetectAnnotations(true);
    FileWriter fw = new FileWriter("data/steam-plant.xml");
    xstream.toXML(plant,fw);
    fw.close();
  }

}
