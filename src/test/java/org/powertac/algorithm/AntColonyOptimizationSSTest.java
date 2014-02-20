package org.powertac.algorithm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Matchers.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.Equals;
import org.mockito.internal.matchers.Not;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class AntColonyOptimizationSSTest
{

  List<Object> workSet = new LinkedList<Object>();
  @SuppressWarnings({ "unchecked" })
  Constraints<Object> con = mock(Constraints.class);
  @SuppressWarnings({ "unchecked" })
  MinFunction<Object> minf = mock(MinFunction.class);
  double a = 1;
  double b = 2;
  double evapRate = 3;
  int antNum = 4;
  double tmin = 5;
  double tmax = 6;

  AntColonyOptimizationSS<Object> aco =
    new AntColonyOptimizationSS<Object>(workSet, con, minf, a, b, evapRate,
                                        antNum, tmin, tmax);

  @Before
  public void setUp ()
  {
    // For now empty
  }

  @Test
  public void testAntColonyOptimizationSS ()
  {
    assertTrue(aco.getWorkSet() == workSet);
    assertTrue(aco.getConstraints() == con);
    assertTrue(aco.getMinFunction() == minf);
    assertTrue(aco.getA() == a);
    assertTrue(aco.getB() == b);
    assertTrue(aco.getEvapRate() == evapRate);
    assertTrue(aco.getAntNum() == antNum);
    assertTrue(aco.getTmin() == tmin);
    assertTrue(aco.getTmax() == tmax);
  }

  @Test
  public void testExecute ()
  {
    fail("Not yet implemented");
  }

  @Test
  public void testUpdatePheromones ()
  {
    fail("Not yet implemented");
  }

  @Test
  public void testFindBestSolution ()
  {
    List<Object> sol1 = new ArrayList<Object>();
    List<Object> sol2 = new ArrayList<Object>();
    List<Object> sol3 = new ArrayList<Object>();
    List<List<Object>> sols = new ArrayList<List<Object>>();
    sols.add(sol1);
    sols.add(sol2);
    sols.add(sol3);
    
    when(minf.minFunc(sol1)).thenReturn(1.0);
    when(minf.minFunc(sol2)).thenReturn(2.0);
    when(minf.minFunc(sol3)).thenReturn(3.0);
    
    List<Object> best = aco.findBestSolution(sols);
    assertTrue(best == sol1);
    
  }

  @Test
  public void testConstructSolution ()
  {
    List<Object> candidates = new ArrayList<Object>();
    candidates.add(new Object());
    candidates.add(new Object());
    candidates.add(new Object());
    candidates.add(new Object());
    for (Object o: candidates) {
      workSet.add(o);
    }

    //when(minf.minFunc(any(Object.class))).thenReturn(1.0);
    when(minf.minFunc(candidates.get(0))).thenReturn(1.0);
    when(minf.minFunc(candidates.get(1))).thenReturn(10000000000000000000.0);
    when(minf.minFunc(candidates.get(2))).thenReturn(1.0);
    when(minf.minFunc(candidates.get(3))).thenReturn(1.0);
    aco.setTmax(1);
    aco.setA(1);
    aco.setB(1);
    aco.initializePheromones();

    aco.setConstraints(new TestCon(candidates,3));
    
    Random r = mock(Random.class);
    when(r.nextInt(anyInt())).thenReturn(0);
    when(r.nextDouble()).thenReturn(0.4);
    aco.setRng(r);
    
    List<Object> solution = aco.constructSolution();
    assertEquals(3, solution.size(), 0);
    assertTrue(solution.contains(workSet.get(0)));
    assertTrue(solution.contains(workSet.get(2)));
    assertTrue(solution.contains(workSet.get(3)));
  }

  @Test
  public void testCalculateProbabilities ()
  {
    List<Object> candidates = new ArrayList<Object>();
    candidates.add(new Object());
    candidates.add(new Object());
    candidates.add(new Object());
    candidates.add(new Object());
    candidates.add(new Object());
    candidates.add(new Object());
    candidates.add(new Object());
    for (Object o: candidates) {
      workSet.add(o);
    }

    List<Double> probabilies = new ArrayList<Double>();

    when(minf.minFunc(any(Object.class))).thenReturn(1.0);
    aco.setTmax(1);
    aco.setA(1);
    aco.setB(1);
    aco.initializePheromones();

    List<Double> probabilities =
      aco.calculateProbabilities(candidates, probabilies);
    assertTrue(probabilities.size() == 7);
    for (Double prob: probabilities) {
      assertTrue(prob <= 1);
      assertTrue(prob >= 0);
      assertEquals(1.0 / 7.0, prob, 0.001);
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCalculateProbabilitiesNotEqual ()
  {
    List<Object> candidates = new ArrayList<Object>();
    candidates.add(new Object());
    candidates.add(new Object());
    candidates.add(new Object());
    candidates.add(new Object());
    candidates.add(new Object());
    candidates.add(new Object());
    candidates.add(new Object());
    for (Object o: candidates) {
      workSet.add(o);
    }

    List<Double> probabilies = new ArrayList<Double>();

    when(minf.minFunc(argThat(new Equals(candidates.get(0))))).thenReturn(0.5);
    when(minf.minFunc(argThat(new Not(new Equals(candidates.get(0))))))
            .thenReturn(1.0);

    aco.setTmax(1);
    aco.setA(1);
    aco.setB(1);
    aco.initializePheromones();

    List<Double> probabilities =
      aco.calculateProbabilities(candidates, probabilies);
    assertTrue(probabilities.size() == 7);
    for (int i = 0; i < probabilies.size(); i++) {
      double prob = probabilies.get(i);
      if (i == 0) {
        assertEquals(1.0 / 4.0, prob, 0.0001);
        continue;
      }
      assertTrue(prob <= 1);
      assertTrue(prob >= 0);
      assertEquals(1.0 / 8.0, prob, 0.001);
    }
  }

  @Test
  public void testCalculateProbFactor ()
  {
    aco.setA(0);
    aco.setB(0);
    aco.setTmax(4);
    Object item = new Object();
    workSet.add(item);
    aco.initializePheromones();
    when(minf.minFunc(item)).thenReturn(1 / 4.0);

    assertEquals(1, aco.calculateProbFactor(item), 0.0001);
    aco.setA(1);
    aco.setB(1);
    assertEquals(16.0, aco.calculateProbFactor(item), 0.0001);
    aco.setA(0.5);
    aco.setB(0.5);
    assertEquals(4.0, aco.calculateProbFactor(item), 0.0001);
    verify(minf, times(3)).minFunc(item);
  }

  @Test
  public void testInitializePheromones ()
  {
    aco.getWorkSet().add(new Object());
    aco.getWorkSet().add(new Object());
    aco.getWorkSet().add(new Object());
    aco.getWorkSet().add(new Object());
    aco.getWorkSet().add(new Object());
    aco.initializePheromones();
    Map<Object, Double> pher = aco.getPheromones();
    for (Object o: pher.keySet()) {
      assertTrue(pher.get(o) == tmax);
    }
  }
  
  static class TestCon implements Constraints<Object>{

    private List<Object> cand;
    private int solSize;
    public TestCon (List<Object> cand, int solSize)
    {
      this.cand = cand;
      this.solSize = solSize;
    }
    
    @Override
    public void setWorkingSet (Set<Object> workSet)
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public List<Object> getCandidates (List<Object> solution,
                                       List<Object> candidates)
    {
      if(candidates.size() == 0){
        assertTrue(solution.size() == 1);
        cand.remove(solution.get(0));
        return cand;
      }
        
      
      if(solution.size() < solSize){
        candidates.remove(solution.get(solution.size()-1));
      }else{
        candidates.clear();
      }
      
      return candidates;
    }
    
  }

}
