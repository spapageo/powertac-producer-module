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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;

import org.powertac.common.WeatherReport;
import org.powertac.producer.Producer;

/**
 * This class implements the Constraint interface as energy constraint of
 * the Producer class so that the candidate's power added to the solution
 * power doesn't exceed the limit.
 * 
 * @author Spyros Papageorgiou
 * 
 */
public class EnergyConstraint implements Constraints<Producer>
{
  // This is the working set of the algorithm from which a optimal subset is
  // estimated
  private List<Producer> workSet;
  // This is the energy threshold tha must not be overcome by the sum of the
  // power produced by the producers
  private double energyThreshold;
  // This is the weather report for the instance when the algorithm in run
  private WeatherReport report;
  // This is the cache of the producer's power
  private Map<Producer, Double> powerCache = new HashMap<Producer, Double>();
  // The sum of previous processed solution
  private double previousEnergySum = 0;

  /**
   * Constructs the energy constraint for the Ant Colony algorithm and Producer
   * class
   * 
   * @param workSet The set of Producers on which the algorithm is run
   * @param energy Threshold The energy threshold. Must be negative.
   * @param report The weather report for the time when the algorithm is run.
   */
  public EnergyConstraint (List<Producer> workSet, double energyThreshold,
                           WeatherReport report)
  {
    this.workSet = workSet;
    this.energyThreshold = energyThreshold;
    this.report = report;
  }

  /*
   * This function caches the producer's power output
   */
  private void initializeCache ()
  {
    for (Producer prod: workSet) {
      powerCache.put(prod, prod.getOutput(report));
    }
  }

  @Override
  public List<Producer> initializeCandidates (List<Producer> solution)
  {
    initializeCache();
    List<Producer> candidates = new ArrayList<Producer>();
    double solution_power = getSolutionPower(solution);
    previousEnergySum = solution_power;

    for (Producer prod: workSet) {
      if (abs(getProducerPower(prod) + solution_power) <= abs(energyThreshold)) {
        candidates.add(prod);
      }
    }
    
    candidates.remove(solution.get(0));

    return candidates;
  }

  @Override
  public List<Producer> updateCandidates (List<Producer> solution,
                                          List<Producer> candidates)
  {
    double solution_power =
      previousEnergySum + getProducerPower(solution.get(solution.size() - 1));
    previousEnergySum = solution_power;

    for (Iterator<Producer> it = candidates.iterator(); it.hasNext();) {
      Producer prod = it.next();
      if (abs(getProducerPower(prod) + solution_power) > abs(energyThreshold)) {
        it.remove();
      }else if(prod == solution.get(solution.size() - 1)){
        it.remove();
      }
    }

    return candidates;
  }

  /**
   * This function returns the power output of the specified producer
   * 
   * @param prod
   *          The producer whose power to return
   * @return The producer's power
   */
  protected double getProducerPower (Producer prod)
  {
    return powerCache.get(prod);
  }

  /**
   * This function returns the sum of the power output of the solution
   * 
   * @param solution
   *          the solution whose power to compute
   * @return The solution's power
   */
  protected double getSolutionPower (List<Producer> solution)
  {
    double sum = 0;
    for (Producer prod: solution) {
      sum += getProducerPower(prod);
    }
    return sum;
  }

}
