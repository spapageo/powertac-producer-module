package org.powertac.producer.fossil;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.Competition;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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

}
