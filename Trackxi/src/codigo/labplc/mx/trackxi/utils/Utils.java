package codigo.labplc.mx.trackxi.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import codigo.labplc.mx.trackxi.log.BeanDatosLog;
import android.util.Log;

public class Utils {

	public static boolean isNetworkConnectionOk(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		return networkInfo != null && networkInfo.isConnected();
	}
	
	/**
	 * metodo que hace la conexion al servidor con una url especifica
	 * @param url(String) ruta del web service
	 * @return (String) resultado del service
	 */
	public static String doHttpConnection(String url) {
		HttpClient Client = new DefaultHttpClient();
		try {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
			HttpGet httpget = new HttpGet(url);
			HttpResponse hhrpResponse = Client.execute(httpget);
			HttpEntity httpentiti = hhrpResponse.getEntity();
			//Log.d("RETURN HTTPCLIENT", EntityUtils.toString(httpentiti));
			return EntityUtils.toString(httpentiti);
		} catch (ParseException e) {
			BeanDatosLog.setDescripcion(Utils.getStackTrace(e));
			return null;
		} catch (IOException e) {
			BeanDatosLog.setDescripcion(Utils.getStackTrace(e));
			return null;
		}
	}
	/**
	 * Metodo privado que genera un codigo unico segun la hora y fecha del
	 * sistema
	 * 
	 * @return photoCode
	 * */
	@SuppressLint("SimpleDateFormat")
	public static String getCode() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String date = dateFormat.format(new Date());
		String photoCode = "pic_" + date;
		return photoCode;
	}
	
	public static JSONObject SendHttpPost(String URL, JSONObject jsonObjSend) {

		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost httpPostRequest = new HttpPost(URL);

			StringEntity se;
			se = new StringEntity(jsonObjSend.toString());

			// Set HTTP parameters
			httpPostRequest.setEntity(se);
			httpPostRequest.setHeader("Accept", "application/json");
			httpPostRequest.setHeader("Content-type", "application/json");
			httpPostRequest.setHeader("Accept-Encoding", "gzip"); // only set this parameter if you would like to use gzip compression

			long t = System.currentTimeMillis();
			HttpResponse response = (HttpResponse) httpclient.execute(httpPostRequest);
			

			// Get hold of the response entity (-> the data):
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				// Read the content stream
				InputStream instream = entity.getContent();
				Header contentEncoding = response.getFirstHeader("Content-Encoding");
				if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
					instream = new GZIPInputStream(instream);
				}

				// convert content stream to a String
				String resultString= convertStreamToString(instream);
				instream.close();
				resultString = resultString.substring(1,resultString.length()-1); // remove wrapping "[" and "]"

				// Transform the String into a JSONObject
				JSONObject jsonObjRecv = new JSONObject(resultString);
				// Raw DEBUG output of our received JSON object:
				

				return jsonObjRecv;
			} 

		}
		catch (Exception e)
		{
			// More about HTTP exception handling in another tutorial.
			// For now we just print the stack trace.
			BeanDatosLog.setDescripcion(Utils.getStackTrace(e));
		}
		return null;
	}


	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a StringBuilder
		 * and returned as String.
		 * 
		 * (c) public domain: http://senior.ceng.metu.edu.tr/2009/praeda/2009/01/11/a-simple-restful-client-at-android/
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			BeanDatosLog.setDescripcion(Utils.getStackTrace(e));
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				BeanDatosLog.setDescripcion(Utils.getStackTrace(e));
			}
		}
		return sb.toString();
	}
	
	
	public static String getStackTrace(final Throwable throwable) {
	    final StringWriter sw = new StringWriter();
	    final PrintWriter pw = new PrintWriter(sw, true);
	    throwable.printStackTrace(pw);
	    return sw.getBuffer().toString();
	}
		

	/*
	 * metodo que convierte una imagen a grises
	 * 
	 * @return bitmap
	 */
		public static Bitmap toGrayscale(Bitmap bmpOriginal)
		{        
			int width, height;
			height = bmpOriginal.getHeight();
			width = bmpOriginal.getWidth();    
			Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
			Canvas c = new Canvas(bmpGrayscale);
			Paint paint = new Paint();
			ColorMatrix cm = new ColorMatrix();
			cm.setSaturation(0);
			ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
			paint.setColorFilter(f);
			c.drawBitmap(bmpOriginal, 0, 0, paint);
			return bmpGrayscale;
		}


}
