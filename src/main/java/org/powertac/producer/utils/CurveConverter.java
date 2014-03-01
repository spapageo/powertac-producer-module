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

import java.util.Iterator;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Serializes and deserializes the {@link Curve} class.
 * 
 * @author Doom
 * 
 */
public class CurveConverter implements Converter
{

  @SuppressWarnings("rawtypes")
  @Override
  public boolean canConvert (Class type)
  {
    return type.equals(Curve.class);
  }

  @Override
  public void marshal (Object source, HierarchicalStreamWriter writer,
                       MarshallingContext context)
  {
    Curve c = (Curve) source;
    if (c.getCanBeNegative() == false)
      writer.addAttribute("canBeNegative", String.valueOf(c.getCanBeNegative()));
    if (!c.getProlongFirstValue()) {
      writer.addAttribute("first-value",
                          String.valueOf(c.getCustomFirstValue()));
    }
    if (!c.getProlongLastValue()) {
      writer.addAttribute("last-value", String.valueOf(c.getCustomLastValue()));
    }
    Iterator<Double> it = c.xy.keySet().iterator();

    while (it.hasNext()) {
      double x = it.next();
      writer.startNode("entry");
      writer.addAttribute("x", String.valueOf(x));
      writer.setValue(String.valueOf(c.xy.get(x)));
      writer.endNode();
    }

  }

  @Override
  public Object unmarshal (HierarchicalStreamReader reader,
                           UnmarshallingContext context)
  {
    Curve c = new Curve();
    if (reader.getAttribute("canBeNegative") != null)
      c.setCanBeNegative(false);

    if (reader.getAttribute("first-value") != null)
      c.setCustomFirstValue(Double.parseDouble(reader
              .getAttribute("first-value")));

    if (reader.getAttribute("last-value") != null)
      c.setCustomLastValue(Double.parseDouble(reader.getAttribute("last-value")));

    while (reader.hasMoreChildren()) {
      reader.moveDown();
      if (reader.getNodeName().equals("entry")) {
        double x = Double.parseDouble(reader.getAttribute("x"));
        double y = Double.parseDouble(reader.getValue());
        c.add(x, y);
      }
      reader.moveUp();
    }

    return c;
  }

}
