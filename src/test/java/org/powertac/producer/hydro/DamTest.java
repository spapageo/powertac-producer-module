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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.Competition;
import org.powertac.producer.utils.Curve;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thoughtworks.xstream.XStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
@DirtiesContext
public class DamTest
{

  @Test
  public void testCalcInverseOutAndOutput ()
  {
    Curve efficiency = new Curve();
    efficiency.add(0, 0.5);
    efficiency.add(0.5, 1);
    efficiency.add(1, 0.5);

    Curve flow = new Curve();
    flow.add(1, 9);
    flow.add(182, 9);
    flow.add(365, 9);

    Curve volume = new Curve();
    volume.add(0, 0);
    volume.add(1000000, 16.5);
    volume.add(3000000, 28);
    volume.add(4000000, 31);
    volume.add(6000000, 36);
    volume.add(8000000, 39.5);

    Competition.newInstance("Inverse curve test");
    Dam dam = new Dam(flow, 1, 9, efficiency, volume, 6000000, -3500, 1);

    Curve invOut = dam.getInvCurveOut();

    assertEquals(4.5, invOut.value(4.5 * 999.972 * 9.80665 * 0.5), 10);

    dam.setPreferredOutput(-9 * 999.972 * 9.80665 * 0.5 * 36 / 1000);

    assertEquals(dam.getOutput(5), -9 * 999.972 * 9.80665 * 0.5 * 36 / 1000,
                 100);

    dam.setPreferredOutput(-4.5 * 999.972 * 9.80665 * 1 * 36 / 1000);

    assertEquals(dam.getOutput(5), -4.5 * 999.972 * 9.80665 * 1 * 36 / 1000,
                 100);
  }

  @Test
  public void dataGenerateXml () throws IOException
  {
    Curve volume = new Curve();
    volume.add(0, 0);
    volume.add(100000000.0, 56.4);
    volume.add(300000000.0, 95.7);
    volume.add(400000000.0, 105);
    volume.add(600000000.0, 123);
    volume.add(800000000.0, 135);
    volume.value(5000);

    double[] x =
      { 0.10, 0.11, 0.12, 0.13, 0.14, 0.15, 0.16, 0.17, 0.18, 0.19, 0.20, 0.21,
       0.22, 0.23, 0.24, 0.25, 0.26, 0.27, 0.28, 0.29, 0.30, 0.31, 0.32, 0.33,
       0.34, 0.35, 0.36, 0.37, 0.38, 0.39, 0.40, 0.41, 0.42, 0.43, 0.44, 0.45,
       0.46, 0.47, 0.48, 0.49, 0.50, 0.51, 0.52, 0.53, 0.54, 0.55, 0.56, 0.57,
       0.58, 0.59, 0.60, 0.61, 0.62, 0.63, 0.64, 0.65, 0.66, 0.67, 0.68, 0.69,
       0.70, 0.71, 0.72, 0.73, 0.74, 0.75, 0.76, 0.77, 0.78, 0.79, 0.80, 0.81,
       0.82, 0.83, 0.84, 0.85, 0.86, 0.87, 0.88, 0.89, 0.90, 0.91, 0.92, 0.93,
       0.94, 0.95, 0.96, 0.97, 0.98, 0.99, 1 };
    double[] y =
      { 0.54, 0.567510059257987, 0.593427468223978, 0.617806965819306,
       0.640703290965304, 0.662171182583305, 0.682265379594641,
       0.701040620920646, 0.718551645482652, 0.734853192201992, 0.75,
       0.764046807798008, 0.777048354517348, 0.789059379079354,
       0.800134620405359, 0.810328817416695, 0.819696709034696,
       0.828293034180694, 0.836172531776022, 0.843389940742013, 0.85,
       0.856052709549983, 0.861579113706630, 0.866605517863277,
       0.871158227413260, 0.875263547749914, 0.878947784266575,
       0.882237242356579, 0.885158227413260, 0.887737044829955, 0.89,
       0.891972354002061, 0.893675190656132, 0.895128549467537,
       0.896352469941601, 0.897366991583648, 0.898192153899004,
       0.898847996392992, 0.899354558570938, 0.899731879938166, 0.90,
       0.900177874441773, 0.900280123668842, 0.900320284266575,
       0.900311892820337, 0.900268485915493, 0.900203600137410,
       0.900130772071453, 0.900063538302989, 0.900015435417382, 0.90,
       0.900026148230849, 0.900084314668499, 0.900160313466163,
       0.900239958777053, 0.900309064754380, 0.900353445551357,
       0.900358915321196, 0.900311288217108, 0.900196378392305, 0.90,
       0.899712532634833, 0.899342617657162, 0.898903461868774,
       0.898408272071453, 0.897870255066987, 0.897302617657163,
       0.896718566643765, 0.896131308828581, 0.895554051013397,
       0.895000000000000, 0.894478721229818, 0.893985214702851,
       0.893510839058743, 0.893046952937135, 0.892584914977671,
       0.892116083819993, 0.891631818103744, 0.891123476468568,
       0.890582417554105, 0.89, 0.889367582445895, 0.888676523531433,
       0.887918181896256, 0.887083916180007, 0.886165085022329,
       0.885153047062865, 0.884039160941257, 0.882814785297149,
       0.881471278770182, 0.88 };
    Curve efficiency = new Curve();

    for (int i = 0; i < x.length; i++) {
      efficiency.add(x[i], y[i]);
    }
    efficiency.value(0.5);

    Curve inputFlow = new Curve();
    inputFlow.add(1, 420);
    inputFlow.add(60, 400);
    inputFlow.add(90, 480);
    inputFlow.add(120, 700);
    inputFlow.add(150, 1200);
    inputFlow.add(180, 1800);
    inputFlow.add(210, 2000);
    inputFlow.add(240, 2000);
    inputFlow.add(270, 2000);
    inputFlow.add(300, 1800);
    inputFlow.add(330, 1100);
    inputFlow.add(365, 600);
    inputFlow.value(125);
    Competition.newInstance("Inverse curve test");
    Dam dam =
      new Dam(inputFlow, 100, 600, efficiency, volume, 600000000.0, -500000,
              0.95);
    new File("data/").mkdir();
    FileWriter fw = new FileWriter("data/dam.xml");
    XStream xstream = new XStream();
    xstream.autodetectAnnotations(true);
    xstream.toXML(dam, fw);
    fw.close();

  }

}
