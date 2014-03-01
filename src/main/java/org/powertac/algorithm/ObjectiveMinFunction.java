/**
 * 
 */
package org.powertac.algorithm;

import java.util.List;

/**
 * <p>
 * This interface represents the objective function that need to be minimized by
 * the aco algorithm.
 * </p>
 * </p>
 * The objective function is used in:
 * </p>
 * <ul>
 * <li>
 * The calculation of the attractiveness of each item</li>
 * <li>
 * The calculation of the attractiveness of a solution</li>
 * </ul>
 * </p>
 * Note: This function should NEVER return a zero value.
 * </p>
 * 
 * @author Spyros Papageorgiou
 * 
 */
public interface ObjectiveMinFunction<E>
{
  /**
   * This function calculates the output of the objective function for a single
   * item.
   * 
   * @param item
   *          The item to grade
   * @return The grade of the item
   */
  double gradeItem (E item);

  /**
   * This function calculates the output of the objective function for a
   * solution
   * 
   * @param item
   *          The item to grade
   * @return The grade of the item
   */
  double gradeSolution (List<E> solution);
}
