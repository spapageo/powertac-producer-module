/**
 * 
 */
package org.powertac.algorithm;

import java.util.List;

/**
 * @author Spyros Papageorgiou
 *
 */
public interface MinFunction<E>
{
  void setWorkingSet(List<E> workSet);
  
  double minFunc(E e);
  
  double minFunc(List<E> solution);
}
