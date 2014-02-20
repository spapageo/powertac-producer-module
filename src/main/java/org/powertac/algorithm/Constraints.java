/**
 * 
 */
package org.powertac.algorithm;

import java.util.List;

/**
 * @author Spyros Papageorgiou
 *
 */
public interface Constraints<E>
{
  
  void setWorkingSet(List<E> workSet);
  
  List<E> getCandidates(List<E> solution,List<E> candidates);
}
