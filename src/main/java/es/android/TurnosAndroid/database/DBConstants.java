package es.android.TurnosAndroid.database;

/**
 * User: Jesús
 * Date: 17/11/13
 */
public class DBConstants {
  public static final String   DATABASE_NAME    = "TurnosAndroid";
  public static final int      DATABASE_VERSION = 1;
  public static final String   EVENTS_TABLE     = "events";
  public static final String   ID               = "_id";
  public static final String   NAME             = "name";
  public static final String   DESCRIPTION      = "description";
  public static final String   START            = "start";
  public static final String   DURATION         = "duration";
  public static final String   LOCATION         = "location";
  public static final String   COLOR            = "color";
  public static final String[] EVENT_PROJECTION = new String[]{
      ID,
      NAME,
      DESCRIPTION,
      START,
      DURATION,
      LOCATION,
      COLOR
  };
}
