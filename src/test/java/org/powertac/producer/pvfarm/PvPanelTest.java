package org.powertac.producer.pvfarm;

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
    System.out.println(out);
  }

}
