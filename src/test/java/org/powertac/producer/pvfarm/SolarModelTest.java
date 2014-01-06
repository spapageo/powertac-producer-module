package org.powertac.producer.pvfarm;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;
import org.powertac.producer.pvfarm.SolarModel;

public class SolarModelTest
{

  @Test
  public void testGetIncidenceAngle ()
  {
    double result = SolarModel.getIncidenceAngle(30, 180, 180, 60);
    assertEquals(0.0, result, 0.5);
    result = SolarModel.getIncidenceAngle(30, 180, 180, 0);
    assertEquals(60.0, result, 0.5);
    result = SolarModel.getIncidenceAngle(30, 180, 30, 0);
    assertEquals(60.0, result, 0.5);
  }

  @Test
  public void dataGetSolarPosition () throws FileNotFoundException
  {
    double latitude = 45;

    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+2:00"));
    cal.set(Calendar.YEAR, 2013);
    cal.set(Calendar.MONTH, 6);
    cal.set(Calendar.DAY_OF_MONTH, 11);
    cal.set(Calendar.HOUR_OF_DAY, 1);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    PrintWriter pw = new PrintWriter(new File("dataGetSolarPosition.txt"));

    for (int i = 0; i < 24; i++) {
      double altitude =
        SolarModel.getSunAltitudeAngle(i, latitude,
                                       cal.get(Calendar.DAY_OF_YEAR));
      double azimuth =
        SolarModel.getSunAzinuthAngle(altitude, cal.get(Calendar.DAY_OF_YEAR),
                                      latitude, i);
      double incid = SolarModel.getIncidenceAngle(altitude, azimuth, 180, 45);
      if (altitude >= 0)
        pw.printf("%d,%f,%f,%f%n", i, altitude, azimuth, incid);

      if (i == 5) {
        assertEquals(6.2, altitude, 2);
        assertEquals(63, azimuth, 3);
      }
      else if (i == 9) {
        assertEquals(48, altitude, 2);
        assertEquals(105, azimuth, 3);
      }
      else if (i == 12) {
        assertEquals(68, altitude, 2);
        assertEquals(180, azimuth, 3);
      }

    }
    pw.flush();
    pw.close();
  }

  @Test
  public void dataGetSunDivergance () throws FileNotFoundException
  {
    PrintWriter pw = new PrintWriter(new File("dataGetSunDivergance.txt"));
    for (int i = 1; i < 366; i++) {
      double data = SolarModel.getSunDivergance(i);
      pw.println(i + "," + data);
      assertTrue(data >= -25.0 && data <= 25.0);
    }
    pw.close();
  }

  @Test
  public void dataGetSolarTime () throws IOException
  {
    double longitude = 22;
    int timezone = 2;

    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+2:00"));
    cal.set(Calendar.YEAR, 2013);
    cal.set(Calendar.DAY_OF_YEAR, 337);
    cal.set(Calendar.HOUR_OF_DAY, 6);
    cal.set(Calendar.MINUTE, 30);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    assertEquals(6.127,
                 SolarModel.getSolarTime(longitude, timezone,
                                         cal.getTimeInMillis()), 0.009);
  }
}
