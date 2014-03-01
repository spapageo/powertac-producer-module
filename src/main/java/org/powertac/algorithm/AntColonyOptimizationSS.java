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

import java.util.*;

import static java.lang.Math.*;

/**
 * This class implements the the ant colony optimization method on the subset
 * selection problem. The algorithm is a combination of the MIN-MAX and the
 * original ant system algorithms. The algorithm in using generics to not tie
 * it to one type of producer.
 * 
 * @author Spyros Papageorgiou
 * 
 */
public final class AntColonyOptimizationSS<E>
{
  // The maximum number of iterations that that algorithm will perform
  private static final int MAX_ITERATIONS = 20;
  // This is the working set of the algorithm from which a optimal subset is
  // estimated
  private List<E> workSet;
  // The constraints objects the returns the candidate from which the next
  // producer is chosen.
  private Constraints<E> constraints;
  // The function that generates the lowers value for the best solution
  private ObjectiveMinFunction<E> minFunction;
  // This weight controls the importance of the pheromones
  private double a;
  // This weight controls the importance of the local attractiveness
  private double b;
  // This parameter represents the pheromone evaporation rate
  private double evapRate;
  // The number of ants to use
  private int antNum;
  // This parameter specifies the minimum value that a pheromone can take
  private double tmin;
  // This parameter specifies the maximum value that a pheromone can take
  private double tmax;

  // This variable maintains the pheremones of the working set.
  private Map<E, Double> pheromones = new HashMap<E, Double>();

  // The random seed generator
  private Random rng = new Random();

  /**
   * A ACO SS algorithm is constructed for the given constraints and minimize
   * function.
   * 
   * @param workSet
   *          This is the working set of the algorithm from which a optimal
   *          subset is
   *          estimated
   * @param constraints
   *          The constraints objects the returns the candidate from which the
   *          next
   *          producer is chosen.
   * @param minFunction
   *          The function that generates the lowers value for the best solution
   * @param a
   *          This weight controls the importance of the pheromones
   * @param b
   *          This weight controls the importance of the local attractiveness
   * @param evapRate
   *          This parameter represents the pheromone evaporation rate
   * @param antNum
   *          The number of ants to use
   * @param tmin
   *          This parameter specifies the minimum value that a pheromone can
   *          take
   * @param tmax
   *          This parameter specifies the maximum value that a pheromone can
   *          take
   * 
   */
  public AntColonyOptimizationSS (List<E> workSet, Constraints<E> constraints,
                                  ObjectiveMinFunction<E> minFunction, double a,
                                  double b, double evapRate, int antNum,
                                  double tmin, double tmax)
  {
    this.constraints = constraints;
    this.minFunction = minFunction;
    this.a = a;
    this.b = b;
    this.evapRate = evapRate;
    this.antNum = antNum;
    this.workSet = workSet;
    this.tmin = tmin;
    this.tmax = tmax;
  }

  /**
   * This function executes the algorithm and returns the chosen subset.
   */
  public List<E> execute ()
  {
    initializePheromones();
    List<List<E>> antSolutions = new ArrayList<List<E>>(antNum);
    List<E> bestSolution = new ArrayList<E>(0);

    for (int i = 0; i < MAX_ITERATIONS; i++) {
      antSolutions.clear();
      for (int antI = 0; antI < antNum; antI++) {
        antSolutions.add(constructSolution());
      }
      bestSolution = findBestSolution(antSolutions);
      updatePheromones(bestSolution);
    }

    return bestSolution;
  }

  /**
   * This function takes the best solutions and updates all the pheromone of the
   * working set.
   * @param bestSolution
   */
  void updatePheromones (List<E> bestSolution)
  {
   
    Set<E> bestSet = new HashSet<E>(bestSolution);
    double bestValue = minFunction.gradeSolution(bestSolution);
    //Apply evaporations
    for(E item : workSet){
      
      double ifbest = bestSet.contains(item) ? 1/bestValue : 0;
      
      double oldPheromone = pheromones.get(item);
      double newPheromone = oldPheromone * (1-evapRate) + ifbest;
      
      //enforce the limits
      newPheromone = min(max(newPheromone,tmin),tmax);
      //update the pheromone
      pheromones.put(item, newPheromone);
    }
  }

  /**
   * Finds the solutions the minimizes the minFunc
   * 
   * @param antSolutions
   * @return
   */
  List<E> findBestSolution (List<List<E>> antSolutions)
  {
    if(antSolutions.size() == 0)
      return new ArrayList<E>(0);
    
    //Initialize min and min solution;
    List<E> minSolution = antSolutions.get(0);
    double min = minFunction.gradeSolution(minSolution);
    
    for(List<E> solution : antSolutions){
      double value = minFunction.gradeSolution(solution);
      if(value < min){
        min = value;
        minSolution = solution;
      }
    }
    return minSolution;
  }

  /**
   * Constructs a random solution for one ant
   * 
   * @return the constructed solution
   */
  List<E> constructSolution ()
  {
    // This contains the solutions
    List<E> solution = new ArrayList<E>();
    List<E> candidates = new ArrayList<E>();
    List<Double> probabilities = new ArrayList<Double>();
    double prob = 0;

    // randomly add the first producer to the solution
    solution.add(workSet.get(rng.nextInt(workSet.size())));

    // initialize the candidates list
    candidates = constraints.initializeCandidates(solution);
    while (candidates.size() != 0) {
      // Calculate the probabilities
      probabilities = calculateProbabilities(candidates, probabilities);
      // Make the choice
      prob = rng.nextDouble();
      for (int i = 0; i < probabilities.size(); i++) {
        double tier = probabilities.get(i);
        if (prob <= tier) {
          solution.add(candidates.get(i));
          break;
        }
        prob -= tier;
      }
      // update the candidates
      candidates = constraints.updateCandidates(solution, candidates);
    }
    return solution;
  }

  /**
   * Calculates the list of probabilities for choosing the next item
   * 
   * @param candidates
   *          The list of candidates for whom to calculate the probabilities.
   * @param probabilities
   *          We use the same list to avoid memory allocations. The contents of
   *          this list are cleared.
   * @return
   */
  List<Double> calculateProbabilities (List<E> candidates,
                                       List<Double> probabilities)
  {
    probabilities.clear();

    double sum = 0;
    for (E candidate: candidates) {
      sum += calculateProbFactor(candidate);
    }
    for (E candidate: candidates) {
      final double value = calculateProbFactor(candidate) / sum;
      probabilities.add(value);
    }

    return probabilities;
  }

  /**
   * Inline function
   * 
   * @param item
   * @return
   */
  double calculateProbFactor (E item)
  {
    return pow(pheromones.get(item), a)
           * pow(1.0 / minFunction.gradeItem(item), b);
  }

  /**
   * This function initializes all the pheromones
   */
  void initializePheromones ()
  {
    for (E e: workSet) {
      pheromones.put(e, tmax);
    }
  }

  /**
   * @return the workSet
   */
  public List<E> getWorkSet ()
  {
    return workSet;
  }

  /**
   * @param workSet the workSet to set
   */
  public void setWorkSet (List<E> workSet)
  {
    this.workSet = workSet;
  }

  /**
   * @return the constraints
   */
  public Constraints<E> getConstraints ()
  {
    return constraints;
  }

  /**
   * @param constraints the constraints to set
   */
  public void setConstraints (Constraints<E> constraints)
  {
    this.constraints = constraints;
  }

  /**
   * @return the minFunction
   */
  public ObjectiveMinFunction<E> getMinFunction ()
  {
    return minFunction;
  }

  /**
   * @param minFunction the minFunction to set
   */
  public void setMinFunction (ObjectiveMinFunction<E> minFunction)
  {
    this.minFunction = minFunction;
  }

  /**
   * @return the a
   */
  public double getA ()
  {
    return a;
  }

  /**
   * @param a the a to set
   */
  public void setA (double a)
  {
    this.a = a;
  }

  /**
   * @return the b
   */
  public double getB ()
  {
    return b;
  }

  /**
   * @param b the b to set
   */
  public void setB (double b)
  {
    this.b = b;
  }

  /**
   * @return the evapRate
   */
  public double getEvapRate ()
  {
    return evapRate;
  }

  /**
   * @param evapRate the evapRate to set
   */
  public void setEvapRate (double evapRate)
  {
    this.evapRate = evapRate;
  }

  /**
   * @return the antNum
   */
  public int getAntNum ()
  {
    return antNum;
  }

  /**
   * @param antNum the antNum to set
   */
  public void setAntNum (int antNum)
  {
    this.antNum = antNum;
  }

  /**
   * @return the tmin
   */
  public double getTmin ()
  {
    return tmin;
  }

  /**
   * @param tmin the tmin to set
   */
  public void setTmin (double tmin)
  {
    this.tmin = tmin;
  }

  /**
   * @return the tmax
   */
  public double getTmax ()
  {
    return tmax;
  }

  /**
   * @param tmax the tmax to set
   */
  public void setTmax (double tmax)
  {
    this.tmax = tmax;
  }

  /**
   * @return the rng
   */
  public Random getRng ()
  {
    return rng;
  }

  /**
   * @param rng the rng to set
   */
  public void setRng (Random rng)
  {
    this.rng = rng;
  }

  /**
   * @return the pheromones
   */
  public Map<E, Double> getPheromones ()
  {
    return pheromones;
  }
}
