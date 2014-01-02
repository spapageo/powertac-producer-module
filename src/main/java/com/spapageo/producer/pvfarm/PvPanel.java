/**
 * 
 */
package com.spapageo.producer.pvfarm;

import java.util.Calendar;
import java.util.TimeZone;


/**
 * @author Spyros Papageorgiou
 *
 */
public class PvPanel {
	private double panelArrea;
	private double panelLatitude;
	private double panelLongitude;
	private double panelAzimuth;
	private double panelTilt;
	private double panelEfficiency;
	private double groundAlbedo = 0.2;
	private double humidity = 0.2;
	private double ozoneLayerDepth = 3;
	private double pressure = 101325;
	private double Ta = 0.95;
	private double w0 = 0.95;
	private double p = 0.95;
	private double q = 0.33;
	private double r = -1.06;
	private double s = 0;
	private double m = 0;
	private double b = 0.0045;
	private double Tref = 313.15;
	private double staticLosses = 0.78;
	private double clearIndex = 0.2;		
	
	public PvPanel(double panelArrea, double panelLatitude,double panelLongitude,double panelAzimuth,double panelTilt, double referenceEfficiency){
		this.panelArrea = panelArrea;
		this.panelLatitude = panelLatitude;
		this.panelLongitude = panelLongitude;
		this.panelAzimuth = panelAzimuth;
		this.panelTilt = panelTilt;
		this.panelEfficiency = referenceEfficiency;
	}

	public double getOutput(long systemTime,int timezone,double cloudcover,double temperature,double windspeed){
		//assuming duration of one hour
		//do the calculations once for every min
		Calendar cal;
		if(timezone > 0)
			cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+"+timezone+":00"));
		else
			cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-"+timezone+":00"));

		cal.setTimeInMillis(systemTime);

		double sum = 0;

		for(int i = 0; i < 60; i++ ){
			//calculate solar time
			double solarTime = SolarModel.getSolarTime(panelLongitude,timezone, cal.getTimeInMillis());
			//calculate sun position
			double sunAltitude = SolarModel.getSunAltitudeAngle(solarTime, panelLatitude, cal.get(Calendar.DAY_OF_YEAR));
			sunAltitude = sunAltitude + SolarModel.getSunAltitudeCorrection(sunAltitude, pressure, temperature);
			double sunAzimuth = SolarModel.getSunAzinuthAngle(sunAltitude, cal.get(Calendar.DAY_OF_YEAR), panelLatitude, solarTime);

			if(sunAltitude > 0){
				//calculate irradiance
				double inci = SolarModel.getIncidenceAngle(sunAltitude, sunAzimuth, panelAzimuth , panelTilt);
				double airmass = IrradianceModel.getAirMass(sunAltitude);

				double T0 = IrradianceModel.getT0(airmass, ozoneLayerDepth);
				
				double Tr = IrradianceModel.getTr(airmass);
				
				double aw = IrradianceModel.getaw(airmass, humidity, temperature);
				
				double f = IrradianceModel.getf(sunAltitude);
				
				double solarConstanct = IrradianceModel.getIrradianceConstant(cal.get(Calendar.DAY_OF_YEAR));
				
				double dir = IrradianceModel.getDirectIrradiance(sunAltitude, solarConstanct, T0, Tr, aw, Ta);
				double dif = IrradianceModel.getDiffuseIrradiance(sunAltitude, solarConstanct, T0, Tr, aw, Ta, w0, f);
				
				dir = IrradianceModel.getCloudModifiedIrradiance(dir, cloudcover, groundAlbedo, p, q,  r, s, m);
				dif = IrradianceModel.getCloudModifiedIrradiance(dif, cloudcover, groundAlbedo, p, q,  r, s, m);

				double inputIrrad;
				if(inci <= 90){
					inputIrrad = IrradianceModel.getIrradiancOnTiltedPlane(dir, dif, inci, sunAltitude, panelTilt, groundAlbedo);
				}else{
					inputIrrad = IrradianceModel.getIrradiancOnTiltedPlane(0, dif, inci, sunAltitude, panelTilt, groundAlbedo);
				}
				
				double panelTemperature = ElectricalModel.getPanelTemperature(temperature,windspeed,inputIrrad);
				double thermaLosCoeff = ElectricalModel.getThermalLossCoeff(b,panelTemperature,Tref);
				double reflectiveLosCoeff = ElectricalModel.getReflectiveLossCoeff(inci, clearIndex);
				
				double output = ElectricalModel.getElectricalOutput(panelEfficiency, staticLosses, thermaLosCoeff, reflectiveLosCoeff, inputIrrad, panelArrea);
				
				sum = sum + output;
			}
			cal.add(Calendar.MINUTE, 1);
		}


		return sum/60;
	};
}
