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
package org.powertac.producer.utils;

import static org.junit.Assert.*;

import org.junit.Test;
import org.powertac.producer.utils.Curve;

import com.thoughtworks.xstream.XStream;

public class CurveTest
{

  @Test
  public void testDefaultConstr ()
  {
    Curve c = new Curve();

    c.add(1, 5);
    c.add(2, 10);
    c.add(3, 25);
    c.add(4, 20);
    c.add(5, 5);

    assertTrue(c.value(2) == 10);
    assertTrue(c.value(3) == 25);
    assertTrue(c.value(2.5) < 25);
    assertTrue(c.value(6) == 5);
    assertTrue(c.value(0) == 5);
  }

  @Test
  public void testConstr ()
  {
    double[] x = { 1, 2, 3, 4, 5 };
    double[] y = { 5, 10, 25, 20, 5 };

    Curve c = new Curve(x, y);

    assertTrue(c.value(2) == 10);
    assertTrue(c.value(3) == 25);
    assertTrue(c.value(2.5) < 25);
    assertTrue(c.value(6) == 5);
    assertTrue(c.value(0) == 5);
  }

  @Test
  public void testIsInversible ()
  {
    double[] x = { 1, 2, 3, 4, 5 };
    double[] y = { 5, 10, 25, 20, 5 };

    Curve c = new Curve(x, y);

    assertFalse(c.isInvertible());

    double[] x2 = { 1, 2, 3, 4, 5 };
    double[] y2 = { 5, 10, 25, 25, 50 };

    c = new Curve(x2, y2);

    assertFalse(c.isInvertible());

    double[] x3 = { 1, 2, 3, 4, 5 };
    double[] y3 = { 5, 10, 25, 40, 60 };

    c = new Curve(x3, y3);

    assertTrue(c.isInvertible());
  }

  @Test
  public void testGetInvertiblePart ()
  {
    double[] x = { 1, 2, 3, 4, 5 };
    double[] y = { 5, 10, 25, 20, 5 };

    Curve c = new Curve(x, y);
    Curve inv = c.getInvertiblePart();

    assertTrue(inv.value(25) == 3);
    assertTrue(inv.value(28) == 3);
  }

  @Test
  public void testSerialization ()
  {
    double[] x = { 1, 1.5, 3, 4, 5 };
    double[] y = { 5, 10, 25, 20, 5 };

    Curve c = new Curve(x, y);

    XStream xstr = new XStream();
    xstr.autodetectAnnotations(true);
    String out = xstr.toXML(c);
    c = (Curve) xstr.fromXML(out);
    assertTrue(c.needRebuild);
    assertTrue(c.value(1.5) == 10);
    assertTrue(c.getFirstX() == 1);
    assertTrue(c.getLastX() == 5);
  }
}
