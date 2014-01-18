/*******************************************************************************
 * Copyright 2014 Spyros Papageorgiou
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.powertac.producer.utils;

import java.util.Iterator;
import java.util.LinkedHashMap;

import static java.lang.Math.signum;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;

/**
 * Represents a curve from the supplied points using spline interpolation.
 * At least 3 points must be provided. 
 * 
 * @author Doom
 * 
 */
@XStreamAlias("curve")
@XStreamConverter(CurveConverter.class)
public class Curve
{
  // If the spline should return negative values
  private boolean canBeNegative = true;

  // prolong last value
  private boolean prolongLastValue = true;

  // prolong firstValue
  private boolean prolongFirstValue = true;
  // custom last value
  private double customLastValue = 0.0;
  // custom first value
  private double customFirstValue = 0.0;

  // cached fist x
  private double firstX = -1;
  // cached last x
  private double lastX = -1;

  // The x-y axis of the data
  protected LinkedHashMap<Double, Double> xy =
    new LinkedHashMap<Double, Double>();

  // Need to rebuild the spline
  boolean needRebuild = true;

  private PolynomialSplineFunction spline;

  private SplineInterpolator interpolator = new SplineInterpolator();

  /**
   * Default constructor.
   */
  public Curve ()
  {
    //Nothing done here
  }

  /**
   * Initialize this curve with the supplied points.
   * Points should be given in an increasing x way.  
   * @param x
   * @param y
   */
  public Curve (double x[], double y[])
  {
    if (x.length != y.length) {
      throw new IllegalArgumentException();
    }

    firstX = x[0];
    lastX = x[x.length - 1];

    for (int i = 0; i < x.length; i++) {
      xy.put(x[i], y[i]);
    }
  }

  /**
   * Adds this point to the curve to be created
   * 
   * @param x
   * @param y
   */
  public void add (double x, double y)
  {
    if (xy.size() != 0 && x < lastX)
      throw new IllegalArgumentException("Input arguments in order");
    xy.put(x, y);
    if (firstX == -1) {
      firstX = x;
      lastX = x;
    }
    else {
      lastX = x;
    }
    needRebuild = true;
  }

  protected void createSpline ()
  {
    double x[] = new double[xy.size()];
    double y[] = new double[xy.size()];
    int i = 0;
    for (Double key: xy.keySet()) {
      x[i] = key;
      y[i] = xy.get(key);
      i++;
    }
    spline = interpolator.interpolate(x, y);
  }

  /**
   * Get the interpolated value of the curve at the point x of the horizontal
   * axis
   * 
   * @param x
   *          the point on the horizontal axis
   * @return the value on the vertical axis
   */
  public double value (double xVal)
  {
    if (needRebuild) {
      createSpline();
      needRebuild = false;
    }

    if (xVal < firstX) {
      if (prolongFirstValue)
        return xy.get(firstX);
      else
        return customFirstValue;
    }

    if (xVal > lastX) {
      if (prolongLastValue)
        return xy.get(lastX);
      else
        return customLastValue;
    }

    double value = spline.value(xVal);

    if (!canBeNegative && value < 0) {
      return 0;
    }
    return value;
  }

  /**
   * Check whether this curve is invertible
   * 
   * @return
   */
  public boolean isInvertible ()
  {
    if (xy.size() < 3)
      return true;

    Iterator<Double> it = xy.keySet().iterator();
    it.next();

    double nextX = it.next(), prevX = firstX;
    double nextY = xy.get(nextX), prevY = xy.get(prevX);

    double prevAngle = (nextY - prevY) / (nextX - prevX), nextAngle;

    prevX = nextX;
    prevY = nextY;

    while (it.hasNext()) {
      nextX = it.next();
      nextY = xy.get(nextX);

      nextAngle = (nextY - prevY) / (nextX - prevX);
      if (signum(nextAngle) - signum(prevAngle) != 0)
        return false;

      prevX = nextX;
      prevY = nextY;
      prevAngle = nextAngle;
    }
    return true;
  }

  /**
   * Return the invertible part of this curve
   * 
   * @return
   */
  public Curve getInvertiblePart ()
  {
    if (xy.size() < 3)
      return this;

    Iterator<Double> it = xy.keySet().iterator();
    it.next();

    double nextX = it.next(), prevX = firstX;
    double nextY = xy.get(nextX), prevY = xy.get(prevX);
    double prevAngle = (nextY - prevY) / (nextX - prevX), nextAngle;

    Curve newCurve = new Curve();
    newCurve.add(prevY, prevX);
    newCurve.add(nextY, nextX);

    prevX = nextX;
    prevY = nextY;

    while (it.hasNext()) {
      nextX = it.next();
      nextY = xy.get(nextX);

      nextAngle = (nextY - prevY) / (nextX - prevX);
      if (signum(nextAngle) - signum(prevAngle) != 0) {
        return newCurve;
      }
      else {
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
  public boolean getCanBeNegative ()
  {
    return canBeNegative;
  }

  /**
   * @param canBeNegative
   *          the canBeNegative to set
   */
  public void setCanBeNegative (boolean canBeNegative)
  {
    this.canBeNegative = canBeNegative;
  }

  /**
   * Prolong the last value of the curve
   */
  public void setProlongLastValue ()
  {
    this.prolongLastValue = true;
  }

  /**
   * Value beyond the last x will be set to lastValue
   * 
   * @param lastValue
   */
  public void setCustomLastValue (double lastValue)
  {
    this.prolongLastValue = false;
    this.customLastValue = lastValue;
  }

  /**
   * Prolong the last value of the curve
   */
  public void setProlongFirstValue ()
  {
    this.prolongFirstValue = true;
  }

  /**
   * Value beyond the last x will be set to lastValue
   * 
   * @param lastValue
   */
  public void setCustomFirstValue (double firstValue)
  {
    this.customFirstValue = firstValue;
    this.prolongFirstValue = false;
  }

  /**
   * @return the firstX
   */
  public double getFirstX ()
  {
    return firstX;
  }

  /**
   * @return the lastX
   */
  public double getLastX ()
  {
    return lastX;
  }

  /**
   * @return the prolongLastValue
   */
  public boolean getProlongLastValue ()
  {
    return prolongLastValue;
  }

  /**
   * @return the prolongFirstValue
   */
  public boolean getProlongFirstValue ()
  {
    return prolongFirstValue;
  }

  /**
   * @return the customLastValue
   */
  public double getCustomLastValue ()
  {
    return customLastValue;
  }

  /**
   * @return the customFirstValue
   */
  public double getCustomFirstValue ()
  {
    return customFirstValue;
  }
}
