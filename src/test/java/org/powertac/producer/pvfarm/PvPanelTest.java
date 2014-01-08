package org.powertac.producer.pvfarm;

import static org.junit.Assert.*;

import java.util.TimeZone;

import org.junit.Test;

import com.thoughtworks.xstream.XStream;

public class PvPanelTest
{
  @Test
  public void testSerialize ()
  {
    XStream x = new XStream();
    x.autodetectAnnotations(true);
    PvPanel panel = new PvPanel(10, 22, 22, 180, 30, 30, -1);
    String out = x.toXML(panel);
    panel = (PvPanel) x.fromXML(out);
    assertTrue(panel.getCapacity() == -1);
    assertTrue(panel.getPanelArrea() == 10);

  }
  
  @Test
  public void testOutput(){
    PvPanel panel = new PvPanel(1, 22, 22, 180, 45, 0.3, -100);
    long startTime = System.currentTimeMillis();
    double outSum = 0.0;
    for(int i = 0; i < 24; i++){
      outSum += panel.getOutput(startTime,TimeZone.getDefault(), 0, 35, 4);
      startTime += 60*60*1000;
    }
    assertTrue(outSum > -3000);
  }

}
