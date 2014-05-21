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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.powertac.common.WeatherReport;
import org.powertac.producer.Producer;

/**
 * This class implement the ObjectiveMinFunction interface as a set of weights
 * for the energy cost, emissions and distance from the energy limit.
 * 
 * @author Spyros Papageorgiou
 * 
 */
public class EnrgEmsMinFunction implements ObjectiveMinFunction<Producer>
{

  private double limit;
  private double costWeight;
  private double emissionWeight;
  private double limitWeight;
  private WeatherReport waetherReport;
  private List<Producer> workSet;
  private Map<Producer, Double> energyCache = new HashMap<Producer, Double>();

  public EnrgEmsMinFunction (double limit, double costWeight,
                             double emissionWeight, double limitWeight,
                             WeatherReport waetherReport, List<Producer> workSet)
  {
    this.limit = limit;
    this.costWeight = costWeight;
    this.emissionWeight = emissionWeight;
    this.limitWeight = limitWeight;
    this.waetherReport = waetherReport;
    this.workSet = workSet;
  }

  public void initiliazeCache ()
  {
    energyCache.clear();

    for (Producer prod: workSet) {
      energyCache.put(prod, prod.getOutput(waetherReport));
    }
  }

  @Override
  public double gradeItem (Producer item)
  {
    if (energyCache.isEmpty())
      initiliazeCache();

    double result =
      (costWeight * item.getCostPerKw() + emissionWeight
                                          * item.getCo2Emissions());
    return result;
  }

  @Override
  public double gradeSolution (List<Producer> solution)
  {
    if (energyCache.isEmpty())
      initiliazeCache();

    double sum = 0;
    double energySum = 0;
    for (Producer prod: solution) {
      double energy = energyCache.get(prod);
      sum +=
        (  costWeight * prod.getCostPerKw()
         + emissionWeight * prod.getCo2Emissions()) * energy;
      energySum += energy;
    }
    
    double result = Math.abs(sum) + limitWeight * Math.abs(energySum - limit);
    result = result/Math.abs(energySum);
    return result;
  }

}
