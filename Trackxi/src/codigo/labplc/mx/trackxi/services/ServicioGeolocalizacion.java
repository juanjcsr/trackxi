package codigo.labplc.mx.trackxi.services;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import codigo.labplc.mx.trackxi.R;
import codigo.labplc.mx.trackxi.buscarplaca.paginador.DatosAuto;
import codigo.labplc.mx.trackxi.califica.Califica_taxi;
import codigo.labplc.mx.trackxi.log.BeanDatosLog;
import codigo.labplc.mx.trackxi.panic.MyReceiver;
import codigo.labplc.mx.trackxi.panic.PanicAlert;
import codigo.labplc.mx.trackxi.tracking.Activity_null;
import codigo.labplc.mx.trackxi.tracking.map.Mapa_tracking;
import codigo.labplc.mx.trackxi.utils.Utils;

/**
 * 
 * @author mikesaurio
 * 
 */
public class ServicioGeolocalizacion extends Service implements Runnable {
	/**
	 * Declaraci�n de variables
	 */
	public static DatosAuto taxiActivity;
	private LocationManager mLocationManager;
	private MyLocationListener mLocationListener;
	public static double latitud_inicial = 19.0f;
	public static double longitud_inicial = -99.0f;
	public static double latitud =0;
	public static double longitud=0;
	public static String horaInicio;
	private Location currentLocation = null;
	private boolean isFirstLocation = true;
	private Thread thread;
	ArrayList<String> pointsLat = new ArrayList<String>();
	ArrayList<String> pointsLon = new ArrayList<String>();
	private boolean isFirstTime = true;
	private Timer timer,timerParanoico;
	public static boolean serviceIsIniciado = false;
	private BroadcastReceiver mReceiver;
	private ResultReceiver resultReceiver;
	private static int countStart = -1;
	private Handler handler_time = new Handler();
	private Handler handler_panic = new Handler();
	private String uuid;
	private String telemer;
	private String correoemer;
	private String placa;
	PanicAlert panic;
	public static boolean countTimer = true;
	public static  boolean panicoActivado = false;
	public boolean isSendMesagge= false;
    private String timeLocation = "0";
    private int intervaloLocation =5000;
    private int intervaloLocationParanoia =0;
    private boolean algoPaso=true;
    
    

	@Override
	public void onCreate() {
		super.onCreate();
		
		
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		   //get current date time with Date()
		   Date date = new Date();
		   System.out.println(dateFormat.format(date));
		   
		   
		mLocationListener = new MyLocationListener();
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		timer = new Timer();
		timerParanoico = new Timer();

		intervaloLocation = getPreferencia("prefSyncFrequency");
		intervaloLocationParanoia  = getPreferencia("prefSyncFrequencyParanoia");

		// para le panic
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		mReceiver = new MyReceiver();
		registerReceiver(mReceiver, filter);
	}
	
	
	

	@Override
	public void onStart(Intent intent, int startId) {
		//PAnic
		if(isFirstTime){
			obtenerSenalGPS();
			isFirstTime=false;
			serviceIsIniciado= true;
		}
			try{
					resultReceiver = intent.getParcelableExtra("receiver");
	
					// revisamos si la pantalla esta prendida o apagada y contamos el numero de click al boton de apagado
					boolean screenOn = intent.getBooleanExtra("screen_state", false);
					// si damos m�s de 4 click al boton de apagado se activa la alarma
					if (countStart >= 4) {
						Log.i("*********", "mas de 4");
						countStart = -1;
						countTimer = true;
						// activamos el mensaje de auxilio
						
						SharedPreferences prefs = getSharedPreferences("MisPreferenciasTrackxi",Context.MODE_PRIVATE);
				           uuid = prefs.getString("uuid", null);
				           telemer = prefs.getString("telemer", null);
				           correoemer = prefs.getString("correoemer", null);
				           placa = prefs.getString("placa", null);
				           
						panic = new PanicAlert(this.getApplicationContext());
						panic.activate();
					    String mensajeEmer= "Estoy en peligro!, eres mi contacto de emergencia, revisa tu correo porfavor";
					    panic.sendSMS(telemer,mensajeEmer);
						isSendMesagge=true;
	
					} else {
						countStart += 1;
						Log.i("*********", "else " + countStart + "");
						// incrementamos los click en 1
	
						// contamos 10 segundos si no reiniciamos los contadores
						if (countTimer) {
							countTimer = false;
							handler_time.postDelayed(runnable, 10000);// 10 segundos de espera
						}
					}
			}catch(Exception e){
				BeanDatosLog.setDescripcion(Utils.getStackTrace(e));
			}
				
		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		if (mLocationManager != null)
			if (mLocationListener != null)
				mLocationManager.removeUpdates(mLocationListener);

		//Toast.makeText(this, "Servicio detenido ", Toast.LENGTH_SHORT).show();
		super.onDestroy();
		CancelNotification(this, 0);
		timer.cancel();
		CancelNotification(this, 1);
		timerParanoico.cancel();
		
		serviceIsIniciado= false;
		// panic
		unregisterReceiver(mReceiver);
	}

	@Override
	public IBinder onBind(Intent intencion) {
		return null;
	}

/*	/**
	 * handler
	 */
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// mLocationManager.removeUpdates(mLocationListener);
			updateLocation(currentLocation);
		}
	};

	/**
	 * metodo para actualizar la localizaci�n
	 * 
	 * @param currentLocation
	 * @return void
	 */
	public void updateLocation(Location currentLocation) {
		if (currentLocation != null) {
			latitud = Double.parseDouble(currentLocation.getLatitude() + "");
			longitud = Double.parseDouble(currentLocation.getLongitude() + "");

			if (isFirstLocation) {
				latitud_inicial = latitud;
				longitud_inicial = longitud;
				isFirstLocation = false;
				showNotification();
				//si es que activo el nivel paranoico
				if(intervaloLocationParanoia!=10000){
					mensajeParanoico();
				}
			}

			pointsLat.add(latitud + "");
			pointsLon.add(longitud + "");
			
			Intent intent = new Intent("key");
			intent.putExtra("latitud", pointsLat);
			intent.putExtra("longitud", pointsLon);
			getApplicationContext().sendBroadcast(intent);

			if(isSendMesagge){

				enviaCorreo();
				isSendMesagge=false;
			}
		}
	}

	public boolean yaPasaronMinutos(){
		
		
		return false;
	}
	/**
	 * Hilo de la aplicacion para cargar las cordenadas del usuario
	 */
	public void run() {
		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Looper.prepare();
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, intervaloLocation, 1, mLocationListener);
			Looper.loop();
			Looper.myLooper().quit();
		} else {
			taxiActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(),
							"GPS apagado inesperadamente", Toast.LENGTH_LONG)
							.show();
				}
			});
		}
	}

	/**
	 * Metodo para Obtener la se�al del GPS
	 */
	private void obtenerSenalGPS() {
		thread = new Thread(this);
		thread.start();
	}

	/**
	 * Metodo para asignar las cordenadas del usuario
	 * */
	private void setCurrentLocation(Location loc) {
		currentLocation = loc;
	}

	/**
	 * Metodo para obtener las cordenadas del GPS
	 */
	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location loc) {
			// Log.d("finura",loc.getAccuracy()+"");
			if (loc != null) {
				setCurrentLocation(loc);
				handler.sendEmptyMessage(0);
			}
		}

		/**
		 * metodo que revisa si el GPS esta apagado
		 */
		public void onProviderDisabled(String provider) {
			taxiActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(),	"GPS apagado inesperadamente", Toast.LENGTH_LONG).show();
				}
			});
		}

		// @Override
		public void onProviderEnabled(String provider) {
		}

		// @Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

	public void showNotification() {
		// notification is selected
		Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
    	v.vibrate(3000);

		Intent intent_mapa = new Intent(this, Mapa_tracking.class);
		intent_mapa.putExtra("latitud_inicial", latitud_inicial);
		intent_mapa.putExtra("longitud_inicial", longitud_inicial);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent_mapa,
				PendingIntent.FLAG_UPDATE_CURRENT);

		Intent intent_califica = new Intent(this, Califica_taxi.class);
		PendingIntent pIntent_cal = PendingIntent.getActivity(this, 0,
				intent_califica, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification noti = new Notification.Builder(this)
				.setContentTitle("Traxi").setContentText("�Qu� quieres hacer?")
				.setSmallIcon(R.drawable.ic_launcher)
		
				// .setContentIntent(pIntent)
				.addAction(R.drawable.ic_launcher_chinche, "viaje", pIntent)
				.addAction(R.drawable.ic_launcher_fin_viaje, "Finalizar",
						pIntent_cal).build();
		
		
		noti.flags += Notification.FLAG_ONGOING_EVENT;
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// noti.flags |= Notification.FLAG_AUTO_CANCEL;
		
	
		notificationManager.notify(0, noti);
	}

	public static void CancelNotification(Context ctx, int notifyId) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager nMgr = (NotificationManager) ctx
				.getSystemService(ns);
		nMgr.cancel(notifyId);
	}

	// panic
	 /**
     * hilo que al pasar el tiempo reeinicia los valores
     */
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //reiniciamos los contadores
            countStart = -1;
            countTimer = true;
        }
    };

    
    public void enviaCorreo(){
    	
    	timer.scheduleAtFixedRate(new TimerTask() {

    	    @Override
    	    public void run() {
    	    	panic.sendMail("TRAXI",
						"Estoy en peligro en taxi placas: "+placa+ " mi ubicacion es https://www.google.com.mx/maps/place/"+latitud+","+longitud+" bater�a: "+ panic.getLevelBattery()+"%",
						"mikesaurio@gmail.com", 
						correoemer);
    	    }
    	},
    	0,
    	intervaloLocationParanoia);

    }
    
    
   public void mensajeParanoico(){
    	timerParanoico.scheduleAtFixedRate(new TimerTask() {
    	    @Override
    	    public void run() {
    	    	showNotificationPanic();
    	    	
    	    }
    	},
    	intervaloLocationParanoia,
    	intervaloLocationParanoia);

    }
    
   public void showNotificationPanic() {
		// notification is selected
		Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(3000);
		
		
		  // revise si la alarma esta en true y si se cuentan 2 veces se mande el mensaje de panico
		if(getPanicoActivado()){
			//Solo activa la alarma una vez
			if(algoPaso){
					v.vibrate(3000);
					SharedPreferences prefs = getSharedPreferences("MisPreferenciasTrackxi",Context.MODE_PRIVATE);
		           uuid = prefs.getString("uuid", null);
		           telemer = prefs.getString("telemer", null);
		           correoemer = prefs.getString("correoemer", null);
		           placa = prefs.getString("placa", null); 
		           panic = new PanicAlert(this.getApplicationContext());
		           panic.activate();
		           String mensajeEmer= "Estoy en peligro!, eres mi contacto de emergencia, revisa tu correo porfavor";
		           panic.sendSMS(telemer,mensajeEmer);
		           isSendMesagge=true;
		           algoPaso=false;
			}
		}else{
			 isSendMesagge=false;
		}
		
		ServicioGeolocalizacion.setPanicoActivado(true);
		 
		String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        
		 int icon = R.drawable.ic_launcher;
         CharSequence tickerText = "Traxi";
         long when = System.currentTimeMillis();
         int requestID = (int) System.currentTimeMillis();
         Notification notification = new Notification(icon, tickerText, when);
         Context context = getApplicationContext();
         Intent notificationIntent = new Intent(this, Activity_null.class);

         PendingIntent contentIntent = PendingIntent.getActivity(this, requestID, notificationIntent, 0);
         notification.setLatestEventInfo(context, "�Todo bien?", "Tocame si es as�", contentIntent);
                 notification.flags += Notification.FLAG_ONGOING_EVENT;
                 notification.flags += Notification.FLAG_AUTO_CANCEL;
                 
                 AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                 alarmManager.set(AlarmManager.RTC_WAKEUP,0, contentIntent);
                 
             mNotificationManager.notify(1, notification);
             
           
            
	}
  

    /**
     * obtiene el valor de frecuencia de las preferencia 
     * @param preferencia
     * @return (int)intervalo de tiempo
     */
    public int getPreferencia(String preferencia){
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		timeLocation = sharedPrefs.getString(preferencia, "0");
		if(timeLocation.equals("0")){
			return 10000;
		}else if(timeLocation.equals("1")){
			return 60000;
		}else if(timeLocation.equals("2")){
			return 120000;
		}else if(timeLocation.equals("5")){
			return 300000;
		}else{
			return 10000;
		}
    } 
    
    public static synchronized void setPanicoActivado(boolean flag)
	{
    	ServicioGeolocalizacion.panicoActivado=flag;
	}
    
    public static synchronized boolean getPanicoActivado()
   	{
       return	ServicioGeolocalizacion.panicoActivado;
   	}
}