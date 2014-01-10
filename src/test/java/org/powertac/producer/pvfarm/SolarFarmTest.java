package org.powertac.producer.pvfarm;

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
public class SolarFarmTest
{

  @Test
  public void dataGenerateXML() throws IOException
  {
    Competition.newInstance("Solar famr xml");
    SolarFarm farm = new SolarFarm();
    PvPanel panel = new PvPanel(10, 22, 22, 180, 45, 0.35, -2);
    
    //100 panels
    for(int i = 0;i < 100; i++)
      farm.addPanel(panel);
    
    FileWriter fw = new FileWriter("data/solar-farm.xml");
    XStream xstream = new XStream();
    xstream.autodetectAnnotations(true);
    xstream.toXML(farm, fw);
    
    fw.close();
  }

}
