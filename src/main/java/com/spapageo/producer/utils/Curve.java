/**
 * 
 */
package com.spapageo.producer.utils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.Math.signum;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * @author Doom
 *
 */
public class Curve {
	//If the spline should return negative values
	private boolean canBeNegative = true;
	
	//prolong last value
	private boolean prolongLastValue = true;
	
	//prolong firstValue
	private boolean prolongFirstValue = true;
	//custom last value
	private double customLastValue = 0.0;
	//custom first value
	private double customFirstValue = 0.0;
	
	//cached fist x
	private double firstX = -1;
	//cached last x
	private double lastX = -1;
	
	// The x-y axis of the data
	private Map<Double, Double> xy = new LinkedHashMap<>();
		
	// Need to rebuild the spline
	boolean needRebuild = true;
	
	private PolynomialSplineFunction spline;
	
	private SplineInterpolator interpolator = new SplineInterpolator();
	
	public Curve() {}
	
	public Curve(double x[], double y[]){
		if (x.length != y.length){
			throw new IllegalArgumentException();
		}

		firstX = x[0];
		lastX = x[x.length-1];
		
		for(int i = 0; i< x.length;i++){
			xy.put(x[i], y[i]);
		}
	}
	
	/**
	 * Adds this point to the curve to be created
	 * @param x
	 * @param y
	 */
	public void add(double x, double y){
		xy.put(x, y);
		if(firstX == -1){
			firstX = x;
			lastX = x;
		}else{
			lastX = x;
		}
		needRebuild = true;
	}
	
	protected void createSpline(){
		double x[] = new double[xy.size()];
		double y[] = new double[xy.size()];
		int i = 0;
		for(Double key:xy.keySet()){
			x[i] = key;
			y[i] = xy.get(key);
			i++;
		}
		spline = interpolator.interpolate(x,y);
	}
	
	/**
	 * Get the interpolated value of the curve at the point x of the horizontal axis
	 * @param x the point on the horizontal axis
	 * @return the value on the vertical axis
	 */
	public double value(double xVal){
		if(needRebuild){
			createSpline();
			needRebuild = false;
		}
		
		if(xVal < firstX){
			if(prolongFirstValue)
				return xy.get(firstX);
			else
				return customFirstValue;
		}
		
		if(xVal > lastX){
			if(prolongLastValue)
				return xy.get(lastX);
			else
				return customLastValue;
		}
		
		double value = spline.value(xVal);
		
		if(!canBeNegative && value < 0){
			return 0;
		}
		return value;
	}
	
	/**
	 * Check whether this curve is invertible
	 * 
	 * @return
	 */
	public boolean isInvertible(){
		if(xy.size() < 3)
			return true;
		
		Iterator<Double> it = xy.keySet().iterator();
		it.next();
		
		double nextX = it.next(),prevX = firstX;
		double nextY = xy.get(nextX),prevY = xy.get(prevX);
		
		double prevAngle = (nextY-prevY)/(nextX - prevX),nextAngle;
		
		prevX = nextX; prevY = nextY;
		
		while(it.hasNext()){
			nextX = it.next();
			nextY = xy.get(nextX);
			
			nextAngle = (nextY-prevY)/(nextX - prevX);
			if(signum(nextAngle) - signum(prevAngle) != 0)
				return false;
			
			prevX = nextX;
			prevY = nextY;
			prevAngle = nextAngle;
		}
		return true;
	}
	
	/**
	 * Check whether this curve is invertible
	 * 
	 * @return
	 */
	public Curve getInvertiblePart(){
		if(xy.size() < 3)
			return this;
		
		Iterator<Double> it = xy.keySet().iterator();
		it.next();
		
		
		
		double nextX = it.next(),prevX = firstX;
		double nextY = xy.get(nextX),prevY = xy.get(prevX);
		double prevAngle = (nextY-prevY)/(nextX - prevX),nextAngle;
		
		Curve newCurve = new Curve();
		newCurve.add(prevY, prevX);
		newCurve.add(nextY, nextX);
		
		prevX = nextX; prevY = nextY;
		
		
		
		while(it.hasNext()){
			nextX = it.next();
			nextY = xy.get(nextX);
			
			nextAngle = (nextY-prevY)/(nextX - prevX);
			if(signum(nextAngle) - signum(prevAngle) != 0){
				return newCurve;
			} else {
				newCurve.add(nextY, nextX);
			}
				
			prevX = nextX;
			prevY = nextY;
			prevAngle = nextAngle;
		}
		return newCurve;
	}
	
	/**
	 * @return the canBeNegative
	 */
	public boolean getCanBeNegative() {
		return canBeNegative;
	}

	/**
	 * @param canBeNegative the canBeNegative to set
	 */
	public void setCanBeNegative(boolean canBeNegative) {
		this.canBeNegative = canBeNegative;
	}
	
	/**
	 * Prolong the last value of the curve
	 */
	public void prolongLastValue(){
		this.prolongLastValue = true;
	}
	
	/**
	 * Value beyond the last x will be set to lastValue
	 * @param lastValue
	 */
	public void setCustomLastValue(double lastValue){
		this.customLastValue = lastValue;
	}
	
	/**
	 * Prolong the last value of the curve
	 */
	public void prolongFirstValue(){
		this.prolongFirstValue = true;
	}
	
	/**
	 * Value beyond the last x will be set to lastValue
	 * @param lastValue
	 */
	public void setCustomFirstValue(double firstValue){
		this.customFirstValue = firstValue;
	}
}

