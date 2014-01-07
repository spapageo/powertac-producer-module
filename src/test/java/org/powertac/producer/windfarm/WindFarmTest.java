package org.powertac.producer.windfarm;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.Competition;
import org.powertac.common.TimeService;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.producer.utils.Curve;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thoughtworks.xstream.XStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
@DirtiesContext
public class WindFarmTest
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
  private BrokerRepo brokerRepo;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Before
  public void setup(){

    tariffRepo.recycle();

    tariffSubscriptionRepo.recycle();

    timeslotRepo.recycle();

    weatherReportRepo.recycle();

    randomSeedRepo.recycle();

    brokerRepo.recycle();

    customerRepo.recycle();
    
    Competition.newInstance("Wind Farm Test");
  }

  @Test
  public void testSerialize ()
  {
    double[] x =
          { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25 };
    double[] y =
          { 0, -66, -166, -288, -473, -709, -1000, -1316, -1651, -1860, -1968, -2000,
            -2000, -2000, -2000, -2000, -2000, -2000, -2000, -2000, -2000, -2000, -2000 };
    Curve c = new Curve(x, y);
    c.setCustomLastValue(0);
    WindFarm wf = new WindFarm();
    WindTurbine wt = new WindTurbine(22, 0.01, -2000, 80, c);
    wf.addWindTurbine(wt);
    wf.addWindTurbine(wt);
    wf.addWindTurbine(wt);
    wf.addWindTurbine(wt);
    wf.addWindTurbine(wt);
    wf.addWindTurbine(wt);
    wf.addWindTurbine(wt);
    wf.addWindTurbine(wt);
    wf.addWindTurbine(wt);
    wf.addWindTurbine(wt);
    wf.addWindTurbine(wt);
    wf.addWindTurbine(wt);
    wf.addWindTurbine(wt);

    XStream xstream = new XStream();
    xstream.autodetectAnnotations(true);
    String out = xstream.toXML(wf);
    wf = (WindFarm) xstream.fromXML(out);
    
    assertTrue(wf.getTurbineList().get(3).getPowerCurve() != null);
    assertTrue(wf.getTurbineList().get(3).getPowerCurve().getCustomLastValue() == 0);
  }

}
