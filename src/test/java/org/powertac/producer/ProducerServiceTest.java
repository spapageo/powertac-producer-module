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
import org.junit.After;
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
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config-service.xml" })
@DirtiesContext
public class ProducerServiceTest
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
  private TariffSubscriptionRepo mockTariffSubscriptionRepo;

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
  
  @Autowired
  private ProducerService producerService;
  
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
    reset(mockTariffSubscriptionRepo);
    randomSeedRepo.recycle();
    timeslotRepo.recycle();
    weatherReportRepo.recycle();
    weatherReportRepo.runOnce();
    producerService.clearConfiguration();
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
  
  @After
  public void tearDown ()
  {
    timeService = null;
    mockAccounting = null;
    mockTariffMarket = null;
    mockServerProperties = null;
    tariffRepo = null;
    customerRepo = null;
    mockTariffSubscriptionRepo = null;
    timeslotRepo = null;
    weatherReportRepo = null;
    brokerRepo = null;
    randomSeedRepo = null;
    config = null;
    exp = null;
    broker1 = null;
    now = null;
    defaultTariffSpec = null;
    defaultTariff = null;
    comp = null;
    accountingArgs = null;
    producerService = null;
  }
  
  @Test
  public void testActivate ()
  {
    fail("Not yet implemented");
  }

  @Test
  public void testInitialize ()
  {
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    
    TariffSubscription sub = mock(TariffSubscription.class);
    
    List<TariffSubscription> l = new ArrayList<>();
    l.add(sub);
        
    
    when(mockTariffSubscriptionRepo.
         findActiveSubscriptionsForCustomer(any(CustomerInfo.class)))
    .thenReturn(l);

    producerService.initialize(comp, inits);
    assertTrue(producerService.getProducerFileFolder().contains("conf"));
    assertTrue(producerService.getProducerList().size() > 0);
    assertTrue(producerService.getProducerList().get(0).currentSubscription != null);
  }

  @Test
  public void testLoadProducers ()
  {
    fail("Not yet implemented");
  }

  @Test
  public void testPublishNewTariffs ()
  {
    fail("Not yet implemented");
  }

}
