/**
 * 
 */
package org.powertac.producer;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
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

    if (capacity >= 0)
      throw new IllegalArgumentException("Positive plant capacity");

    initialize(name, powerType, profileHours, capacity,IdGenerator.createId());
  }

  protected void initialize(String name, PowerType powerType, int profileHours,
                          double capacity, long custId){
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
    
    //Initialize the customer info with population 1
    customerInfo = new CustomerInfo(name, 1);
    customerInfo.withPowerType(powerType);
    customerRepo.add(customerInfo);

    //Initialize the custom object id
    this.custId = custId;
    this.name = name + custId;
    //Initialize the random seed
    seed = randomSeedRepo.getRandomSeed(name, (int) custId, "Misc");
    
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
    double power = getOutput(report);

    List<TariffSubscription> subscriptions =
      tariffSubscriptionRepo
              .findActiveSubscriptionsForCustomer(customerInfo);

    if (subscriptions.size() > 0)
      subscriptions.get(0).usePower(power);
    else
      log.error("Got empty subscriptions list. We dont have an avtive tariff");
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
  private class ProducerAccessor implements CustomerModelAccessor
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
    protected PreferredOutput generateOutput (Tariff tariff, int hours)
    {
      // try and collect forecasts for the requested number of hours
      SortedMap<Integer, WeatherForecastPrediction> predictions =
        new TreeMap<>();
      // CARE don't know exactly what is returned here
      // the past forecast are return which aren't very useful because we
      // use past weather to decide for the
      // future output.
      List<WeatherForecast> forecasts =
        parent.weatherForecastRepo.allWeatherForecasts();
      // add the current forecast in the front
      forecasts.add(0, weatherForecastRepo.currentWeatherForecast());
      boolean quit = false;

      if (forecasts.size() == 0) {
        log.error("Got zero weather forecasts on the creation of the customer profile");
        return new PreferredOutput(preferredOutput, new double[0]);
      }

      for (WeatherForecast forecast: forecasts) {

        for (WeatherForecastPrediction prediction: forecast.getPredictions()) {

          predictions.put(prediction.getForecastTime()
                                  + forecast.getTimeslotIndex(), prediction);
          if (predictions.size() == hours)
            quit = false;
        }

        if (quit)
          break;
      }

      Iterator<Integer> slotIter = predictions.keySet().iterator();

      // Generate the output
      // We could check the tariff prices and close the plant down if
      // the production costs
      // are greater the the tariff payments. Since production is not an
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
      double savePreferredOutput = preferredOutput;

      // CARE Careful on the signs
      for (preferredOutput = 0; preferredOutput >= upperPowerCap; preferredOutput -=
        step * upperPowerCap) {
        double[] out = new double[predictions.size()];
        for (int i = 0; i < out.length; i++) {
          int timeslot = slotIter.next();
          out[i] = getOutput(timeslot, predictions.get(timeslot));
        }
        // calculate the money
        double money = tariffEvaluationHelper.estimateCost(tariff, out, true);
        if (money > maxPayment) {
          maxPayment = money;
          maxOuput = out;
          maxPreferredOutput = preferredOutput;
        }
        // reset the iterator
        slotIter = predictions.keySet().iterator();
      }

      // restore the preferred output
      preferredOutput = savePreferredOutput;

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

  private class PreferredOutput
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
}
