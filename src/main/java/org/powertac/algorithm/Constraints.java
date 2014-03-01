/**
 * 
 */
package org.powertac.algorithm;

import java.util.List;

/**
 * <p>
 * This interface represents the constraints that a solution must follow in
 * order to be considered possible.
 * </p>
 * <p>
 * Note: Even partial possible solution must be excepted. If for example the
 * problem is to find the subset of some integers that have a sum of 10, the
 * constraint should be sum(solution) <= 10 and NOT sum(solution) == 10. If the
 * aco algorithm doesn't return a perfect solution i.e sum(solution) == 9, the
 * result is still valid. Only, not very good.
 * </p>
 * 
 * @author Spyros Papageorgiou
 * 
 */
public interface Constraints<E>
{
  /**
   * This function is called once for every construction of a solution. Given
   * the state of the solution it initializes the candidates array. The solution
   * should contain only one item at time of the call.
   * 
   * @param solution
   *          The solution initialized with one random item
   * @return The list of the candidates from which the next item can be chosen
   * 
   */
  List<E> initializeCandidates (List<E> solution);

  /**
   * This function is called after each choice on the item is made. Given
   * the state of the solution it updates the candidates array or
   * 
   * @param solution
   *          The partial or complete solution
   * @param candidates
   *          The candidates from the last choice.
   * @return The list of the candidates from which the next item can be chosen.
   *         This can be one given the argument after being updated or a new
   *         one.
   */
  List<E> updateCandidates (List<E> solution, List<E> candidates);
}
