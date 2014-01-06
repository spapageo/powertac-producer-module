/**
 * 
 */
package org.powertac.producer;

import java.util.ArrayList;
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
  private TimeslotRepo timeslotRepo;

  private List<Producer> producerList = new ArrayList<>();
  
  public ProducerService ()
  {
    super();
  }
  
  @Override
  public void setDefaults ()
  {
    // Nothing to do here
  }

  @Override
  public String
    initialize (Competition competition, List<String> completedInits)
  {
    int index = completedInits.indexOf("DefaultBroker");
    if (index == -1) {
      return null;
    }

    //TODO this does nothing write now
    serverPropertiesService.configureMe(this);

    //Clear the list of producers and create new ones
    producerList.clear();

    tariffMarketService.registerNewTariffListener(this);

    //TODO take care of deserialization and configuration configureMe()

    super.init();
    
    return "Producer";
  }

  @Override
  public void publishNewTariffs (List<Tariff> tariffs)
  {
    for (Producer producer: producerList)
      producer.evaluateNewTariffs();
  }

  @Override
  public void activate (Instant time, int phaseNumber)
  {
    for (Producer producer: producerList)
      producer.step();
  }

}
