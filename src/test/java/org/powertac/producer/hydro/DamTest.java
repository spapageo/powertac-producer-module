package org.powertac.producer.hydro;

import static org.junit.Assert.*;

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
  
  @Test
  public void dataGenerateXml() throws IOException{
    Curve volume = new Curve();
    volume.add(0,0);
    volume.add(100000000.0, 56.4);
    volume.add(300000000.0, 95.7);
    volume.add(400000000.0, 105);
    volume.add(600000000.0, 123);
    volume.add(800000000.0, 135);
    volume.value(5000);
    
    double[] x = {0.10,0.11,0.12,0.13,0.14,0.15,0.16,0.17,0.18,0.19,0.20,0.21,
                  0.22,0.23,0.24,0.25,0.26,0.27,0.28,0.29,0.30,0.31,0.32,0.33,
                  0.34,0.35,0.36,0.37,0.38,0.39,0.40,0.41,0.42,0.43,0.44,0.45,
                  0.46,0.47,0.48,0.49,0.50,0.51,0.52,0.53,0.54,0.55,0.56,0.57,
                  0.58,0.59,0.60,0.61,0.62,0.63,0.64,0.65,0.66,0.67,0.68,0.69,
                  0.70,0.71,0.72,0.73,0.74,0.75,0.76,0.77,0.78,0.79,0.80,0.81,
                  0.82,0.83,0.84,0.85,0.86,0.87,0.88,0.89,0.90,0.91,0.92,0.93,
                  0.94,0.95,0.96,0.97,0.98,0.99,1};
    double[] y = {54,56.7510059257987,59.3427468223978,61.7806965819306,
                64.0703290965304,66.2171182583305,68.2265379594641,
                70.1040620920646,71.8551645482652,73.4853192201992,
                75,76.4046807798008,77.7048354517348,78.9059379079354,
                80.0134620405359,81.0328817416695,81.9696709034696,
                82.8293034180694,83.6172531776022,84.3389940742013,
                85,85.6052709549983,86.1579113706630,86.6605517863277,
                87.1158227413260,87.5263547749914,87.8947784266575,
                88.2237242356579,88.5158227413260,88.7737044829955,
                89,89.1972354002061,89.3675190656132,89.5128549467537,
                89.6352469941601,89.7366991583648,89.8192153899004,
                89.8847996392992,89.9354558570938,89.9731879938166,
                90,90.0177874441773,90.0280123668842,90.0320284266575,
                90.0311892820337,90.0268485915493,90.0203600137410,
                90.0130772071453,90.0063538302989,90.0015435417382,90,
                90.0026148230849,90.0084314668499,90.0160313466163,
                90.0239958777053,90.0309064754380,90.0353445551357,
                90.0358915321196,90.0311288217108,90.0196378392305,90,
                89.9712532634833,89.9342617657162,89.8903461868774,
                89.8408272071453,89.7870255066987,89.7302617657163,
                89.6718566643765,89.6131308828581,89.5554051013397,
                89.5000000000000,89.4478721229818,89.3985214702851,
                89.3510839058743,89.3046952937135,89.2584914977671,
                89.2116083819993,89.1631818103744,89.1123476468568,
                89.0582417554105,89,88.9367582445895,88.8676523531433,
                88.7918181896256,88.7083916180007,88.6165085022329,
                88.5153047062865,88.4039160941257,88.2814785297149,
                88.1471278770182,88 };
    Curve efficiency = new Curve();
    
    for(int i = 0;i < x.length; i++){
      efficiency.add(x[i], y[i]);
    }
    efficiency.value(0.5);
    
    Curve inputFlow = new Curve();
    inputFlow.add(1, 420);
    inputFlow.add(60,400);
    inputFlow.add(90,480);
    inputFlow.add(120,700);
    inputFlow.add(150,1200);
    inputFlow.add(180,1800);
    inputFlow.add(210,2000);
    inputFlow.add(240,2000);
    inputFlow.add(270,2000);
    inputFlow.add(300,1800);
    inputFlow.add(330,1100);
    inputFlow.add(365,600);
    inputFlow.value(125);
    Competition.newInstance("Inverse curve test");
    Dam dam = new Dam(inputFlow, 100, 600, efficiency, volume, 600000000.0, -500000, 0.95);
    FileWriter fw = new FileWriter("data/dam.xml");
    XStream xstream = new XStream();
    xstream.autodetectAnnotations(true);
    xstream.toXML(dam, fw);
    fw.close();
    
  }

}
