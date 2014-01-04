/**
 * 
 */
package org.powertac.producer;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.List;
import java.util.TreeMap;

import org.powertac.common.AbstractCustomer;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.common.spring.SpringApplicationContext;

/**
 * @author Spyros Papageorgiou
 *
 */
public abstract class Producer extends AbstractCustomer{

	private static final double TOU_FACTOR = 0.05;
	private static final double TIERED_RATE_FACTOR = 0.1;
	private static final double VARIABLE_PRICING_FACTOR = 0.7;
	private static final double INTERRUPTIBILITY_FACTOR = 0.5;
	private static final double WEIGHT_INCONVENIENCE = 1;
	private static final double INNERTIA = 0.1;
	private static final double RATIONALITY_FACTOR = 0.9;
	private static final int TARIFF_COUNT = 5;
	private static final double BROKER_SWITCH_FACTOR = 0.02;
	

	ProducerAccessor producerAccessor;

	WeatherReportRepo weatherReportRepo;

	WeatherForecastRepo weatherForecastRepo;

	TimeslotRepo timeslotService;
	
	TariffEvaluator tariffEvaluator;
	

	public Producer(String name,PowerType powerType,int profileHours) {
		super(name);
		// TODO Set maximum and minimum output/capacity

		//Initialize the weather report and forecast repos because they are not included in the AbstractCustomer
		weatherReportRepo = (WeatherReportRepo) SpringApplicationContext.getBean("WeatherReportRepo");
		weatherForecastRepo = (WeatherForecastRepo) SpringApplicationContext.getBean("WeatherForecastRepo");
		timeslotService = (TimeslotRepo) SpringApplicationContext.getBean("TimeslotRepo");

		CustomerInfo customerInfo = new CustomerInfo(name, 1);
		customerInfo.withPowerType(powerType);
		addCustomerInfo(customerInfo);

		ProducerAccessor wrapper = new ProducerAccessor(this,profileHours);

		tariffEvaluator = new TariffEvaluator(wrapper);

		tariffEvaluator.initializeInconvenienceFactors(TOU_FACTOR,
				TIERED_RATE_FACTOR,
				VARIABLE_PRICING_FACTOR,
				INTERRUPTIBILITY_FACTOR);
		
		double weight = rs1.nextDouble() * WEIGHT_INCONVENIENCE;
		
		tariffEvaluator.withInconvenienceWeight(weight)
						.withInertia(INNERTIA)
						.withRationality(RATIONALITY_FACTOR)
						.withTariffEvalDepth(TARIFF_COUNT)
						.withTariffSwitchFactor(BROKER_SWITCH_FACTOR);
	}

	public void initialize(){


	}

	// TODO implenent if needed
	/*
	@Override
	public void subscribeDefault(){

	}
	 */

	@Override
	public void consumePower (){
		// TODO implement
		// We need to get the Weather report
	}

	abstract protected double getOutput(WeatherReport weatherReport);

	abstract protected double getOutput(int timeslotIndex, WeatherForecastPrediction weatherForecastPrediction);

	@Override
	public void step (){
		// TODO implement
		// We produce power here for the active tariff
	}

	public void evaluateNewTariffs(){
		// TODO impelement
	}
	
	
	/**
	 * The most important function of this class is to generated the hypothetical load under a specified tariff.
	 * These allows the TariffEvaluatorHelper to choose the best tariff of the ones that are available.  
	 * @author Spyros Papageorgiou
	 *
	 */
	public class ProducerAccessor implements CustomerModelAccessor
	{

		private Producer parent;
		int hours;

		public ProducerAccessor (Producer parent, int hours){
			if(hours <= 0 || parent == null)
				throw new IllegalArgumentException("Negative or zero duration for the Customer profile");
			this.parent = parent;
			this.hours = hours;
		}

		/**
		 * Generates the producer's output for N hours in the future 
		 * @return
		 */
		protected double[] generateOutput(Tariff tariff){
			//try and collect forecasts for the requested number of hours
			SortedMap<Integer,WeatherForecastPrediction> predictions = new TreeMap<>();
			List<WeatherForecast> forecasts = parent.weatherForecastRepo.allWeatherForecasts();
			boolean quit = false;

			if(forecasts.size() == 0)
				return new double[0];

			for(WeatherForecast forecast: forecasts){

				for(WeatherForecastPrediction prediction: forecast.getPredictions()){

					predictions.put(prediction.getForecastTime() + forecast.getTimeslotIndex(),
							prediction);

					if(predictions.size() == hours)
						quit = false;
				}

				if (quit)
					break;
			}


			double[] out = new double[predictions.size()];
			Iterator<Integer> slotIter = predictions.keySet().iterator();

			//Generate the output
			//TODO We could check the tariff prices and close the plant down if the production costs
			// are greater the the tariff payments.

			for(int i = 0; i < out.length; i++){
				int timeslot = slotIter.next();
				out[i] = getOutput(timeslot, predictions.get(timeslot));;
			}
			return out;
		}

		@Override
		public CustomerInfo getCustomerInfo ()
		{
			return parent.customerInfos.get(0);
		}

		@Override
		public double[] getCapacityProfile (Tariff tariff)
		{
			// TODO test
			return generateOutput(tariff);
		}

		@Override
		public double getBrokerSwitchFactor (boolean isSuperseding)
		{
			// TODO reimplement if needed
			double result = BROKER_SWITCH_FACTOR;
			if (isSuperseding)
				return result * 5.0;
			return result;
		}

		@Override
		public double getTariffChoiceSample ()
		{
			return parent.rs1.nextDouble();
		}

		@Override
		public double getInertiaSample ()
		{
			return parent.rs1.nextDouble();
		}

	}
}
