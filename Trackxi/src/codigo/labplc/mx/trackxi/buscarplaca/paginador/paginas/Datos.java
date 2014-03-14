package codigo.labplc.mx.trackxi.buscarplaca.paginador.paginas;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import codigo.labplc.mx.trackxi.R;
import codigo.labplc.mx.trackxi.buscarplaca.paginador.paginas.termometro.ThermometerView;

public class Datos extends View {

	private TextView marca,submarca,modelo,descripcion;
	private LinearLayout container,container_usuario;
	private View view;
	private Activity context;
	
	public Datos(Activity context) {
		super(context);
		init(context);
	}
	public Datos(Activity context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	public Datos(Activity context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	

	public void init(Activity con){
		
		this.context=con;

		LayoutInflater inflater = (LayoutInflater)   getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		view = inflater.inflate(R.layout.activity_datos, null);
	
		
	marca = (TextView)view.findViewById(R.id.datos_tv_marca);
	submarca = (TextView)view.findViewById(R.id.datos_tv_submarca);
	modelo = (TextView)view.findViewById(R.id.datos_tv_modelo);
	descripcion =(TextView)view.findViewById(R.id.datos_tv_descripcion);
	container = (LinearLayout)view.findViewById(R.id.Thermometer_Container);
	container_usuario = (LinearLayout)view.findViewById(R.id.Thermometer_Container_usuarios);
	
	crearTermometro();
		
	}

	public void crearTermometro(){

			final ThermometerView thermometer = new ThermometerView(context);
			thermometer.setThermometerProgress(50);
	        container.addView(thermometer);
	        final ThermometerView thermometer_usuario = new ThermometerView(context);
	        thermometer_usuario.setThermometerProgress(80);
	        container_usuario.addView(thermometer_usuario);
	        
		
	}
	
	public View getView(){
		return view;
	}

}
