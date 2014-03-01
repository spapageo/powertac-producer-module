package org.powertac.algorithm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Matchers.*;

import org.junit.Test;
import org.mockito.internal.matchers.Equals;
import org.mockito.internal.matchers.Not;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AntColonyOptimizationSSTest
{

  List<Object> workSet = new LinkedList<Object>();
  @SuppressWarnings({ "unchecked" })
  Constraints<Object> con = mock(Constraints.class);
  @SuppressWarnings({ "unchecked" })
  ObjectiveMinFunction<Object> minf = mock(ObjectiveMinFunction.class);
  double a = 1;
  double b = 2;
  double evapRate = 3;
  int antNum = 4;
  double tmin = 5;
  double tmax = 6;

  AntColonyOptimizationSS<Object> aco =
    new AntColonyOptimizationSS<Object>(workSet, con, minf, a, b, evapRate,
                                        antNum, tmin, tmax);

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
    List<Integer> workItems = new ArrayList<Integer>();
    workItems.add(2);
    workItems.add(3);
    workItems.add(6);
    workItems.add(5);
    IntCond conint = new IntCond(10);
    conint.setWorkingSet(workItems);
    IntMin minfint = new IntMin(10);

    AntColonyOptimizationSS<Integer> ac =
      new AntColonyOptimizationSS<Integer>(workItems, conint, minfint, 1, 1,
                                           0.1, 30, 0.01, 10);
    List<Integer> bestSol = ac.execute();

    assertTrue(bestSol.contains(2));
    assertTrue(bestSol.contains(3));
    assertTrue(bestSol.contains(5));
  }

  @Test
  public void testUpdatePheromones ()
  {
    for (int i = 0; i < 10; i++) {
      workSet.add(new Object());
    }

    // The best solution contains only the first three items
    List<Object> bestSolution = new ArrayList<Object>();
    bestSolution.add(workSet.get(0));
    bestSolution.add(workSet.get(1));
    bestSolution.add(workSet.get(2));

    aco.setTmax(1);
    aco.setTmin(0.5);
    aco.setEvapRate(0.1);
    when(minf.gradeSolution(bestSolution)).thenReturn(4.0);

    aco.initializePheromones();
    aco.getPheromones().put(workSet.get(0), 0.5);
    aco.getPheromones().put(workSet.get(4), 0.5);

    aco.updatePheromones(bestSolution);

    assertEquals((1 - 0.1) * 0.5 + (1.0 / 4.0),
                 aco.getPheromones().get(workSet.get(0)), 0.0001);
    assertEquals(1, aco.getPheromones().get(workSet.get(1)), 0.0001);
    assertEquals(1, aco.getPheromones().get(workSet.get(2)), 0.0001);
    assertEquals(0.9, aco.getPheromones().get(workSet.get(3)), 0.0001);
    assertEquals(0.5, aco.getPheromones().get(workSet.get(4)), 0.0001);
    assertEquals(0.9, aco.getPheromones().get(workSet.get(5)), 0.0001);

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

    when(minf.gradeSolution(sol1)).thenReturn(1.0);
    when(minf.gradeSolution(sol2)).thenReturn(2.0);
    when(minf.gradeSolution(sol3)).thenReturn(3.0);

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

    // when(minf.minFunc(any(Object.class))).thenReturn(1.0);
    when(minf.gradeItem(candidates.get(0))).thenReturn(1.0);
    when(minf.gradeItem(candidates.get(1))).thenReturn(10000000000000000000.0);
    when(minf.gradeItem(candidates.get(2))).thenReturn(1.0);
    when(minf.gradeItem(candidates.get(3))).thenReturn(1.0);
    aco.setTmax(1);
    aco.setA(1);
    aco.setB(1);
    aco.initializePheromones();

    aco.setConstraints(new TestCon(candidates, 3));

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

    when(minf.gradeItem(any(Object.class))).thenReturn(1.0);
    aco.setTmax(1);
    aco.setTmin(1);
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

    when(minf.gradeItem(argThat(new Equals(candidates.get(0))))).thenReturn(0.5);
    when(minf.gradeItem(argThat(new Not(new Equals(candidates.get(0))))))
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
    when(minf.gradeItem(item)).thenReturn(1 / 4.0);

    assertEquals(1, aco.calculateProbFactor(item), 0.0001);
    aco.setA(1);
    aco.setB(1);
    assertEquals(16.0, aco.calculateProbFactor(item), 0.0001);
    aco.setA(0.5);
    aco.setB(0.5);
    assertEquals(4.0, aco.calculateProbFactor(item), 0.0001);
    verify(minf, times(3)).gradeItem(item);
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

  static class TestCon implements Constraints<Object>
  {

    private List<Object> cand;
    private int solSize;

    public TestCon (List<Object> cand, int solSize)
    {
      this.cand = cand;
      this.solSize = solSize;
    }

    @Override
    public List<Object> updateCandidates (List<Object> solution,
                                          List<Object> candidates)
    {

      if (solution.size() < solSize) {
        candidates.remove(solution.get(solution.size() - 1));
      }
      else {
        candidates.clear();
      }

      return candidates;
    }

    @Override
    public List<Object> initializeCandidates (List<Object> solution)
    {
      cand.remove(solution.get(0));
      return cand;
    }

  }

  static class IntCond implements Constraints<Integer>
  {
    int upperLimit;
    List<Integer> workSet;

    public IntCond (int upperLimit)
    {
      this.upperLimit = upperLimit;
    }

    public void setWorkingSet (List<Integer> workSet)
    {
      this.workSet = workSet;
    }

    @Override
    public List<Integer> updateCandidates (List<Integer> solution,
                                           List<Integer> candidates)
    {
      double solSum = sum(solution);
      for (Iterator<Integer> it = candidates.iterator(); it.hasNext();) {
        Integer item = it.next();
        if (item + solSum > upperLimit || solution.contains(item)) {
          it.remove();
        }
      }
      return candidates;
    }

    @Override
    public List<Integer> initializeCandidates (List<Integer> solution)
    {
      List<Integer> candidates = new ArrayList<Integer>();
      double solSum = sum(solution);
      for (Integer item: workSet) {
        if (item + solSum <= upperLimit && !solution.contains(item)) {
          candidates.add(item);
        }
      }
      return candidates;
    }

    static Integer sum (List<Integer> list)
    {
      Integer sum = 0;
      for (Integer in: list) {
        sum += in;
      }
      return sum;
    }
  }

  static class IntMin implements ObjectiveMinFunction<Integer>
  {

    Integer limit;

    public IntMin (Integer limit)
    {
      this.limit = limit;
    }

    @Override
    public double gradeItem (Integer e)
    {
      return 1;
    }

    @Override
    public double gradeSolution (List<Integer> solution)
    {
      return limit - IntCond.sum(solution) + 0.1;
    }

  }

}
