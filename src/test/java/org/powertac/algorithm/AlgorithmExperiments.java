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
package org.powertac.algorithm;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
public class AlgorithmExperiments
{
  WeatherReport report = new WeatherReport(0, 0, 0, 0, 0);

  @Before
  public void setUp ()
  {
    Competition.newInstance("tests");
  }

  public void addProducers (int count, double co2em, double cost,
                            double capacity, List<Producer> workSet)
  {
    for (int i = 0; i < count; i++) {
      Producer prod = new SteamPlant(5000, 0.001, capacity);
      prod.setCo2Emissions(co2em);
      prod.setCostPerKw(cost);
      workSet.add(prod);
    }
  }

  @Test
  public void testIterations () throws Exception
  {
    List<Producer> workSet = new ArrayList<Producer>();
    addProducers(20, 0, 0.03, -5000, workSet);
    addProducers(20, 1.5, 0.1, -4000, workSet);
    addProducers(20, 0, 0.14, -2000, workSet);
    addProducers(20, 0, 0.08, -3000, workSet);

    List<Producer> perfectSol = new ArrayList<Producer>();
    for (int i = 0; i < 20; i++) {
      perfectSol.add(workSet.get(i));

    }

    EnergyConstraint enrgCon =
      new EnergyConstraint(workSet, -20 * 5000 - 1, report);
    EnrgEmsMinFunction minFun =
      new EnrgEmsMinFunction(-20 * 5000 - 1, 16, 1, 1, report, workSet);

    AntColonyOptimizationSS<Producer> algo =
      new AntColonyOptimizationSS<Producer>(workSet, enrgCon, minFun, 1, 2,
                                            0.99, 10, 0.1, 10);

    double bestGrade = minFun.gradeSolution(perfectSol);
    Double[][] diffs = new Double[40][10];
    System.out.println(bestGrade);
    for (int j = 0; j < 40; j++) {
      algo.setMaxIterations(j + 1);
      for (int i = 0; i < 10; i++) {
        List<Producer> sol = algo.execute();
        diffs[j][i] = bestGrade - minFun.gradeSolution(sol);
      }
      System.out.print(j);
    }
    System.out.println();
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/algo_iterations.txt");
    for (int j = 0; j < 40; j++) {
      for (int i = 0; i < 10; i++) {
        pw.print(diffs[j][i]);
        pw.print(',');
      }
      pw.println();
    }
    pw.close();
  }
  
  @Test
  public void testAntNumber () throws Exception
  {
    List<Producer> workSet = new ArrayList<Producer>();
    addProducers(20, 0, 0.03, -5000, workSet);
    addProducers(20, 1.5, 0.1, -4000, workSet);
    addProducers(20, 0, 0.14, -2000, workSet);
    addProducers(20, 0, 0.08, -3000, workSet);

    List<Producer> perfectSol = new ArrayList<Producer>();
    for (int i = 0; i < 20; i++) {
      perfectSol.add(workSet.get(i));

    }

    EnergyConstraint enrgCon =
      new EnergyConstraint(workSet, -20 * 5000 - 1, report);
    EnrgEmsMinFunction minFun =
      new EnrgEmsMinFunction(-20 * 5000 - 1, 16, 1, 1, report, workSet);

    AntColonyOptimizationSS<Producer> algo =
      new AntColonyOptimizationSS<Producer>(workSet, enrgCon, minFun, 1, 2,
                                            0.99, 10, 0.1, 10);

    double bestGrade = minFun.gradeSolution(perfectSol);
    Double[][] diffs = new Double[40][10];
    System.out.println(bestGrade);
    for (int j = 0; j < 40; j++) {
      algo.setAntNum(j+1);
      for (int i = 0; i < 10; i++) {
        List<Producer> sol = algo.execute();
        diffs[j][i] = bestGrade - minFun.gradeSolution(sol);
      }
      System.out.print(j);
    }
    System.out.println();
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/algo_ant_num.txt");
    for (int j = 0; j < 40; j++) {
      for (int i = 0; i < 10; i++) {
        pw.print(diffs[j][i]);
        pw.print(',');
      }
      pw.println();
    }
    pw.close();
  }

  @Test
  public void testEvapRate () throws Exception
  {
    List<Producer> workSet = new ArrayList<Producer>();
    addProducers(20, 0, 0.03, -5000, workSet);
    addProducers(20, 1.5, 0.1, -4000, workSet);
    addProducers(20, 0, 0.14, -2000, workSet);
    addProducers(20, 0, 0.08, -3000, workSet);

    List<Producer> perfectSol = new ArrayList<Producer>();
    for (int i = 0; i < 20; i++) {
      perfectSol.add(workSet.get(i));

    }

    EnergyConstraint enrgCon =
      new EnergyConstraint(workSet, -20 * 5000 - 1, report);
    EnrgEmsMinFunction minFun =
      new EnrgEmsMinFunction(-20 * 5000 - 1, 16, 1, 1, report, workSet);

    AntColonyOptimizationSS<Producer> algo =
      new AntColonyOptimizationSS<Producer>(workSet, enrgCon, minFun, 1, 1,
                                            0.99, 10, 0.1, 10);

    double bestGrade = minFun.gradeSolution(perfectSol);
    Double[][] diffs = new Double[20][20];
    System.out.println(bestGrade);
    for (int j = 0; j < 20; j++) {
      algo.setEvapRate(1-(j+1)*0.05);
      for (int i = 0; i < 20; i++) {
        List<Producer> sol = algo.execute();
        diffs[j][i] = bestGrade - minFun.gradeSolution(sol);
      }
      System.out.print(j);
    }
    System.out.println();
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/algo_evap_rate.txt");
    for (int j = 0; j < 20; j++) {
      for (int i = 0; i < 20; i++) {
        pw.print(diffs[j][i]);
        pw.print(',');
      }
      pw.println();
    }
    pw.close();
  }
  
  @Test
  public void testMinPher () throws Exception
  {
    List<Producer> workSet = new ArrayList<Producer>();
    addProducers(20, 0, 0.03, -5000, workSet);
    addProducers(20, 1.5, 0.1, -4000, workSet);
    addProducers(20, 0, 0.14, -2000, workSet);
    addProducers(20, 0, 0.08, -3000, workSet);

    List<Producer> perfectSol = new ArrayList<Producer>();
    for (int i = 0; i < 20; i++) {
      perfectSol.add(workSet.get(i));

    }

    EnergyConstraint enrgCon =
      new EnergyConstraint(workSet, -20 * 5000 - 1, report);
    EnrgEmsMinFunction minFun =
      new EnrgEmsMinFunction(-20 * 5000 - 1, 16, 1, 1, report, workSet);

    AntColonyOptimizationSS<Producer> algo =
      new AntColonyOptimizationSS<Producer>(workSet, enrgCon, minFun, 1, 1,
                                            0.99, 10, 0.1, 10);

    double bestGrade = minFun.gradeSolution(perfectSol);
    Double[][] diffs = new Double[20][20];
    System.out.println(bestGrade);
    for (int j = 0; j < 20; j++) {
      algo.setTmin((j+1)*0.1);
      for (int i = 0; i < 20; i++) {
        List<Producer> sol = algo.execute();
        diffs[j][i] = bestGrade - minFun.gradeSolution(sol);
      }
      System.out.print(j);
    }
    System.out.println();
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/algo_min_pher.txt");
    for (int j = 0; j < 20; j++) {
      for (int i = 0; i < 20; i++) {
        pw.print(diffs[j][i]);
        pw.print(',');
      }
      pw.println();
    }
    pw.close();
  }
  
  @Test
  public void testMaxPher () throws Exception
  {
    List<Producer> workSet = new ArrayList<Producer>();
    addProducers(20, 0, 0.03, -5000, workSet);
    addProducers(20, 1.5, 0.1, -4000, workSet);
    addProducers(20, 0, 0.14, -2000, workSet);
    addProducers(20, 0, 0.08, -3000, workSet);

    List<Producer> perfectSol = new ArrayList<Producer>();
    for (int i = 0; i < 20; i++) {
      perfectSol.add(workSet.get(i));

    }

    EnergyConstraint enrgCon =
      new EnergyConstraint(workSet, -20 * 5000 - 1, report);
    EnrgEmsMinFunction minFun =
      new EnrgEmsMinFunction(-20 * 5000 - 1, 16, 1, 1, report, workSet);

    AntColonyOptimizationSS<Producer> algo =
      new AntColonyOptimizationSS<Producer>(workSet, enrgCon, minFun, 1, 1,
                                            0.99, 10, 0.1, 10);

    double bestGrade = minFun.gradeSolution(perfectSol);
    Double[][] diffs = new Double[20][20];
    System.out.println(bestGrade);
    for (int j = 0; j < 20; j++) {
      algo.setTmax((j+1));
      for (int i = 0; i < 20; i++) {
        List<Producer> sol = algo.execute();
        diffs[j][i] = bestGrade - minFun.gradeSolution(sol);
      }
      System.out.print(j);
    }
    System.out.println();
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/algo_max_pher.txt");
    for (int j = 0; j < 20; j++) {
      for (int i = 0; i < 20; i++) {
        pw.print(diffs[j][i]);
        pw.print(',');
      }
      pw.println();
    }
    pw.close();
  }
}
