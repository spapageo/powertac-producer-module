/**
 * 
 */
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
    
    double result = sum + limitWeight * Math.abs(energySum - limit);
    result = result/energySum;
    return result;
  }

}
