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
public class EnergyConstraintTest
{

  @Test
  public void testInitializeCandidates ()
  {
    Competition.newInstance("producer-test");
    List<Producer> workSet = new ArrayList<Producer>();
    for (int i = 999; i < 10000; i += 1000) {
      workSet.add(new SteamPlant(5000, 0.1, -i));
    }
    List<Producer> solution = new ArrayList<Producer>();
    solution.add(workSet.get(0));

    EnergyConstraint enrgcon =
      new EnergyConstraint(workSet, -5000, new WeatherReport(0, 0, 0, 0, 0));

    List<Producer> candidates = enrgcon.initializeCandidates(solution);

    assertTrue(candidates.size() == 3);
  }

  @Test
  public void testUpdateCandidates ()
  {
    Competition.newInstance("producer-test");
    List<Producer> workSet = new ArrayList<Producer>();
    for (int i = 999; i < 10000; i += 1000) {
      workSet.add(new SteamPlant(5000, 0.1, -i));
    }
    List<Producer> solution = new ArrayList<Producer>();
    solution.add(workSet.get(0));

    EnergyConstraint enrgcon =
      new EnergyConstraint(workSet, -6000, new WeatherReport(0, 0, 0, 0, 0));

    List<Producer> candidates = enrgcon.initializeCandidates(solution);
    assertTrue(candidates.size() == 4);

    solution.add(workSet.get(2));
    candidates = enrgcon.updateCandidates(solution, candidates);
    
    assertTrue(candidates.size() == 1);
    assertTrue(candidates.get(0) == workSet.get(1));

  }

}
