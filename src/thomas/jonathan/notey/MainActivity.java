package thomas.jonathan.notey;

import android.app.Activity;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{
	
	Integer[] imageIconDatabase = {R.drawable.check, R.drawable.warn, R.drawable.pencil, R.drawable.star};
	NotificationManager nm;
	ImageButton ib1, ib2, ib3, ib4, btn;
	public EditText et;
	String[] pos = {"0", "1", "2", "3"};
	Spinner spinner;
	String title = "Notey";
	int button=1, loc=0, id = (int)(Math.random()*10000);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		spinner = (Spinner) findViewById(R.id.spinner1);
		spinner.setAdapter(new MyAdapter(this, R.layout.spinner, pos));
		
		ib1 = (ImageButton) findViewById(R.id.imageButton1);
		ib2 = (ImageButton) findViewById(R.id.imageButton2);
		ib3 = (ImageButton) findViewById(R.id.imageButton3);
		ib4 = (ImageButton) findViewById(R.id.imageButton4);
		ib1.setBackgroundColor(Color.argb(150, 51, 181, 229));
		ib2.setBackgroundColor(Color.TRANSPARENT);
		ib3.setBackgroundColor(Color.TRANSPARENT);
		ib4.setBackgroundColor(Color.TRANSPARENT);
		
		btn = (ImageButton) findViewById(R.id.btn);
		
		et = (EditText) findViewById(R.id.editText1);
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(id);
		id++;
		
		//set the click listener
		ib1.setOnClickListener(this);
		ib2.setOnClickListener(this);
		ib3.setOnClickListener(this);
		ib4.setOnClickListener(this);
		btn.setOnClickListener(this);

		//change font
		Typeface font = Typeface.createFromAsset(getAssets(),"fonts/ROBOTO-LIGHT.TTF");
		et.setTypeface(font);
		
		//show keyboard at start
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		
		et.setText("");
		
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() 
		{

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) 
            {	
            	if(position==0)
            	{
                	loc=0;
                	ib1.setImageResource(R.drawable.check_white);
                	ib2.setImageResource(R.drawable.check_green);
                	ib3.setImageResource(R.drawable.check_yellow);
                	ib4.setImageResource(R.drawable.check_red);

            	}
            	
            	if(position==1)
            	{
            		loc=1;
            		ib1.setImageResource(R.drawable.warn_white);
            		ib2.setImageResource(R.drawable.warn_green);
            		ib3.setImageResource(R.drawable.warn_yellow);
            		ib4.setImageResource(R.drawable.warn_red);
            	}
            	
            	if(position==2)
            	{
            		loc=2;
            		ib1.setImageResource(R.drawable.pencil_white);
            		ib2.setImageResource(R.drawable.pencil_green);
            		ib3.setImageResource(R.drawable.pencil_yellow);
            		ib4.setImageResource(R.drawable.pencil_red);
            	}
            	
            	if(position==3)
            	{
            		loc=3;
            		ib1.setImageResource(R.drawable.star_white);
            		ib2.setImageResource(R.drawable.star_green);
            		ib3.setImageResource(R.drawable.star_yellow);
            		ib4.setImageResource(R.drawable.star_red);
            	}
              
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        }); 
		
		
	}
	 public class MyAdapter extends ArrayAdapter<String>
	 {
		 
	        public MyAdapter(Context context, int textViewResourceId,   String[] objects) 
	        {
	            super(context, textViewResourceId, objects);
	        }
	 
	        @Override
	        public View getDropDownView(int position, View convertView,ViewGroup parent) 
	        {
	            return getCustomView(position, convertView, parent);
	        }
	 
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) 
	        {
	            return getCustomView(position, convertView, parent);
	        }
	 
	        public View getCustomView(int position, View convertView, ViewGroup parent) 
	        {
	 
	            LayoutInflater inflater=getLayoutInflater();
	            View row=inflater.inflate(R.layout.spinner, parent, false);

	            ImageView icon=(ImageView)row.findViewById(R.id.imageView1);
	            icon.setImageResource(imageIconDatabase[position]);
	 
	            return row;
	       }
	       
	}
	 
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		if(v.getId()== R.id.imageButton1)
		{
			button=1;
			ib1.setBackgroundColor(Color.argb(150, 51, 181, 229));
			ib2.setBackgroundColor(Color.TRANSPARENT);
			ib3.setBackgroundColor(Color.TRANSPARENT);
			ib4.setBackgroundColor(Color.TRANSPARENT);

		}

		else if(v.getId()== R.id.imageButton2)
		{
			button=2;
			ib2.setBackgroundColor(Color.argb(150, 51, 181, 229));
			ib1.setBackgroundColor(Color.TRANSPARENT);
			ib3.setBackgroundColor(Color.TRANSPARENT);
			ib4.setBackgroundColor(Color.TRANSPARENT);
		}
		else if(v.getId()== R.id.imageButton3)
		{
			button=3;
			ib3.setBackgroundColor(Color.argb(150, 51, 181, 229));
			ib1.setBackgroundColor(Color.TRANSPARENT);
			ib2.setBackgroundColor(Color.TRANSPARENT);
			ib4.setBackgroundColor(Color.TRANSPARENT);
		}
		else if(v.getId()== R.id.imageButton4)
		{
			button=4;
			ib4.setBackgroundColor(Color.argb(150, 51, 181, 229));
			ib1.setBackgroundColor(Color.TRANSPARENT);
			ib2.setBackgroundColor(Color.TRANSPARENT);
			ib3.setBackgroundColor(Color.TRANSPARENT);
		}
		else if(v.getId()== R.id.btn)
		{
			btn.setBackgroundColor(Color.argb(150, 51, 181, 229));
			
			//if nothing is entered delay the blue background for a brief time
			if(et.getText().toString().equals(""))
			{
				
				btn.postDelayed(new Runnable() {
					@Override
					public void run() {
						btn.setBackgroundColor(Color.TRANSPARENT);
					}
				}, 100);

			}
			else
			{
				int d;
				
				if(loc==0)
				{
					if(button==2) d = R.drawable.check_green;
					else if(button==3) d = R.drawable.check_yellow;
					else if (button==4) d = R.drawable.check_red;
					else d = R.drawable.check_white;
				}
				
				else if(loc==1)
				{
					if(button==2) d = R.drawable.warn_green;
					else if(button==3) d = R.drawable.warn_yellow;
					else if (button==4) d = R.drawable.warn_red;
					else d = R.drawable.warn_white;
				}
				
				else if(loc==2)
				{
					if(button==2) d = R.drawable.pencil_green;
					else if(button==3) d = R.drawable.pencil_yellow;
					else if (button==4) d = R.drawable.pencil_red;
					else d = R.drawable.pencil_white;
				}
				
				else
				{
					if(button==2) d = R.drawable.star_green;
					else if(button==3) d = R.drawable.star_yellow;
					else if (button==4) d = R.drawable.star_red;
					else d = R.drawable.star_white;
				}
				
				
				
				Intent i = new Intent(this, CopyOfMainActivity.class);
				PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
				String note = et.getText().toString();
				Notification n = new Notification(d, note, System.currentTimeMillis());
				n.setLatestEventInfo(this, note, title, pi);
				nm.notify(id, n);
				finish();
				 				
			}
		}		
	}
	
//	public boolean onOptionsItemSelected(MenuItem item) {
//		if(item.getItemId() == R.id.){
//			Toast.makeText(getApplicationContext(), "options menu selected", Toast.LENGTH_SHORT).show();
//			return true;
//		}
//		
//		else return false;
//	}
	
//	public void onResume(){
//		int newId = getIntent().getIntExtra("TESTTEST", 0);
//	}
}


