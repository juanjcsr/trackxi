package codigo.labplc.mx.trackxi.panic;

import java.util.ArrayList;

import codigo.labplc.mx.trackxi.R;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.StrictMode;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;

public class PanicAlert {

	Context context;
	int levelBattery = 0;
	
	public PanicAlert(Context context) {
       this.context=context;
    }

    public void activate() {
    	Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    	v.vibrate(3000);
    	

    }
    
      
     public int getLevelBattery(){
    	 Intent i = new ContextWrapper(context).registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    	return i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
     }
     
     

     public void sendSMS(String phoneNumber, String message)
     {
    	 SmsManager smsManager = SmsManager.getDefault();
    	 smsManager.sendTextMessage(phoneNumber, null, message, null, null);

      }

     
     public void sendMail(String cabecera,String mensaje,String correoRemitente,String correoDestino ){
    	 try {   
    		 StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    		 StrictMode.setThreadPolicy(policy); 
             GMailSender sender = new GMailSender(correoRemitente,context.getResources().getString(R.string.pass));
             sender.sendMail(cabecera, mensaje,correoRemitente, correoDestino);  
         } catch (Exception e) {   
             Log.e("SendMail", e.getMessage(), e);   
         } 
     }
}
