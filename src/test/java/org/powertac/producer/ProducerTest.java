package org.powertac.producer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.producer.Producer.PreferredOutput;
import org.powertac.producer.Producer.ProducerAccessor;
import org.powertac.producer.fossil.SteamPlant;
import org.powertac.producer.hydro.RunOfRiver;
import org.powertac.producer.utils.Curve;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thoughtworks.xstream.XStream;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
@DirtiesContext
public class ProducerTest
{

  @Autowired
  private TimeService timeService;

  @Autowired
  private Accounting mockAccounting;

  @Autowired
  private TariffMarket mockTariffMarket;

  @Autowired
  private ServerConfiguration mockServerProperties;

  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private WeatherReportRepo weatherReportRepo;
  
  @Autowired
  private WeatherForecastRepo weatherForecastRepo;

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private RandomSeedRepo randomSeedRepo;
  
  private Configurator config;
  private Instant exp;
  private Broker broker1;
  private Instant now;
  private TariffSpecification defaultTariffSpec;
  private Tariff defaultTariff;
  private Competition comp;
  private List<Object[]> accountingArgs;

  @Before
  public void setUp ()
  {
    customerRepo.recycle();
    brokerRepo.recycle();
    tariffRepo.recycle();
    tariffSubscriptionRepo.recycle();
    randomSeedRepo.recycle();
    timeslotRepo.recycle();
    weatherReportRepo.recycle();
    weatherReportRepo.runOnce();
    reset(mockAccounting);
    reset(mockServerProperties);

    // create a Competition, needed for initialization
    comp = Competition.newInstance("producer-test");

    broker1 = new Broker("Joe");

    // now = new DateTime(2009, 10, 10, 0, 0, 0, 0,
    // DateTimeZone.UTC).toInstant();
    now = comp.getSimulationBaseTime();
    timeService.setCurrentTime(now);
    timeService.setClockParameters(now.toInstant().getMillis(), 720l, 60*60*1000);
    exp = now.plus(TimeService.WEEK * 10);

    defaultTariffSpec =
      new TariffSpecification(broker1, PowerType.PRODUCTION)
              .withExpiration(exp).addRate(new Rate().withValue(0.5));
    
    defaultTariff = new Tariff(defaultTariffSpec);
    defaultTariff.init();
    defaultTariff.setState(Tariff.State.OFFERED);

    tariffRepo.setDefaultTariff(defaultTariffSpec);

    when(mockTariffMarket.getDefaultTariff(PowerType.FOSSIL_PRODUCTION))
         .thenReturn(defaultTariff);
    when(mockTariffMarket.getDefaultTariff(PowerType.RUN_OF_RIVER_PRODUCTION))
         .thenReturn(defaultTariff);
    when(mockTariffMarket.getDefaultTariff(PowerType.SOLAR_PRODUCTION))
         .thenReturn(defaultTariff);
    when(mockTariffMarket.getDefaultTariff(PowerType.WIND_PRODUCTION))
         .thenReturn(defaultTariff);

    accountingArgs = new ArrayList<Object[]>();

    // mock the AccountingService, capture args
    doAnswer(new Answer<Object>() {
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        accountingArgs.add(args);
        return null;
      }
    }).when(mockAccounting)
            .addTariffTransaction(isA(TariffTransaction.Type.class),
                                  isA(Tariff.class), isA(CustomerInfo.class),
                                  anyInt(), anyDouble(), anyDouble());

    // Set up serverProperties mock
    config = new Configurator();

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        config.configureSingleton(args[0]);
        return null;
      }
    }).when(mockServerProperties).configureMe(anyObject());

    
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("common.competition.expectedTimeslotCount", "1440");
    Configuration mapConfig = new MapConfiguration(map);
    config.setConfiguration(mapConfig);
    config.configureSingleton(comp);
  }
  
  @Test
  public void testFossilSerialize ()
  {
    
    SteamPlant plant = new SteamPlant(10000, 2000, -500000);
    XStream x = new XStream();
    x.autodetectAnnotations(true);
    String out = x.toXML(plant);
    plant = (SteamPlant) x.fromXML(out);
    assertNotNull(plant.customerInfo);
    assertNotNull(plant.customerRepo);
    assertNotNull(plant.name);
    assertNotNull(plant.producerAccessor);
    assertNotNull(plant.randomSeedRepo);
    assertNotNull(plant.seed);
    assertNotNull(plant.tariffEvaluationHelper);
    assertNotNull(plant.tariffEvaluator);
    assertNotNull(plant.tariffMarketService);
    assertNotNull(plant.tariffSubscriptionRepo);
    assertNotNull(plant.timeService);
    assertNotNull(plant.timeslotService);
    assertNotNull(plant.weatherForecastRepo);
    assertNotNull(plant.weatherReportRepo);
    assertTrue(plant.preferredOutput == plant.upperPowerCap);
    assertTrue(plant.upperPowerCap != 0);
  }
  
  @Test
  public void testRunofRiverSerialize ()
  {
    Curve inputFlow = new Curve();
    double[] ys = {1.478,4.200,3.147,1.249,0.779,1.658,3.952,3.380,1.911,1.072,
                  0.632,0.422,0.278,0.189,0.282,5.181,1.510,2.466,2.597,3.388,
                  3.731,2.367,1.172,2.233,7.465,1.838,2.575,4.050,6.299,7.258,
                  2.119,2.095,1.188,0.674,1.494,2.088,1.687,1.393,5.438,1.498,
                  1.068,0.958,7.255,1.356,1.442,0.837,0.532,0.451,0.385,0.397,
                  0.266,0.331,0.586,0.684,4.748,4.081,3.892,2.155,3.136,2.657,
                  4.408,2.300,1.063,2.828,3.494,2.103,2.439,4.418,2.645,1.572,
                  1.550,2.999,3.946,2.296,2.155,2.349,1.577,0.909,0.704,2.282,
                  1.450,0.932,0.746,0.484,0.361,0.296,0.253,0.264,0.220,0.203,
                  0.195,0.263,0.341,1.594,1.328,1.058,2.878,0.718,0.528,0.387,
                  0.268,0.220,0.193,0.291,0.253,0.228,0.170,0.149,0.125,0.116,
                  0.130,0.250,0.218,0.156,0.137,0.115,0.105,0.103,0.119,0.406,
                  0.410,6.439,2.978,1.379,1.312,0.616,0.371,0.256,0.214,0.175,
                  0.148,0.119,0.100,0.083,0.077,0.097,0.087,0.084,0.083,0.078,
                  0.080,0.233,0.242,0.257,0.749,0.448,0.274,0.614,0.626,0.283,
                  0.175,0.147,0.114,0.085,0.072,0.065,0.075,0.077,0.065,0.060,
                  0.057,0.194,0.217,0.113,0.088,0.509,0.274,0.146,0.096,0.078,
                  0.088,0.072,0.057,0.048,0.047,0.050,0.059,0.110,0.267,0.137,
                  0.524,0.373,0.612,0.423,1.173,0.693,0.395,0.416,0.274,0.539,
                  0.332,0.196,0.149,0.124,0.997,1.267,0.432,0.386,0.222,0.149,
                  0.114,0.092,0.076,0.063,0.058,0.058,0.054,0.047,0.044,0.043,
                  0.041,0.043,0.126,0.080,0.062,0.053,0.055,0.124,0.086,0.466,
                  0.196,0.137,0.147,0.691,0.512,0.239,0.489,2.408,4.195,3.547,
                  3.645,3.786,4.669,4.040,2.805,5.952,3.559,2.267,1.144,1.705,
                  5.231,2.095,1.159,0.762,0.505,2.967,2.293,3.346,3.407,4.923,
                  2.327,8.239,4.889,3.815,4.096,1.663,1.133,1.565,1.394,0.849,
                  0.805,2.371,5.286,2.031,2.470,2.933,7.810,3.650,5.703,5.035,
                  4.136,1.497,7.962,8.858,2.012,1.258,2.711,1.323,0.748,0.573,
                  0.443,0.350,0.310,0.272,0.688,0.671,0.377,0.411,1.831,2.838,
                  2.787,3.282,1.880,1.703,1.764,1.785,1.773,1.223,1.892,0.968,
                  0.624,5.239,3.996,3.446,1.694,1.553,0.945,1.122,3.178,4.589,
                  1.375,0.980,3.189,2.515,5.211,2.724,1.993,1.457,8.642,2.711,
                  8.083,4.706,2.587,2.694,2.828,2.784,2.419,1.951,2.995,1.503,
                  1.365,1.452,1.010,0.724,0.521,0.430,0.389,4.168,1.603,0.994,
                  2.273,2.480,1.333,1.023,0.599,0.420,0.528,7.982,6.061,1.906,
                  1.112,7.080,6.820,4.021,1.622,0.850,7.273,3.291,5.765,3.105,
                  2.546,1.809,2.215,4.255,5.606};
    int i = 1;
    for(double y: ys){
      inputFlow.add(i, y);
      i++;
    }
    RunOfRiver plant = new RunOfRiver(inputFlow,0,20,inputFlow,1000,60,1,-1000);
    XStream x = new XStream();
    x.autodetectAnnotations(true);
    String out = x.toXML(plant);
    plant = (RunOfRiver) x.fromXML(out);
    assertNotNull(plant.customerInfo);
    assertNotNull(plant.customerRepo);
    assertNotNull(plant.name);
    assertNotNull(plant.producerAccessor);
    assertNotNull(plant.randomSeedRepo);
    assertNotNull(plant.seed);
    assertNotNull(plant.tariffEvaluationHelper);
    assertNotNull(plant.tariffEvaluator);
    assertNotNull(plant.tariffMarketService);
    assertNotNull(plant.tariffSubscriptionRepo);
    assertNotNull(plant.timeService);
    assertNotNull(plant.timeslotService);
    assertNotNull(plant.weatherForecastRepo);
    assertNotNull(plant.weatherReportRepo);
    assertTrue(plant.preferredOutput == plant.upperPowerCap);
    assertTrue(plant.upperPowerCap != 0);
    assertTrue(plant.timeslotLengthInMin == 60);
  }
  
  @Test
  public void testSubscribeDefault(){
    final SteamPlant plant = new SteamPlant(10000, 2000, -500000);
    assertNull(plant.getCurrentSubscription());
    
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer (InvocationOnMock invocation) throws Throwable
      {
        assertTrue((Tariff)invocation.getArguments()[0] == defaultTariff);
        assertTrue((CustomerInfo)invocation.getArguments()[1] == 
                plant.getCustomerInfo());
        assertTrue((int)invocation.getArguments()[2] == 1);
        TariffSubscription sub = new TariffSubscription(
                                                        plant.getCustomerInfo(),
                                                        defaultTariff);
        sub.subscribe(1);
        tariffSubscriptionRepo.add(sub);
        return null;
      }
      
    }).when(mockTariffMarket).subscribeToTariff(defaultTariff,
                                               plant.getCustomerInfo(),
                                               1);
    plant.subscribeDefault();
    assertNotNull(plant.getCurrentSubscription());
    assertEquals(plant.getCurrentSubscription().getTariff(), defaultTariff);
  }
  
  @Test
  public void testEvaluateTariffs(){
    SteamPlant plant = new SteamPlant(10000, 2000, -500000);
    
    TariffEvaluator te = mock(TariffEvaluator.class);
    plant.setTariffEvaluator(te);
    
    plant.evaluateNewTariffs();
    verify(te).evaluateTariffs();
    
    
    ProducerAccessor accessor = mock(ProducerAccessor.class);
    when(accessor.generateOutput(any(Tariff.class), anyInt()))
      .thenReturn(new PreferredOutput(plant.upperPowerCap, new double[24]));
    plant.setProducerAccessor(accessor);
    
    TariffSubscription sub = new TariffSubscription(plant.customerInfo, defaultTariff);
    sub.subscribe(1);
    tariffSubscriptionRepo.add(sub);
    
    plant.evaluateNewTariffs();
    verify(te,times(2)).evaluateTariffs();
    verify(accessor).generateOutput(any(Tariff.class), anyInt());
    assertTrue(plant.currentSubscription == sub);
  }
  
  @Test
  public void testStep(){
    SteamPlant plant = new SteamPlant(10000, 2000, -500000);
    SteamPlant spy = spy(plant);
    doNothing().when(spy).consumePower();
    spy.step();
    verify(spy).consumePower();
  }
  
  @Test
  public void testCalculateOutput(){
    // TODO
    SteamPlant plant = new SteamPlant(10000, 2000, -500000);
    List<WeatherForecastPrediction> predictions = new ArrayList<>();
    predictions.add(new WeatherForecastPrediction(1, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(2, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(3, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(4, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(5, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(6, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(7, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(8, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(9, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(10, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(11, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(12, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(13, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(14, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(15, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(16, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(17, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(18, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(19, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(20, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(21, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(22, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(23, 22, 5, 0.5, 0));
    predictions.add(new WeatherForecastPrediction(24, 22, 5, 0.5, 0));

    assertTrue(predictions.size() == 24);
    
    WeatherForecast forecast = new WeatherForecast(timeslotRepo.currentSerialNumber()
                                                   , predictions);
    weatherForecastRepo.add(forecast);
    
    Rate r = new Rate().withDailyBegin(9).withDailyEnd(13).withValue(-1.0);
    //Rate r2 = new Rate().withDailyBegin(13).withDailyEnd(9).withValue(+1);
    //Rate r3 = new Rate().withValue(-0.5);
        
    defaultTariff.getTariffSpecification().addRate(r);
    //defaultTariff.getTariffSpecification().addRate(r2);
    //defaultTariff.getTariffSpecification().addRate(r3);
    
    assertTrue(defaultTariff.init());
    defaultTariff.setState(Tariff.State.OFFERED);
    double mon = defaultTariff.getUsageCharge(timeslotRepo.getTimeForIndex(10), -1, 0);
    assertTrue(mon < 0);
    mon = defaultTariff.getUsageCharge(timeslotRepo.getTimeForIndex(10), -100000, 0);
    assertTrue(mon < 0);
    mon = defaultTariff.getUsageCharge(timeslotRepo.getTimeForIndex(10), -100000, -125000000);
    assertTrue(mon < 0);
    assertFalse(defaultTariff.isTiered());
    
    double pref = plant.producerAccessor.generateOutput(defaultTariff, 24).preferredOutput;
    double[] out = plant.producerAccessor.generateOutput(defaultTariff, 24).output;
    
    assertTrue(out.length == 24);
    
    assertEquals(plant.getUpperPowerCap(), pref,1000);
    
    for(double i: out){
      if(i == 0){
        return;
      }
    }
    fail("Shouldn't be reachable");
  }
  
  @Test
  public void testProducePower(){
    SteamPlant plant = new SteamPlant(10000, 2000, -500000);
    
    WeatherReport report = new WeatherReport(5,22, 5, 0.5, 0);
    
    WeatherReportRepo rep = mock(WeatherReportRepo.class);
    when(rep.currentWeatherReport()).thenReturn(report);
    plant.setWeatherReportRepo(rep);
    
    TariffSubscription sub = mock(TariffSubscription.class);
    when(sub.getTariff()).thenReturn(defaultTariff);
    plant.setCurrentSubscription(sub);
    
    plant.consumePower();
    verify(rep).currentWeatherReport();
    verify(sub).usePower(anyDouble());
    verify(sub).getTariff();
  }

}
