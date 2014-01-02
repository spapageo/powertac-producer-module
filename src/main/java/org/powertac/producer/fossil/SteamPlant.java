/**
 * 
 */
package org.powertac.producer.fossil;

import java.util.Random;

import org.powertac.producer.utils.Curve;

import static java.lang.Math.*;
/**
 * @author Doom
 *
 */
public class SteamPlant {
	private double maxOutput;
	private double adjustmentSpeed;
	private double diviation;
	private double prefferedOutput;
	private double lastOutput;
	private Random rand;
	
	SteamPlant(double ratedOutput, double adjustmentSpeed, double diviation){
		this.maxOutput = ratedOutput;
		this.lastOutput = maxOutput;
		this.prefferedOutput = this.maxOutput;
		
		this.adjustmentSpeed = adjustmentSpeed;
		this.diviation = diviation;
		
		rand = new Random();
	}
	
	public double getOutput(){
		Curve out = new Curve();
		out.add(0, lastOutput);
		double time = (prefferedOutput - lastOutput)/(signum(prefferedOutput - lastOutput)*adjustmentSpeed);
		out.add(time, prefferedOutput);
		
		double outSum = 0.0;
		for(double t = 0.0; t < 60; t++){
			outSum += out.value(t) + rand.nextGaussian()*diviation;
		}

		lastOutput = out.value(60.0);
		
		return outSum/60;
	}

	/**
	 * @return the maxOutput
	 */
	public double getMaxOutput() {
		return maxOutput;
	}

	/**
	 * @param maxOutput the maxOutput to set
	 */
	public void setMaxOutput(double maxOutput) {
		this.maxOutput = maxOutput;
	}

	/**
	 * @return the adjustmentSpeed
	 */
	public double getAdjustmentSpeed() {
		return adjustmentSpeed;
	}

	/**
	 * @param adjustmentSpeed the adjustmentSpeed to set
	 */
	public void setAdjustmentSpeed(double adjustmentSpeed) {
		this.adjustmentSpeed = adjustmentSpeed;
	}

	/**
	 * @return the diviation
	 */
	public double getDiviation() {
		return diviation;
	}

	/**
	 * @param diviation the diviation to set
	 */
	public void setDiviation(double diviation) {
		this.diviation = diviation;
	}

	/**
	 * @return the prefferedOutput
	 */
	public double getPrefferedOutput() {
		return prefferedOutput;
	}

	/**
	 * @param prefferedOutput the prefferedOutput to set
	 */
	public void setPrefferedOutput(double prefferedOutput) {
		this.prefferedOutput = prefferedOutput;
	}
	
	
}
