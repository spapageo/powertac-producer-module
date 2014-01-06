/**
 * 
 */
package org.powertac.producer;

import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.Tariff;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Spyros Papageorgiou
 * 
 */
@Service
public class ProducerService extends TimeslotPhaseProcessor
  implements NewTariffListener, InitializationService
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  static final private Logger log = Logger.getLogger(ProducerService.class
          .getName());

  @Autowired
  private TariffMarket tariffMarketService;

  @Autowired
  private ServerConfiguration serverPropertiesService;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Override
  public void setDefaults ()
  {
    // TODO Auto-generated method stub

  }

  @Override
  public String
    initialize (Competition competition, List<String> completedInits)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void publishNewTariffs (List<Tariff> tariffs)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void activate (Instant time, int phaseNumber)
  {
    // TODO Auto-generated method stub

  }

}
