/**
 * 
 */
package org.powertac.producer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.Tariff;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.exceptions.PowerTacException;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.producer.fossil.SteamPlant;
import org.powertac.producer.hydro.Dam;
import org.powertac.producer.hydro.HydroBase;
import org.powertac.producer.hydro.RunOfRiver;
import org.powertac.producer.pvfarm.PvPanel;
import org.powertac.producer.pvfarm.SolarFarm;
import org.powertac.producer.windfarm.WindFarm;
import org.powertac.producer.windfarm.WindTurbine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thoughtworks.xstream.XStream;

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

  //The place where the xml files of the producers are stored
  private String producerFileFolder;
  
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

    serverPropertiesService.configureMe(this);
    if(producerFileFolder == null)
      producerFileFolder = ProducerService.class.getResource("/conf").toString();
    log.info("The configuration folder is located at: " + producerFileFolder);
    
    //Clear the list of producers and create new ones
    producerList.clear();

    tariffMarketService.registerNewTariffListener(this);

    //TODO take care of deserialization
    try {
      producerList = loadProducers();
    }
    catch (IOException e) {
      throw new PowerTacException(e);
    }
    
    
    //Make sure producers subscribe to the default tariff
    for(Producer producer : producerList)
      producer.subscribeDefault();
    
    super.init();
    
    return "Producer";
  }
  
  protected List<Producer> loadProducers() throws IOException{
    File confFolder = new File(new URL(this.producerFileFolder).getFile());
    List<Producer> list = new ArrayList<>();
    if(!confFolder.isDirectory() || !confFolder.exists()){
      log.error("The supplied configuration path was invalid.");
      return list;
    }
    
    FileFilter filter = new FileFilter() {
      @Override
      public boolean accept (File pathname)
      {
        String name = pathname.toString().toLowerCase();
        if(pathname.isFile() && name.endsWith(".xml")){
          return true;
        }
        return false;
      }
    };
    
    XStream xstream = new XStream();
    xstream.processAnnotations(SteamPlant.class);
    xstream.processAnnotations(Dam.class);
    xstream.processAnnotations(RunOfRiver.class);
    xstream.processAnnotations(HydroBase.class);
    xstream.processAnnotations(WindFarm.class);
    xstream.processAnnotations(WindTurbine.class);
    xstream.processAnnotations(SolarFarm.class);
    xstream.processAnnotations(PvPanel.class);
    xstream.processAnnotations(Producer.class);


    for(File conf : confFolder.listFiles(filter)){
      String name = conf.toString().toLowerCase();
      if (name.contains("steam") || name.contains("dam") || name.contains("river")
              || name.contains("solar") || name.contains("wind")) {
        Producer producer = (Producer) xstream.fromXML(conf);
        list.add(producer);
      }
    }
    
    
    return list;
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

  /**
   * @return the producerFileFolder
   */
  public String getProducerFileFolder ()
  {
    return producerFileFolder;
  }

  /**
   * This function cleans the configuration files in case they have not been
   * cleaned at the beginning of the game
   */
  public void clearConfiguration ()
  {
    producerFileFolder = null;
  }
  
  /**
   * @param producerFileFolder the producerFileFolder to set
   */
  @ConfigurableValue(valueType="String",description="The root of the xml configurations files.")
  public void setProducerFileFolder (String producerFileFolder)
  {
    this.producerFileFolder = producerFileFolder;
  }

  /**
   * @return the producerList
   */
  public List<Producer> getProducerList ()
  {
    return Collections.unmodifiableList(producerList);
  }

  /**
   * @param producerList the producerList to set
   */
  public void setProducerList (List<Producer> producerList)
  {
    this.producerList = producerList;
  }

}
