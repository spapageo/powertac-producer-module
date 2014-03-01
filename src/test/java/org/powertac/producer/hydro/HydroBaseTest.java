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
public class HydroBaseTest
{

  @Test
  public void testGetOutput ()
  {
    Curve efficiency = new Curve();
    efficiency.add(0, 0.5);
    efficiency.add(0.5, 0.5);
    efficiency.add(1, 0.5);

    Curve flow = new Curve();
    flow.add(1, 9);
    flow.add(182, 9);
    flow.add(365, 9);

    Competition.newInstance("Generate output test");
    RunOfRiver river = new RunOfRiver(flow, 2, 12, efficiency, 0, 50, 1, -100);
    double out = river.getOutput(1);
    assertEquals(-0.5 * 9.80665 * 999.972 * 9 * 50 / 1000, out, 50);
  }
}
