package org.powertac.producer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.springframework.test.util.ReflectionTestUtils;

public class TarifEvalVariableTest
{
  // foundation components
  private Competition competition;
  private TimeService timeService;
  private TariffRepo tariffRepo;
  private TariffSubscriptionRepo tariffSubscriptionRepo;
  private CustomerInfo customer;

  // mocks
  private TariffMarket tariffMarket;
  private Accounting accountingService;

  // brokers and tariffs
  private Broker defaultBroker;
  private Tariff defaultConsumption;
  private Tariff defaultProduction;
  private Broker bob;
  private Broker jim;

  // customer model accessor
  private RandomAccessor cma;

  // unit under test
  private TariffEvaluator evaluator;
  
  @Before
  public void setUp () throws Exception
  {
    competition = Competition.newInstance("tariff-evaluator-test");
    timeService = new TimeService();
    timeService.setCurrentTime(competition.getSimulationBaseTime()
                               .plus(TimeService.HOUR * 7));
    //tariffRepo = new TariffRepo();
    tariffSubscriptionRepo = new TariffSubscriptionRepo();

    // set up mocks
    makeMocks();

    // satisfy dependencies
    ReflectionTestUtils.setField(tariffSubscriptionRepo,
                                 "tariffRepo", tariffRepo);
    //ReflectionTestUtils.setField(tariffSubscriptionRepo,
    //                             "tariffMarketService", tariffMarket);

    // set up default tariffs
    defaultBroker = new Broker("default");
    TariffSpecification dcSpec =
            new TariffSpecification(defaultBroker,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.8));
    defaultConsumption = new Tariff(dcSpec);
    initTariff(defaultConsumption);
    when(tariffMarket.getDefaultTariff(PowerType.CONSUMPTION))
    .thenReturn(defaultConsumption);

    TariffSpecification dpSpec =
            new TariffSpecification(defaultBroker,
                                    PowerType.PRODUCTION).
                                    addRate(new Rate().withValue(0.1));
    defaultProduction = new Tariff(dpSpec);
    initTariff(defaultProduction);
    when(tariffMarket.getDefaultTariff(PowerType.PRODUCTION))
    .thenReturn(defaultProduction);

    // other brokers
    jim = new Broker("Jim");
    bob = new Broker("Bob");

    // set up customer
    customer = new CustomerInfo("Guinea Pig", 10000)
    .withMultiContracting(true);
    cma = new RandomAccessor();

    // uut setup
    evaluator = new TariffEvaluator(cma).
            withPreferredContractDuration(4).withRationality(0.8);
    ReflectionTestUtils.setField(evaluator,
                                 "tariffRepo", tariffRepo);
    ReflectionTestUtils.setField(evaluator,
                                 "tariffMarket", tariffMarket);
    ReflectionTestUtils.setField(evaluator,
                                 "tariffSubscriptionRepo", tariffSubscriptionRepo);
  }

  private TariffSubscription subscribeTo (Tariff tariff, int count)
  {
    TariffSubscription subscription =
            new TariffSubscription(customer, tariff);
    initSubscription(subscription);
    subscription.subscribe(count);
    tariffSubscriptionRepo.add(subscription);
    return subscription;
  }

  private void makeMocks ()
  {
    tariffRepo = mock(TariffRepo.class);
    tariffMarket = mock(TariffMarket.class);
    accountingService = mock(Accounting.class);
  }

  // initializes a tariff. It needs dependencies injected
  private void initTariff (Tariff tariff)
  {
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
  }

  // initializes a tariff subscription.
  private void initSubscription (TariffSubscription sub)
  {
    ReflectionTestUtils.setField(sub, "timeService", timeService);
    ReflectionTestUtils.setField(sub, "tariffMarketService", tariffMarket);
    ReflectionTestUtils.setField(sub, "accountingService", accountingService);
  }
  //==========================================================================//
  //=================================TESTS====================================//
  //==========================================================================//
  @Test
  public void logicTest2WithPopulation() throws Exception{
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/logic_weight_pop_test.txt");
    //accessor setup
    double[] profile = {10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10};
    cma.profile = profile;
    cma.brokerSwitchFactor = 0;

    //evaluator setup
    evaluator.withChunkSize(1).withInertia(0).withTariffSwitchFactor(0).withInconvenienceWeight(0);

    //suscribe to default 
    subscribeTo(defaultConsumption, customer.getPopulation());
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();

    //Initiate the 2 extra tariffs
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    tariffs.add(bobTariff);

    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.392));
                                    
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    tariffs.add(jimTariff);

    //Make sure this tariff list is returned
    tariffs.add(defaultConsumption);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
    .thenReturn(tariffs);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    for(double logic = 0.0; logic < 1.01; logic += 0.02){
      //reset evaluator
      evaluator.withRationality(logic);
      evaluator.evaluateTariffs();
      
      int countdef = 0, countA = 0, countB = 0;

      if(calls.get(defaultConsumption) != null)
        countdef = calls.get(defaultConsumption);
      if(calls.get(bobTariff) != null)
        countA = calls.get(bobTariff);
      if(calls.get(jimTariff) != null)
        countB = calls.get(jimTariff);
      int pop = customer.getPopulation();
      
      pw.printf(Locale.ENGLISH,"%f,%f,%f,%f%n",logic,(double)(pop+countdef)/pop,(double)countA/pop,(double)countB/pop);
      //System.out.printf(Locale.ENGLISH,"%f,%f,%f,%f%n",logic,(double)(pop+countdef)/pop,(double)countA/pop,(double)countB/pop);
      
      //reset and redo
      calls.clear();
      this.tariffSubscriptionRepo.recycle();
      subscribeTo(defaultConsumption, customer.getPopulation());
    }
    pw.close();
  }
  
  @Test
  public void inconvenienceWeighTestWithPopulation() throws Exception{
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/inconvenience_weight_pop_test.txt");
    //accessor setup
    double[] profile = {100,100,100};
    cma.profile = profile;
    cma.brokerSwitchFactor=0;

    //evaluator setup
    evaluator.withChunkSize(1).withInertia(0).withRationality(1)
    .withTariffSwitchFactor(0);

    //suscribe to default 
    subscribeTo(defaultConsumption, customer.getPopulation());
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();

    //Initiate the 2 extra tariffs
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    tariffs.add(bobTariff);

    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4)).
                                    addRate(new Rate().withTierThreshold(100).withValue(-0.3));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    tariffs.add(jimTariff);

    //Make sure this tariff list is returned
    tariffs.add(defaultConsumption);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
    .thenReturn(tariffs);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    for(double inconW = 0.0; inconW < 1.01; inconW += 0.1){
      evaluator.withInconvenienceWeight(inconW);
      evaluator.evaluateTariffs();
      int countdef = 0, countA = 0, countB = 0;

      if(calls.get(defaultConsumption) != null)
        countdef = calls.get(defaultConsumption);
      if(calls.get(bobTariff) != null)
        countA = calls.get(bobTariff);
      if(calls.get(jimTariff) != null)
        countB = calls.get(jimTariff);
      int pop = customer.getPopulation();
      pw.printf(Locale.ENGLISH,"%f,%f,%f%n",-(double)countdef/pop,(double)countA/pop,(double)countB/pop);
    }
    pw.close();
  }
  
  @Test
  public void tieredWeighTestWithPopulation() throws Exception{
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/tiered_weight_pop_test.txt");
    //accessor setup
    double[] profile = {100,100,100};
    cma.profile = profile;
    cma.brokerSwitchFactor = 0;

    //evaluator setup
    evaluator.withChunkSize(1).withInertia(0).withRationality(1)
    .withTariffSwitchFactor(0).withPreferredContractDuration(1);

    //suscribe to default 
    subscribeTo(defaultConsumption, customer.getPopulation());
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();

    //Initiate the 2 extra tariffs
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    tariffs.add(bobTariff);

    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4)).
                                    addRate(new Rate().withTierThreshold(300).withValue(-0.3));
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    tariffs.add(jimTariff);

    //Make sure this tariff list is returned
    tariffs.add(defaultConsumption);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
    .thenReturn(tariffs);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    for(double tiered = 0.0; tiered < 1.01; tiered += 0.02){
      //reset evaluator
      evaluator = new TariffEvaluator(cma).withChunkSize(1).withInertia(0).withRationality(1)
              .withTariffSwitchFactor(0).withInconvenienceWeight(1);
      ReflectionTestUtils.setField(evaluator,
                                   "tariffRepo", tariffRepo);
      ReflectionTestUtils.setField(evaluator,
                                   "tariffMarket", tariffMarket);
      ReflectionTestUtils.setField(evaluator,
                                   "tariffSubscriptionRepo", tariffSubscriptionRepo);

      evaluator.initializeInconvenienceFactors(0, tiered, 0, 0);

      evaluator.evaluateTariffs();
      
      int countA = 0, countB = 0;

//      if(calls.get(defaultConsumption) != null)
//        countdef = calls.get(defaultConsumption);
      if(calls.get(bobTariff) != null)
        countA = calls.get(bobTariff);
      if(calls.get(jimTariff) != null)
        countB = calls.get(jimTariff);
      int pop = customer.getPopulation();
      pw.printf(Locale.ENGLISH,"%f,%f,%f%n",tiered,(double)countA/pop,(double)countB/pop);
      //System.out.printf(Locale.ENGLISH,"%f,%f,%f%n",tiered,(double)countA/pop,(double)countB/pop);
    }
    pw.close();
  }

  @Test
  public void tieredWeighTest2WithPopulation() throws Exception{
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/tiered2_weight_pop_test.txt");
    //accessor setup
    double[] profile = {10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10};
    cma.profile = profile;
    cma.brokerSwitchFactor = 0;

    //evaluator setup
    evaluator.withChunkSize(1).withInertia(0).withRationality(1)
    .withTariffSwitchFactor(0);

    //suscribe to default 
    subscribeTo(defaultConsumption, customer.getPopulation());
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();

    //Initiate the 2 extra tariffs
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    tariffs.add(bobTariff);

    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4)).
                                    addRate(new Rate().withTierThreshold(300).withValue(-0.3));
    jimTS.withMinDuration(6).withEarlyWithdrawPayment(-96);
                                    
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    tariffs.add(jimTariff);

    //Make sure this tariff list is returned
    tariffs.add(defaultConsumption);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
    .thenReturn(tariffs);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    for(double tiered = 0.0; tiered < 1.01; tiered += 0.02){
      //reset evaluator
      evaluator = new TariffEvaluator(cma).withChunkSize(1).withInertia(0).withRationality(1)
              .withTariffSwitchFactor(0).withInconvenienceWeight(1).withPreferredContractDuration(1);
      ReflectionTestUtils.setField(evaluator,
                                   "tariffRepo", tariffRepo);
      ReflectionTestUtils.setField(evaluator,
                                   "tariffMarket", tariffMarket);
      ReflectionTestUtils.setField(evaluator,
                                   "tariffSubscriptionRepo", tariffSubscriptionRepo);

      evaluator.initializeInconvenienceFactors(0, tiered, 0, 0);

      evaluator.evaluateTariffs();
      
      int countA = 0, countB = 0;

//      if(calls.get(defaultConsumption) != null)
//        countdef = calls.get(defaultConsumption);
      if(calls.get(bobTariff) != null)
        countA = calls.get(bobTariff);
      if(calls.get(jimTariff) != null)
        countB = calls.get(jimTariff);
      int pop = customer.getPopulation();
      pw.printf(Locale.ENGLISH,"%f,%f,%f%n",tiered,(double)countA/pop,(double)countB/pop);
      //System.out.printf(Locale.ENGLISH,"%f,%f,%f%n",tiered,(double)countA/pop,(double)countB/pop);
    }
    pw.close();
  }
  
  @Test
  public void VPFTestWithPopulation() throws Exception{
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/vp_weight_pop_test.txt");
    PrintWriter pw2 = new PrintWriter("data/vp_weight_pop_test2.txt");
    customer.withPowerType(PowerType.PRODUCTION);
    //accessor setup
    double[] profile = {-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,-10,
                        -10,-10,-10,-10,-10,-10,-10,-10,-10,-10};
    cma.profile = profile;
    cma.brokerSwitchFactor = 0;

    //evaluator setup
    evaluator.withChunkSize(1).withInertia(0).withRationality(1)
    .withTariffSwitchFactor(0).withInconvenienceWeight(1);

    //suscribe to default 
    subscribeTo(defaultProduction, customer.getPopulation());
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();

    //Initiate the 2 extra tariffs
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.PRODUCTION).
                                    addRate(new Rate().withValue(0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    tariffs.add(bobTariff);

    Rate r = new Rate().withMaxValue(0.4).withMinValue(0.2).withFixed(false).withExpectedMean(0.3).withNoticeInterval(1);
    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.PRODUCTION).
                                    addRate(r);
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    tariffs.add(jimTariff);

    //Make sure this tariff list is returned
    tariffs.add(defaultProduction);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
    .thenReturn(tariffs);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());
    
    r.withExpectedMean(0.3);
    r.withMaxValue(0.55);
    initTariff(jimTariff);
    int i = 0;
    int j = 0;
    double[][] result = new double[101][4];
    for(double maxvalue = 0.55; maxvalue < 0.751; maxvalue += 0.1){
      r.withMaxValue(maxvalue);
      initTariff(jimTariff);
//      System.out.println(helper.estimateCost(defaultProduction, cma.profile));
//      System.out.println(helper.estimateCost(bobTariff, cma.profile));
//      System.out.println(helper.estimateCost(jimTariff, cma.profile));
      for(double vpf = 0.0; vpf < 1.01; vpf += 0.01){
        
        //reset evaluator
        evaluator = new TariffEvaluator(cma).withChunkSize(1).withInertia(0).withRationality(1)
                .withTariffSwitchFactor(0).withInconvenienceWeight(1);
        ReflectionTestUtils.setField(evaluator,
                                     "tariffRepo", tariffRepo);
        ReflectionTestUtils.setField(evaluator,
                                     "tariffMarket", tariffMarket);
        ReflectionTestUtils.setField(evaluator,
                                     "tariffSubscriptionRepo", tariffSubscriptionRepo);

        evaluator.initializeInconvenienceFactors(0, 0, vpf, 0);
        evaluator.evaluateTariffs();

        int countB = 0;
//        if(calls.get(defaultProduction) != null)
//          countdef = calls.get(defaultProduction);
//        if(calls.get(bobTariff) != null)
//          countA = calls.get(bobTariff);
        if(calls.get(jimTariff) != null)
          countB = calls.get(jimTariff);
        int pop = customer.getPopulation();
        
        result[i][0] = vpf;
        result[i][j+1] = (double)countB/pop;
        i++;
        //reset and redo
        calls.clear();
        this.tariffSubscriptionRepo.recycle();
        subscribeTo(defaultProduction, customer.getPopulation());
      }
      i=0;
      j++;
    }
    for(i=0;i<101;i++){
        //System.out.printf(Locale.ENGLISH,"%f,%f,%f,%f%n",result[i][0],result[i][1],result[i][2],result[i][3]);
        pw.printf(Locale.ENGLISH,"%f,%f,%f,%f%n",result[i][0],result[i][1],result[i][2],result[i][3]);

    }
    System.out.flush();
    i=0;j=0;
    r.withMaxValue(0.55);
    r.withExpectedMean(0.3);
    initTariff(jimTariff);
    for(double meanvalue = 0.3; meanvalue < 0.51; meanvalue += 0.1){
      r.withExpectedMean(meanvalue);
      initTariff(jimTariff);
//      System.out.println(helper.estimateCost(defaultProduction, cma.profile));
//      System.out.println(helper.estimateCost(bobTariff, cma.profile));
//      System.out.println(helper.estimateCost(jimTariff, cma.profile));
      for(double vpf = 0.0; vpf < 1.01; vpf += 0.01){
        
        
        //reset evaluator
        evaluator = new TariffEvaluator(cma).withChunkSize(1).withInertia(0).withRationality(1)
                .withTariffSwitchFactor(0).withInconvenienceWeight(1);
        ReflectionTestUtils.setField(evaluator,
                                     "tariffRepo", tariffRepo);
        ReflectionTestUtils.setField(evaluator,
                                     "tariffMarket", tariffMarket);
        ReflectionTestUtils.setField(evaluator,
                                     "tariffSubscriptionRepo", tariffSubscriptionRepo);

        evaluator.initializeInconvenienceFactors(0, 0, vpf, 0);
        evaluator.evaluateTariffs();

        int countB = 0;
//        if(calls.get(defaultProduction) != null)
//          countdef = calls.get(defaultProduction);
//        if(calls.get(bobTariff) != null)
//          countA = calls.get(bobTariff);
        if(calls.get(jimTariff) != null)
          countB = calls.get(jimTariff);
        int pop = customer.getPopulation();
        //pw.printf(Locale.ENGLISH,"%f,%f,%f%n",vpf,(double)countA/pop,(double)countB/pop);
        //System.out.printf(Locale.ENGLISH,"%f,%f,%f%n",vpf,(double)countA/pop,(double)countB/pop);
        result[i][0] = vpf;
        result[i][j+1] = (double)countB/pop;
        i++;
        //reset and redo
        calls.clear();
        this.tariffSubscriptionRepo.recycle();
        subscribeTo(defaultProduction, customer.getPopulation());
      }
      i=0;
      j++;
    }
    for(i=0;i<101;i++){
        //System.out.printf(Locale.ENGLISH,"%f,%f,%f,%f%n",result[i][0],result[i][1],result[i][2],result[i][3]);
        pw2.printf(Locale.ENGLISH,"%f,%f,%f,%f%n",result[i][0],result[i][1],result[i][2],result[i][3]);

    }
    pw.close();
    pw2.close();
  }

  @Test
  public void TOUchargeTestWithPopulation() throws Exception{
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/tou_weight_pop_test.txt");
    //accessor setup
    double[] profile = {100,100,100,100,100,100,100,100,100,100,100,100,100,100,
                        100,100,100,100,100,100,100,100,100,100};
    cma.profile = profile;
    cma.brokerSwitchFactor = 0;

    //evaluator setup
    evaluator.withChunkSize(1).withInertia(0).withRationality(1)
    .withTariffSwitchFactor(0).withInconvenienceWeight(1);

    //suscribe to default 
    subscribeTo(defaultConsumption, customer.getPopulation());
    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();

    //Initiate the 2 extra tariffs
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    tariffs.add(bobTariff);

    Rate r = new Rate().withDailyBegin(0).withDailyEnd(6).withValue(-0.3);
    Rate r2 = new Rate().withDailyBegin(7).withDailyEnd(0).withValue(-0.3);
    TariffSpecification jimTS =
            new TariffSpecification(jim,
                                    PowerType.CONSUMPTION).
                                    addRate(r).addRate(r2);
    Tariff jimTariff = new Tariff(jimTS);
    initTariff(jimTariff);
    tariffs.add(jimTariff);

    //Make sure this tariff list is returned
    tariffs.add(defaultConsumption);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
    .thenReturn(tariffs);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    for(double tou = 0.0; tou < 1.01; tou += 0.1){
      //reset evaluator
      evaluator = new TariffEvaluator(cma).withChunkSize(1).withInertia(0).withRationality(1)
              .withTariffSwitchFactor(0).withInconvenienceWeight(1);
      ReflectionTestUtils.setField(evaluator,
                                   "tariffRepo", tariffRepo);
      ReflectionTestUtils.setField(evaluator,
                                   "tariffMarket", tariffMarket);
      ReflectionTestUtils.setField(evaluator,
                                   "tariffSubscriptionRepo", tariffSubscriptionRepo);

      evaluator.initializeInconvenienceFactors(tou, 0, 0, 0);
      evaluator.evaluateTariffs();

      int countA = 0, countB = 0;
//      if(calls.get(defaultConsumption) != null)
//        countdef = calls.get(defaultConsumption);
      if(calls.get(bobTariff) != null)
        countA = calls.get(bobTariff);
      if(calls.get(jimTariff) != null)
        countB = calls.get(jimTariff);
      int pop = customer.getPopulation();
      pw.printf(Locale.ENGLISH,"%f,%f,%f%n",tou,(double)countA/pop,(double)countB/pop);
      //System.out.printf(Locale.ENGLISH,"%f,%f,%f%n",tou,(double)countA/pop,(double)countB/pop);

      //reset and redo
      calls.clear();
      this.tariffSubscriptionRepo.recycle();
      subscribeTo(defaultConsumption, customer.getPopulation());
    }
    pw.close();
  }

  @Test
  public void innertiaTestWithPopulation () throws Exception
  {
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/innertia_pop_test.txt");

    RandomAccessor ac = new RandomAccessor();

    ReflectionTestUtils.setField(evaluator, "accessor", ac);

    evaluator.withInconvenienceWeight(0);
    ac.fixedTariffSample = 0;

    subscribeTo(defaultConsumption, customer.getPopulation());

    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
    .thenReturn(tariffs);

    //perform extra evaluation for  the innertia 
    for(int i = 0; i < 10 ; ++i){
      evaluator.evaluateTariffs();
    }

    //Initiate the 2 extra tariffs
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.4));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);

    tariffs.add(bobTariff);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());

    evaluator.withChunkSize(5); // 200 chunks
    evaluator.withInconvenienceWeight(0);
    for(double innertia = 0; innertia < 1.0; innertia += 0.1){
      evaluator.withInertia(innertia);
      evaluator.evaluateTariffs();
      double result = 0;
      if(calls.get(defaultConsumption) != null)
        result = -(double)calls.get(defaultConsumption)/customer.getPopulation();
      pw.printf(Locale.ENGLISH,"%f,%f%n",innertia,result);

      //reset and redo
      calls.clear();
      this.tariffSubscriptionRepo.recycle();
      subscribeTo(defaultConsumption, customer.getPopulation());

    }
    pw.close();
  }

  @Test
  public void innertiaTestNoPopulation() throws Exception
  {
    new File("data/").mkdir();
    PrintWriter pw = new PrintWriter("data/innertia_nopop_test.txt");

    // uut setup
    customer.setPopulation(1);
    customer.withMultiContracting(false);

    RandomAccessor ac = new RandomAccessor();
    //ac.fixedTariffSample = 0;

    evaluator = new TariffEvaluator(ac).
            withPreferredContractDuration(4).withRationality(1)
            .withChunkSize(1).withInconvenienceWeight(0).withTariffSwitchFactor(0);
    ReflectionTestUtils.setField(evaluator,
                                 "tariffRepo", tariffRepo);
    ReflectionTestUtils.setField(evaluator,
                                 "tariffMarket", tariffMarket);
    ReflectionTestUtils.setField(evaluator,
                                 "tariffSubscriptionRepo", tariffSubscriptionRepo);

    subscribeTo(defaultConsumption, customer.getPopulation());

    ArrayList<Tariff> tariffs = new ArrayList<Tariff>();
    tariffs.add(defaultConsumption);
    when(tariffRepo.findRecentActiveTariffs(anyInt(), any(PowerType.class)))
    .thenReturn(tariffs);

    //perform extra evaluation for  the innertia 
    for(int i = 0; i < 10 ; ++i){
      evaluator.evaluateTariffs();
    }

    //Initiate the extra tariff
    TariffSpecification bobTS =
            new TariffSpecification(bob,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.2));
    Tariff bobTariff = new Tariff(bobTS);
    initTariff(bobTariff);
    tariffs.add(bobTariff);

    // capture calls to tariffMarket
    final HashMap<Tariff, Integer> calls = new HashMap<Tariff, Integer>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        assertEquals("correct customer", customer, args[1]);
        calls.put((Tariff)args[0], (Integer)args[2]);
        return null;
      }
    }).when(tariffMarket).subscribeToTariff(any(Tariff.class),
                                            any(CustomerInfo.class),
                                            anyInt());


    for(double innertia = 0; innertia < 1.0; innertia += 0.1){
      int count = 0;
      evaluator.withInertia(innertia);
      for(int i = 0; i < 100; ++i){
        evaluator.evaluateTariffs();
        if(calls.get(bobTariff) != null)
          count++;

        //reset and redo
        calls.clear();
        this.tariffSubscriptionRepo.recycle();
        subscribeTo(defaultConsumption, customer.getPopulation());
      }
      pw.printf(Locale.ENGLISH,"%f,%f%n",innertia,(double)count/100);
    }
    pw.close();
  }

  class RandomAccessor implements CustomerModelAccessor
  {
    double brokerSwitchFactor = 1;
    double[] profile = {1.0,1.0};
    Random rnd = new Random();
    double fixedTariffSample = -1;
    double fixedInertiaSample = -1;

    RandomAccessor ()
    {
      super();
    }

    @Override
    public CustomerInfo getCustomerInfo ()
    {
      return customer;
    }

    @Override
    public double[] getCapacityProfile (Tariff tariff)
    {
      return profile;
    }

    @Override
    public double getBrokerSwitchFactor (boolean isSuperseding)
    {
      return brokerSwitchFactor;
    }

    @Override
    public double getTariffChoiceSample ()
    {
      if(fixedTariffSample == -1)
        return rnd.nextDouble();
      else
        return fixedTariffSample;
    }

    @Override
    public double getInertiaSample ()
    {
      if(fixedInertiaSample == -1)
        return rnd.nextDouble();
      else
        return fixedInertiaSample;
    }
  }

}
