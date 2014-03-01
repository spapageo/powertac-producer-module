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

import java.util.Calendar;
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
    PvPanel panel = new PvPanel(10, 22, 22, 180, 30, 0.3, -1);
    String out = x.toXML(panel);
    panel = (PvPanel) x.fromXML(out);
    assertTrue(panel.getCapacity() == -1);
    assertTrue(panel.getPanelArea() == 10);

  }

  @Test
  public void testOutput ()
  {
    PvPanel panel = new PvPanel(1, 22, 22, 180, 45, 0.3, -100);
    Calendar cal = Calendar.getInstance();
    double outSum = 0.0;
    for (int i = 0; i < 24; i++) {
      long time = cal.getTimeInMillis();
      outSum += panel.getOutput(time, TimeZone.getDefault(), 0, 308, 4);
      cal.add(Calendar.HOUR, 1);
    }
    assertTrue(outSum > -2);
  }

}
