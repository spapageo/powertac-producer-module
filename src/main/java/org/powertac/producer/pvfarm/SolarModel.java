/**
 * 
 */
package org.powertac.producer.pvfarm;

import static java.lang.Math.*;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author Doom
 *
 */
final class SolarModel {
	private SolarModel(){}

	/**
	 * Calculate the suns azimuth angle during the day
	 * 
	 * @param sunElevation the sun altitude angle in degrees
	 * @param day the day of the year between 1-366
	 * @param panelLatitude The panel latitude in degrees 
	 * @param solartime The local solar time in hours 0-23.99999
	 * @return The suns azimuth angle in degrees
	 */
	protected static double getSunAzinuthAngle(double sunElevation, int day, double panelLatitude, double solartime){
		//assert(sunElevation >= 0 && sunElevation <= 90);
		assert(day >= 1 && day <= 366);
		assert(panelLatitude >= -90 && panelLatitude <= 90);
		assert(solartime >= 0 && solartime < 24);


		double res = (sin(toRadians(getSunDivergance(day)))*cos(toRadians(panelLatitude))
				- cos(toRadians(getSunDivergance(day)))*sin(toRadians(panelLatitude))*cos(toRadians((12-solartime)*15)))
				/ cos(toRadians(sunElevation));
		
		if(res > 1){
			res = 1;
		}else if(res < -1){
			res = -1;
		}

		if(solartime <= 12)
			//			return 180 - toDegrees(acos(res));
			return toDegrees(acos(res));
		else
			//			return 180 + toDegrees(acos(res));
			return 360 - toDegrees(acos(res));
	}

	/**
	 * Calculates the suns divergance/declination at solar noon as a function the day of the year
	 * @param day the day of the year between 1-366
	 * @return the declination angle in degrees
	 */
	protected static double getSunDivergance(int day){
		assert(day >= 1 && day <= 366);
		return 0.3948 - 23.2559*cos(toRadians(day*360.0/365.0 + 9.1))
				- 0.3915*cos(toRadians(2*day*360.0/365.0 + 5.4))
				- 0.1764*cos(toRadians(3*day*360.0/365.0 + 26.0));
	}

	/**
	 * Calculates the a correction to the suns altitude angle due to refraction in the atmosphere
	 * @param altitudeAngle
	 * @param pressure Air pressure in Pa
	 * @param temperature Ambient temperature in K
	 * @return The difference from the given altitude angle
	 */
	protected static double getSunAltitudeCorrection(double altitudeAngle, double pressure, double temperature){
		assert(altitudeAngle >= 0 && altitudeAngle <= 90);
		assert(pressure >= 0);
		assert(temperature >= 0);


		//Since we take inputs in SI units and the equation requires pressure in mbar we have to convert the value
		// 1 bar = 100000 Pa
		double p = pressure / 100.0;

		if(altitudeAngle >= 15){
			return 0.00452 * p / ( temperature * tan(toRadians(altitudeAngle)));
		}else{
			return (0.1594 + 0.0196*altitudeAngle + 0.00002*pow(altitudeAngle, 2))*p 
					/ ( (1 + 0.505*altitudeAngle + 0.0845*pow(altitudeAngle,2)) * temperature );
		}
	}

	/**
	 * Calculate the sun angle/altitude angle
	 * 
	 * @param solartime The local solar time in hours 0-23.99999
	 * @param panelLatitude The panel latitude in degrees
	 * @param day the day of the year between 1-366
	 * 
	 * @return The suns altitude angle in degrees
	 */
	protected static double getSunAltitudeAngle(double solarTime, double panelLatitude,int day){
		assert(day >= 1 && day <= 366);
		assert(panelLatitude >= -90 && panelLatitude <= 90);
		assert(solarTime >= 0 && solarTime < 24);

		return toDegrees(asin(
				cos(toRadians((12-solarTime)*15))*cos(toRadians(panelLatitude))*cos(toRadians(getSunDivergance(day)))
				+ sin(toRadians(panelLatitude))*sin(toRadians(getSunDivergance(day)))
				));
	}

	/**
	 * Calculate the incidence angle of the sun rays on a panel
	 * @param sunAltitude Suns altitude angle in degrees 
	 * @param sunAzimuth Suns azimuth angle in degrees
	 * @param panelAzimuth Panels azimuth/facing angle in degrees
	 * @param panelAltitude Panels tilt/altitude angle in degrees
	 * @return The incidence angle in degrees
	 */
	protected static double getIncidenceAngle(double sunAltitude, double sunAzimuth, double panelAzimuth, double panelAltitude){
		//assert(sunAltitude >= 0 && sunAltitude <= 90);
		assert(sunAzimuth >= 0 && sunAzimuth <= 360);
		assert(panelAzimuth >= 0 && panelAzimuth <= 360);
		assert(panelAltitude >= 0 && panelAltitude <= 90);

		return toDegrees(acos(
				cos(toRadians(sunAltitude))*sin(toRadians(panelAltitude))*cos(toRadians(panelAzimuth-sunAzimuth))
				+ sin(toRadians(sunAltitude))*cos(toRadians(panelAltitude))
				));
	}

	/**
	 * Calculate the solar time
	 * @param longitude longitude of the location
	 * @param timeZone time zone offset from UTC
	 * @param localTime local time in milliseconds
	 * @return The local solar time in hours from zero to 23.99999999
	 */
	protected static double getSolarTime(double longitude, int timeZone,long localTime){
		assert(longitude >= -180 && longitude <= 180);
		assert(abs(timeZone) <= 10);
		assert(localTime > 0);


		String tz;
		if(timeZone >= 0){
			tz = new String("GMT+"+ String.valueOf(timeZone) + ":00");
		}else{
			tz = new String("GMT-"+ String.valueOf(timeZone) + ":00");
		}
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(tz));
		cal.setTimeInMillis(localTime);

		int day = cal.get(Calendar.DAY_OF_YEAR);
		double hours = cal.get(Calendar.HOUR_OF_DAY);
		double minutes = cal.get(Calendar.MINUTE);
		double seconds = cal.get(Calendar.SECOND);
		double daylightsavings = cal.get(Calendar.DST_OFFSET)/3600000;

		assert(timeZone >= -12 && timeZone <= 12);
		assert(longitude >= -180 && longitude <= 180);

		double hms = hours + minutes/60 + seconds/3600;
		double EOT = equationOfTime(day);

		double correctedLongitude = longitudeCorrection(timeZone, longitude);

		double result = solarTime(hms, daylightsavings, EOT, correctedLongitude);
		if (result < 0.0){
			return 24 + result;
		} else if(result > 24) {
			return result - 24.0;
		} else{
			return result;
		}
	}

	private static double equationOfTime(int day){
		double X = toRadians((360*(day-1))/365.242);
		return 0.258*cos(X)-7.416*sin(X)-3.648*cos(2*X)-9.228*sin(2*X);
	}

	private static double longitudeCorrection(double timezone, double longitude){
		return (15*timezone-longitude)/15;
	}

	private static double solarTime(double hms, double dayligthSaving,double EOT, double correctedLongitude){
		return hms + (EOT/60) - correctedLongitude - dayligthSaving;
	}
}
