/**
 * 
 */
package org.powertac.producer;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.IdGenerator;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluationHelper;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.common.spring.SpringApplicationContext;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * @author Spyros Papageorgiou
 * 
 */
public abstract class Producer
{

  static protected Logger log = Logger.getLogger(Producer.class.getName());

  // Percentage increment for the calculation of the preferred output
  private static final double step = 0.1;
  private static final double TOU_FACTOR = 0.05;
  private static final double TIERED_RATE_FACTOR = 0.1;
  private static final double VARIABLE_PRICING_FACTOR = 0.7;
  private static final double INTERRUPTIBILITY_FACTOR = 0.5;
  private static final double WEIGHT_INCONVENIENCE = 1;
  private static final double INNERTIA = 0.1;
  private static final double RATIONALITY_FACTOR = 0.9;
  private static final int TARIFF_COUNT = 5;
  private static final double BROKER_SWITCH_FACTOR = 0.02;

  
  @XStreamOmitField
  protected WeatherReportRepo weatherReportRepo;
  @XStreamOmitField
  protected WeatherForecastRepo weatherForecastRepo;
  @XStreamOmitField
  protected TimeslotRepo timeslotService;
  @XStreamOmitField
  protected TimeService timeService;
  @XStreamOmitField
  protected TariffMarket tariffMarketService;
  @XStreamOmitField
  protected TariffSubscriptionRepo tariffSubscriptionRepo;
  @XStreamOmitField
  protected CustomerRepo customerRepo;
  @XStreamOmitField
  protected RandomSeedRepo randomSeedRepo;

  @XStreamOmitField
  protected TariffEvaluator tariffEvaluator;
  @XStreamOmitField
  protected TariffEvaluationHelper tariffEvaluationHelper;
  @XStreamOmitField
  protected RandomSeed seed;
  @XStreamOmitField
  protected TariffSubscription currentSubscription = null;
  @XStreamOmitField
  protected ProducerAccessor producerAccessor;
  
  @XStreamOmitField
  protected double preferredOutput;
  @XStreamOmitField
  protected CustomerInfo customerInfo;
  @XStreamOmitField
  protected long custId;
  
  protected double upperPowerCap;
  @XStreamOmitField
  protected String name;
  @XStreamOmitField
  protected int timeslotLengthInMin;


  /**
   * The constructor of a producer.
   * 
   * @param name
   *          the plant name
   * @param powerType
   *          The power type
   * @param profileHours
   *          The number of hours to profile the plant output
   * @param capacity
   *          the powerplant capacity in kw < 0
   */
  public Producer (String name, PowerType powerType, int profileHours,
                   double capacity)
  {

    if (capacity > 0)
      throw new IllegalArgumentException("Positive plant capacity");

    initialize(name, powerType, profileHours, capacity,IdGenerator.createId());
  }

  protected void initialize(String producerName, PowerType powerType, int profileHours,
                          double capacity, long id){
    // Initialize all the repositories
    weatherReportRepo = (WeatherReportRepo) SpringApplicationContext
            .getBean("weatherReportRepo");
    weatherForecastRepo = (WeatherForecastRepo) SpringApplicationContext
              .getBean("weatherForecastRepo");
    timeslotService = (TimeslotRepo) SpringApplicationContext
            .getBean("timeslotService");
    timeService = (TimeService) SpringApplicationContext
            .getBean("timeService");
    customerRepo = (CustomerRepo) SpringApplicationContext
            .getBean("customerRepo");
    tariffMarketService = (TariffMarket) SpringApplicationContext
            .getBean("tariffMarketService");
    randomSeedRepo = (RandomSeedRepo) SpringApplicationContext
            .getBean("randomSeedRepo");
    tariffSubscriptionRepo = (TariffSubscriptionRepo) SpringApplicationContext
            .getBean("tariffSubscriptionRepo");
    
    this.timeslotLengthInMin = Competition.currentCompetition()
            .getTimeslotLength();
    
    //Initialize the customer info with population 1
    customerInfo = new CustomerInfo(producerName, 1);
    customerInfo.withPowerType(powerType);
    customerRepo.add(customerInfo);

    //Initialize the custom object id
    this.custId = id;
    this.name = producerName + id;
    //Initialize the random seed
    seed = randomSeedRepo.getRandomSeed(producerName, (int) id, "Misc");
    
    //Initialize the evaluation helper and the tariff evaluator
    tariffEvaluationHelper = new TariffEvaluationHelper();
    
    producerAccessor = new ProducerAccessor(this, profileHours);

    tariffEvaluator = new TariffEvaluator(producerAccessor);

    tariffEvaluator.initializeInconvenienceFactors(TOU_FACTOR,
                                                   TIERED_RATE_FACTOR,
                                                   VARIABLE_PRICING_FACTOR,
                                                   INTERRUPTIBILITY_FACTOR);

    double weight = seed.nextDouble() * WEIGHT_INCONVENIENCE;

    tariffEvaluator.withInconvenienceWeight(weight).withInertia(INNERTIA)
            .withRationality(RATIONALITY_FACTOR)
            .withTariffEvalDepth(TARIFF_COUNT)
            .withTariffSwitchFactor(BROKER_SWITCH_FACTOR);

    this.upperPowerCap = capacity;
    this.preferredOutput = capacity;
  }
  
  public void consumePower ()
  {
    // We need to get the Weather report and
    // then produced power for the active tariff
    WeatherReport report = weatherReportRepo.currentWeatherReport();

    if (currentSubscription != null && report != null){
      double power = getOutput(report);
      
      if(currentSubscription.getTariff()
              .getUsageCharge(power, currentSubscription.getTotalUsage(),false) > 0 )
        currentSubscription.usePower(power);
      else
        // We aren't getting payed for the power production so close it down;
        log.debug("Didn't produced power due to negative payment");
    }else{
      log.error("No active subscription or null weather report");
    }
  }

  abstract protected double getOutput (WeatherReport weatherReport);

  abstract protected double
    getOutput (int timeslotIndex,
               WeatherForecastPrediction weatherForecastPrediction);

  public void step ()
  {
    // We produce power here for the active tariff
    // We must call consumePower
    consumePower();
  }

  public void subscribeDefault ()
  {
    PowerType type = customerInfo.getPowerType();
    if (tariffMarketService.getDefaultTariff(type) == null) {
      log.info("No default Subscription for type " + type.toString() + " of "
               + this.toString() + " to subscribe to.");
    }
    else {
      tariffMarketService.subscribeToTariff(tariffMarketService
              .getDefaultTariff(type), customerInfo, customerInfo.getPopulation());
      currentSubscription = tariffSubscriptionRepo
      .findActiveSubscriptionsForCustomer(customerInfo).get(0);
      log.info("CustomerInfo of type " + type.toString() + " of "
               + this.toString()
               + " was subscribed to the default broker successfully.");
    }
  }

  public void evaluateNewTariffs ()
  {
    tariffEvaluator.evaluateTariffs();

    // check if the active tariff changed and recalculate the preferred
    // output
    List<TariffSubscription> subscriptions =
      tariffSubscriptionRepo
              .findActiveSubscriptionsForCustomer(customerInfo);

    if (subscriptions.size() > 0) {
      if (subscriptions.get(0) != currentSubscription) {
        // update preferred output
        currentSubscription = subscriptions.get(0);
        preferredOutput =
          producerAccessor.generateOutput(currentSubscription.getTariff(),
                                          producerAccessor.hours).preferredOutput;
      }
    }
  }
  
  abstract protected Object readResolve();
  
  /**
   * The most important function of this class is to generated the
   * hypothetical load under a specified tariff. These allows the
   * TariffEvaluatorHelper to choose the best tariff of the ones that are
   * available.
   * 
   * @author Spyros Papageorgiou
   * 
   */
  protected static class ProducerAccessor implements CustomerModelAccessor
  {
    
    private Producer parent;
    private int hours;

    public ProducerAccessor (Producer parent, int hours)
    {
      if (hours <= 0 || parent == null)
        throw new IllegalArgumentException(
                         "Negative or zero duration for the Customer profile");
      this.parent = parent;
      this.hours = hours;
    }

    /**
     * Generates the producer's output for N hours in the future Care: this
     * doesn't make sense for the weather sensitive producers, they should
     * ignore the suggested preferred output or set the hours to 24 which by
     * itself isn't very helpful.
     * 
     * @return
     */
    protected PreferredOutput generateOutput (Tariff tariff, int profileHours)
    {
      // try and collect forecasts for the requested number of hours
      SortedMap<Integer, WeatherForecastPrediction> predictions =
        new TreeMap<>();
      
      WeatherForecast forecast = parent.weatherForecastRepo.currentWeatherForecast();
      
      if(forecast == null){
        log.error("Got zero weather forecasts on the creation of the customer profile");
        return new PreferredOutput(parent.preferredOutput, new double[0]);
      }
      
      boolean quit = false;
      int k = 0;
      
      //if the forecasts aren't enough we replicate the rest of the data
      //Non weather sensitive producer we want this like the fossil plants
      while(!quit){
        for (WeatherForecastPrediction prediction: forecast.getPredictions()) {


          if (predictions.size() >= profileHours){
            quit = true;
            break;
          }else{
            predictions.put(prediction.getForecastTime()
                            + forecast.getTimeslotIndex()
                            + k*forecast.getPredictions().size(), prediction);
          }
            
        }
        k++;
      }

      Iterator<Integer> slotIter = predictions.keySet().iterator();

      // Since production is not an
      // interruptible power type
      // we don't bother checking for curtailment.

      // Instead we can modify the max plant preferred output and select
      // the lowest one that
      // provided the highest payments

      // we want to maximize this
      double maxPayment = Double.NEGATIVE_INFINITY;
      // the cached output for which we get the best money
      double[] maxOuput = null;
      // the cached preferred output for which we get maximum money
      double maxPreferredOutput = 0;
      // save the preferred output to restore later
      double savePreferredOutput = parent.preferredOutput;

      // CARE Careful on the signs
      double sum = 0;
      for (parent.preferredOutput = 0; parent.preferredOutput >= parent.upperPowerCap;
              parent.preferredOutput += step * parent.upperPowerCap) {
        
        //Here we create the usage vector
        double[] out = new double[predictions.size()];
        for (int i = 0; i < out.length; i++) {
          int timeslot = slotIter.next();
          double usage = parent.getOutput(timeslot, predictions.get(timeslot));
          double charge = tariff.getUsageCharge(parent.timeslotService.getTimeForIndex(timeslot),
                                                usage,sum);
          System.out.println(charge + " " + timeslot + " " + usage);
          
          if(charge > 0){
            out[i] = usage;
            sum += usage;
          }else{
            out[i] = 0;
          }
        }
        // calculate the money
        double money = parent.tariffEvaluationHelper.estimateCost(tariff, out, true);
        
        //Check if it more advantageous
        if (money > maxPayment) {
          maxPayment = money;
          maxOuput = out;
          maxPreferredOutput = parent.preferredOutput;
        }
        // reset the iterator
        slotIter = predictions.keySet().iterator();
      }

      // restore the preferred output
      parent.preferredOutput = savePreferredOutput;

      return new PreferredOutput(maxPreferredOutput, maxOuput);
    }
    
    @Override
    public CustomerInfo getCustomerInfo ()
    {
      return parent.customerInfo;
    }

    @Override
    public double[] getCapacityProfile (Tariff tariff)
    {
      return generateOutput(tariff, hours).output;
    }

    @Override
    public double getBrokerSwitchFactor (boolean isSuperseding)
    {
      double result = BROKER_SWITCH_FACTOR;
      if (isSuperseding)
        return result * 5.0;
      return result;
    }

    @Override
    public double getTariffChoiceSample ()
    {
      return parent.seed.nextDouble();
    }

    @Override
    public double getInertiaSample ()
    {
      return parent.seed.nextDouble();
    }

  }

  protected static class PreferredOutput
  {
    public PreferredOutput (double preferredOutput, double[] output)
    {
      if (output == null)
        throw new IllegalArgumentException("Null output");

      this.output = output;
      this.preferredOutput = preferredOutput;
    }

    double preferredOutput = 0;
    double[] output = null;
  }

  /**
   * @return the custId
   */
  public long getCustId ()
  {
    return custId;
  }

  /**
   * @param custId the custId to set
   */
  public void setCustId (long custId)
  {
    this.custId = custId;
  }

  /**
   * @return the upperPowerCap
   */
  public double getUpperPowerCap ()
  {
    return upperPowerCap;
  }

  /**
   * @param upperPowerCap the upperPowerCap to set
   */
  public void setUpperPowerCap (double upperPowerCap)
  {
    if(upperPowerCap > 0)
      throw new IllegalArgumentException("Positive capacity");
    this.upperPowerCap = upperPowerCap;
  }

  /**
   * @return the name
   */
  public String getName ()
  {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName (String name)
  {
    if(name != null)
      throw new IllegalArgumentException();
    this.name = name;
  }

  /**
   * @return the step
   */
  public static double getStep ()
  {
    return step;
  }

  /**
   * @return the touFactor
   */
  public static double getTouFactor ()
  {
    return TOU_FACTOR;
  }

  /**
   * @return the tieredRateFactor
   */
  public static double getTieredRateFactor ()
  {
    return TIERED_RATE_FACTOR;
  }

  /**
   * @return the variablePricingFactor
   */
  public static double getVariablePricingFactor ()
  {
    return VARIABLE_PRICING_FACTOR;
  }

  /**
   * @return the interruptibilityFactor
   */
  public static double getInterruptibilityFactor ()
  {
    return INTERRUPTIBILITY_FACTOR;
  }

  /**
   * @return the weightInconvenience
   */
  public static double getWeightInconvenience ()
  {
    return WEIGHT_INCONVENIENCE;
  }

  /**
   * @return the innertia
   */
  public static double getInnertia ()
  {
    return INNERTIA;
  }

  /**
   * @return the rationalityFactor
   */
  public static double getRationalityFactor ()
  {
    return RATIONALITY_FACTOR;
  }

  /**
   * @return the tariffCount
   */
  public static int getTariffCount ()
  {
    return TARIFF_COUNT;
  }

  /**
   * @return the brokerSwitchFactor
   */
  public static double getBrokerSwitchFactor ()
  {
    return BROKER_SWITCH_FACTOR;
  }

  /**
   * @return the weatherReportRepo
   */
  public WeatherReportRepo getWeatherReportRepo ()
  {
    return weatherReportRepo;
  }

  /**
   * @return the weatherForecastRepo
   */
  public WeatherForecastRepo getWeatherForecastRepo ()
  {
    return weatherForecastRepo;
  }

  /**
   * @return the timeslotService
   */
  public TimeslotRepo getTimeslotService ()
  {
    return timeslotService;
  }

  /**
   * @return the timeService
   */
  public TimeService getTimeService ()
  {
    return timeService;
  }

  /**
   * @return the tariffMarketService
   */
  public TariffMarket getTariffMarketService ()
  {
    return tariffMarketService;
  }

  /**
   * @return the tariffSubscriptionRepo
   */
  public TariffSubscriptionRepo getTariffSubscriptionRepo ()
  {
    return tariffSubscriptionRepo;
  }

  /**
   * @return the customerRepo
   */
  public CustomerRepo getCustomerRepo ()
  {
    return customerRepo;
  }

  /**
   * @return the randomSeedRepo
   */
  public RandomSeedRepo getRandomSeedRepo ()
  {
    return randomSeedRepo;
  }

  /**
   * @return the tariffEvaluator
   */
  public TariffEvaluator getTariffEvaluator ()
  {
    return tariffEvaluator;
  }

  /**
   * @return the tariffEvaluationHelper
   */
  public TariffEvaluationHelper getTariffEvaluationHelper ()
  {
    return tariffEvaluationHelper;
  }

  /**
   * @return the seed
   */
  public RandomSeed getSeed ()
  {
    return seed;
  }

  /**
   * @return the currentSubscription
   */
  public TariffSubscription getCurrentSubscription ()
  {
    return currentSubscription;
  }

  /**
   * @return the producerAccessor
   */
  public ProducerAccessor getProducerAccessor ()
  {
    return producerAccessor;
  }

  /**
   * @return the preferredOutput
   */
  public double getPreferredOutput ()
  {
    return preferredOutput;
  }

  /**
   * @return the customerInfo
   */
  public CustomerInfo getCustomerInfo ()
  {
    return customerInfo;
  }

  /**
   * @return the timeslotLengthInMin
   */
  public int getTimeslotLengthInMin ()
  {
    return timeslotLengthInMin;
  }

  /**
   * @param timeslotLengthInMin the timeslotLengthInMin to set
   */
  public void setTimeslotLengthInMin (int timeslotLengthInMin)
  {
    if(timeslotLengthInMin <= 0)
      throw new IllegalArgumentException();
    this.timeslotLengthInMin = timeslotLengthInMin;
  }

  /**
   * @param weatherReportRepo the weatherReportRepo to set
   */
  public void setWeatherReportRepo (WeatherReportRepo weatherReportRepo)
  {
    if(weatherReportRepo == null)
      throw new IllegalArgumentException();
    this.weatherReportRepo = weatherReportRepo;
  }

  /**
   * @param weatherForecastRepo the weatherForecastRepo to set
   */
  public void setWeatherForecastRepo (WeatherForecastRepo weatherForecastRepo)
  {
    if(weatherForecastRepo == null)
      throw new IllegalArgumentException();
    this.weatherForecastRepo = weatherForecastRepo;
  }

  /**
   * @param timeslotService the timeslotService to set
   */
  public void setTimeslotService (TimeslotRepo timeslotService)
  {
    
    this.timeslotService = timeslotService;
  }

  /**
   * @param timeService the timeService to set
   */
  public void setTimeService (TimeService timeService)
  {
    if(timeService == null)
      throw new IllegalArgumentException();
    this.timeService = timeService;
  }

  /**
   * @param tariffMarketService the tariffMarketService to set
   */
  public void setTariffMarketService (TariffMarket tariffMarketService)
  {
    if(tariffMarketService == null)
      throw new IllegalArgumentException();
    this.tariffMarketService = tariffMarketService;
  }

  /**
   * @param tariffSubscriptionRepo the tariffSubscriptionRepo to set
   */
  public void
    setTariffSubscriptionRepo (TariffSubscriptionRepo tariffSubscriptionRepo)
  {
    if(tariffSubscriptionRepo == null)
      throw new IllegalArgumentException();
    this.tariffSubscriptionRepo = tariffSubscriptionRepo;
  }

  /**
   * @param customerRepo the customerRepo to set
   */
  public void setCustomerRepo (CustomerRepo customerRepo)
  {
    if(customerRepo == null)
      throw new IllegalArgumentException();
    this.customerRepo = customerRepo;
  }

  /**
   * @param randomSeedRepo the randomSeedRepo to set
   */
  public void setRandomSeedRepo (RandomSeedRepo randomSeedRepo)
  {
    if(randomSeedRepo == null)
      throw new IllegalArgumentException();
    this.randomSeedRepo = randomSeedRepo;
  }

  /**
   * @param tariffEvaluator the tariffEvaluator to set
   */
  public void setTariffEvaluator (TariffEvaluator tariffEvaluator)
  {
    if(tariffEvaluator == null)
      throw new IllegalArgumentException();
    this.tariffEvaluator = tariffEvaluator;
  }

  /**
   * @param tariffEvaluationHelper the tariffEvaluationHelper to set
   */
  public void
    setTariffEvaluationHelper (TariffEvaluationHelper tariffEvaluationHelper)
  {
    if(tariffEvaluationHelper == null)
      throw new IllegalArgumentException();
    this.tariffEvaluationHelper = tariffEvaluationHelper;
  }

  /**
   * @param seed the seed to set
   */
  public void setSeed (RandomSeed seed)
  {
    if(seed == null)
      throw new IllegalArgumentException();
    this.seed = seed;
  }

  /**
   * @param currentSubscription the currentSubscription to set
   */
  public void setCurrentSubscription (TariffSubscription currentSubscription)
  {
    if(currentSubscription == null)
      throw new IllegalArgumentException();
    this.currentSubscription = currentSubscription;
  }

  /**
   * @param producerAccessor the producerAccessor to set
   */
  public void setProducerAccessor (ProducerAccessor producerAccessor)
  {
    if(producerAccessor == null)
      throw new IllegalArgumentException();
    this.producerAccessor = producerAccessor;
  }

  /**
   * @param preferredOutput the preferredOutput to set
   */
  public void setPreferredOutput (double preferredOutput)
  {
    if(preferredOutput > 0)
      throw new IllegalArgumentException();
    this.preferredOutput = preferredOutput;
  }

  /**
   * @param customerInfo the customerInfo to set
   */
  public void setCustomerInfo (CustomerInfo customerInfo)
  {
    if(customerInfo == null)
      throw new IllegalArgumentException();
    this.customerInfo = customerInfo;
  }
}
