package org.powertac.producer.hydro;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.Competition;
import org.powertac.producer.utils.Curve;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
@DirtiesContext
public class DamTest
{

  @Test
  public void testCalcInverseOutAndOutput()
  {
    Curve efficiency = new Curve();
    efficiency.add(0, 0.5);
    efficiency.add(0.5,1);
    efficiency.add(1, 0.5);
    
    Curve flow = new Curve();
    flow.add(1, 9);
    flow.add(182, 9);
    flow.add(365, 9);
    
    Curve volume = new Curve();
    volume.add(0,0);
    volume.add(1000000, 16.5);
    volume.add(3000000, 28);
    volume.add(4000000, 31);
    volume.add(6000000, 36);
    volume.add(8000000, 39.5);
    
    Competition.newInstance("Inverse curve test");
    Dam dam = new Dam(flow, 1, 9, efficiency, volume, 6000000, -3500, 1);
    
    Curve invOut = dam.getInvCurveOut();
    
    assertEquals(4.5,invOut.value(4.5*999.972*9.80665*0.5),10);
    
    dam.setPreferredOutput(-9*999.972*9.80665*0.5*36/1000);
    
    assertEquals(dam.getOutput(5),-9*999.972*9.80665*0.5*36/1000,100);
    
    dam.setPreferredOutput(-4.5*999.972*9.80665*1*36/1000);
    
    assertEquals(dam.getOutput(5),-4.5*999.972*9.80665*1*36/1000,100);
  }

}
