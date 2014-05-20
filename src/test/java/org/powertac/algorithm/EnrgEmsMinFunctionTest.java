package org.powertac.algorithm;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.Competition;
import org.powertac.common.WeatherReport;
import org.powertac.producer.Producer;
import org.powertac.producer.fossil.SteamPlant;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
@DirtiesContext
public class EnrgEmsMinFunctionTest
{

  @Test
  public void testGradeProducer ()
  {
    Competition.newInstance("producer-test");
    List<Producer> workSet = new ArrayList<Producer>();
    for (int i = 1000; i < 10000; i += 1000) {
      workSet.add(new SteamPlant(5000, 0.1, -i));
    }
    EnrgEmsMinFunction fun = new EnrgEmsMinFunction(-10000, 1, 1, 1, new WeatherReport(0, 0, 0, 0, 0), workSet);
    
    assertEquals(fun.gradeItem(workSet.get(0)),1.6,0); // 0.1*1 + 1.5*1 = 1.6
    
  }
  
  @Test
  public void testGradeSolution ()
  {
    Competition.newInstance("producer-test");
    List<Producer> workSet = new ArrayList<Producer>();
    for (int i = 1000; i < 10000; i += 1000) {
      workSet.add(new SteamPlant(5000, 0.1, -i));
    }
    EnrgEmsMinFunction fun = new EnrgEmsMinFunction(-3000, 1, 1, 1, new WeatherReport(0, 0, 0, 0, 0), workSet);
    
    List<Producer> solution = new ArrayList<Producer>();
    solution.add(workSet.get(0));
    solution.add(workSet.get(1));

    assertEquals(fun.gradeSolution(solution),1.6,0.1); // 0.1*1 + 1.5*1 = 1.6

  }

}
