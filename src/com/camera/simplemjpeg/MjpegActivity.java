package com.camera.simplemjpeg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MjpegActivity extends Activity implements OnSeekBarChangeListener, SensorEventListener {
	private static final boolean DEBUG = false;
	private static final String TAG = "MJPEG";
	ProgressDialog mProgressDialog;
	Bitmap img = null;

	private MjpegView mv = null;
	String URL, imgURL;

	// for settings (network and resolution)
	private static final int REQUEST_SETTINGS = 0;

	private int width = 640;
	private int height = 480;

	private int ip_ad1 = 192;
	private int ip_ad2 = 168;
	private int ip_ad3 = 2;
	private int ip_ad4 = 1;
	private int ip_port = 80;
	private String ip_command = "?action=stream";

	private boolean suspending = false;

	final Handler handler = new Handler();
	SeekBar seekbar;
	TextView value, value2, x_t, y_t, z_t, fps;
	VerticalSeekBar vseek;
	SensorManager senSensorManager;
	Sensor senAccelerometer;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		fps = (TextView) findViewById(R.id.fps);
		x_t = (TextView) findViewById(R.id.x); 
		y_t = (TextView) findViewById(R.id.y); 
		z_t = (TextView) findViewById(R.id.z); 
		value = (TextView) findViewById(R.id.textview);
		value2 = (TextView) findViewById(R.id.textview2);
		seekbar = (SeekBar) findViewById(R.id.seekbar);
		vseek = (VerticalSeekBar) findViewById(R.id.calculatorVerticalSeekBar);
		
		senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);

		SharedPreferences preferences = getSharedPreferences("SAVED_VALUES",
				MODE_PRIVATE);
		width = preferences.getInt("width", width);
		height = preferences.getInt("height", height);
		ip_ad1 = preferences.getInt("ip_ad1", ip_ad1);
		ip_ad2 = preferences.getInt("ip_ad2", ip_ad2);
		ip_ad3 = preferences.getInt("ip_ad3", ip_ad3);
		ip_ad4 = preferences.getInt("ip_ad4", ip_ad4);
		ip_port = preferences.getInt("ip_port", ip_port);
		ip_command = preferences.getString("ip_command", ip_command);

		StringBuilder sb = new StringBuilder();
		StringBuilder sa = new StringBuilder();
		String s_http = "http://";
		String s_dot = ".";
		String s_colon = ":";
		String s_slash = "/";
		sb.append(s_http);
		sb.append(ip_ad1);
		sb.append(s_dot);
		sb.append(ip_ad2);
		sb.append(s_dot);
		sb.append(ip_ad3);
		sb.append(s_dot);
		sb.append(ip_ad4);
		sb.append(s_colon);
		sb.append(ip_port);
		sb.append(s_slash);
		sb.append(ip_command);
		URL = new String(sb);

		sa.append(s_http);
		sa.append(ip_ad1);
		sa.append(s_dot);
		sa.append(ip_ad2);
		sa.append(s_dot);
		sa.append(ip_ad3);
		sa.append(s_dot);
		sa.append(ip_ad4);
		sa.append(s_colon);
		sa.append(ip_port);
		sa.append(s_slash);
		sa.append("image.jpg");
		imgURL = new String(sa);

		mv = (MjpegView) findViewById(R.id.mv);
		if (mv != null) {
			mv.setResolution(width, height);
		}

		setTitle(R.string.title_connecting);
		new DoRead().execute(URL);

		seekbar.setOnSeekBarChangeListener(this);
		vseek.setOnSeekBarChangeListener(this);

		
	}

	public void onResume() {
		if (DEBUG)
			Log.d(TAG, "onResume()");
		super.onResume();
		if (mv != null) {
			if (suspending) {
				new DoRead().execute(URL);
				suspending = false;
			}
		}
		senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
	}

	public void onStart() {
		if (DEBUG)
			Log.d(TAG, "onStart()");
		super.onStart();
	}

	public void onPause() {
		if (DEBUG)
			Log.d(TAG, "onPause()");
		super.onPause();
		if (mv != null) {
			if (mv.isStreaming()) {
				mv.stopPlayback();
				suspending = true;
			}
		}
		senSensorManager.unregisterListener(this);
	}

	public void onStop() {
		if (DEBUG)
			Log.d(TAG, "onStop()");
		super.onStop();
	}

	public void onDestroy() {
		if (DEBUG)
			Log.d(TAG, "onDestroy()");

		if (mv != null) {
			mv.freeCameraMemory();
		}

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			Intent settings_intent = new Intent(MjpegActivity.this,
					SettingsActivity.class);
			settings_intent.putExtra("width", width);
			settings_intent.putExtra("height", height);
			settings_intent.putExtra("ip_ad1", ip_ad1);
			settings_intent.putExtra("ip_ad2", ip_ad2);
			settings_intent.putExtra("ip_ad3", ip_ad3);
			settings_intent.putExtra("ip_ad4", ip_ad4);
			settings_intent.putExtra("ip_port", ip_port);
			settings_intent.putExtra("ip_command", ip_command);
			startActivityForResult(settings_intent, REQUEST_SETTINGS);
			return true;
		case R.id.snapshot:
			// Bitmap bitmap = MjpegView.getBitmap();
			new DownloadImage().execute(imgURL);
			return true;
		}
		return false;
	}

	// DownloadImage AsyncTask
	private class DownloadImage extends AsyncTask<String, Void, Bitmap> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// Create a progressdialog
			mProgressDialog = new ProgressDialog(MjpegActivity.this);
			// Set progressdialog title
			mProgressDialog.setTitle("Getting Image");
			// Set progressdialog message
			mProgressDialog.setMessage("Loading...");
			mProgressDialog.setIndeterminate(false);
			// Show progressdialog
			mProgressDialog.show();
		}

		@Override
		protected Bitmap doInBackground(String... URL) {

			String imageURL = URL[0];

			Bitmap bitmap = null;
			try {
				// Download Image from URL
				InputStream input = new java.net.URL(imageURL).openStream();
				// Decode Bitmap
				bitmap = BitmapFactory.decodeStream(input);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			// Set the bitmap into ImageView
			String msg = saveBitmap(result);
			Toast toast = Toast.makeText(MjpegActivity.this, msg,
					Toast.LENGTH_LONG);
			toast.show();
			// Close progressdialog
			mProgressDialog.dismiss();
		}
	}

	public String saveBitmap(Bitmap bitmap) {
		File imagePath = new File(Environment.getExternalStorageDirectory()
				+ "/screenshot.png");
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(imagePath);
			bitmap.compress(CompressFormat.JPEG, 100, fos);
			fos.flush();
			fos.close();
			return imagePath.toString();
		} catch (FileNotFoundException e) {
			Log.e("GREC", e.getMessage(), e);
			return "Failed to take screenshot" + e;
		} catch (IOException e) {
			Log.e("GREC", e.getMessage(), e);
			return "Failed to take screenshot" + e;
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_SETTINGS:
			if (resultCode == Activity.RESULT_OK) {
				width = data.getIntExtra("width", width);
				height = data.getIntExtra("height", height);
				ip_ad1 = data.getIntExtra("ip_ad1", ip_ad1);
				ip_ad2 = data.getIntExtra("ip_ad2", ip_ad2);
				ip_ad3 = data.getIntExtra("ip_ad3", ip_ad3);
				ip_ad4 = data.getIntExtra("ip_ad4", ip_ad4);
				ip_port = data.getIntExtra("ip_port", ip_port);
				ip_command = data.getStringExtra("ip_command");

				if (mv != null) {
					mv.setResolution(width, height);
				}
				SharedPreferences preferences = getSharedPreferences(
						"SAVED_VALUES", MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putInt("width", width);
				editor.putInt("height", height);
				editor.putInt("ip_ad1", ip_ad1);
				editor.putInt("ip_ad2", ip_ad2);
				editor.putInt("ip_ad3", ip_ad3);
				editor.putInt("ip_ad4", ip_ad4);
				editor.putInt("ip_port", ip_port);
				editor.putString("ip_command", ip_command);

				editor.commit();

				new RestartApp().execute();
			}
			break;
		}
	}

	public void setImageError() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				setTitle(R.string.title_imageerror);
				return;
			}
		});
	}

	public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
		protected MjpegInputStream doInBackground(String... url) {
			// TODO: if camera has authentication deal with it and don't just
			// not work
			HttpResponse res = null;
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpParams httpParams = httpclient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 5 * 1000);
			HttpConnectionParams.setSoTimeout(httpParams, 5 * 1000);
			if (DEBUG)
				Log.d(TAG, "1. Sending http request");
			try {
				res = httpclient.execute(new HttpGet(URI.create(url[0])));
				if (DEBUG)
					Log.d(TAG, "2. Request finished, status = "
							+ res.getStatusLine().getStatusCode());
				if (res.getStatusLine().getStatusCode() == 401) {
					// You must turn off camera User Access Control before this
					// will work
					return null;
				}
				return new MjpegInputStream(res.getEntity().getContent());
			} catch (ClientProtocolException e) {
				if (DEBUG) {
					e.printStackTrace();
					Log.d(TAG, "Request failed-ClientProtocolException", e);
				}
				// Error connecting to camera
			} catch (IOException e) {
				if (DEBUG) {
					e.printStackTrace();
					Log.d(TAG, "Request failed-IOException", e);
				}
				// Error connecting to camera
			}
			return null;
		}

		protected void onPostExecute(MjpegInputStream result) {
			mv.setSource(result);
			if (result != null) {
				result.setSkip(1);
				setTitle(R.string.app_name);
			} else {
				setTitle(R.string.title_disconnected);
			}
			mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
			mv.showFps(false);
		}
	}

	public class RestartApp extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... v) {
			MjpegActivity.this.finish();
			return null;
		}

		protected void onPostExecute(Void v) {
			startActivity((new Intent(MjpegActivity.this, MjpegActivity.class)));
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		switch (seekBar.getId()) {
		case R.id.seekbar:
			value2.setText("HSeekBar value is " + progress);
			break;
		case R.id.calculatorVerticalSeekBar:
			value.setText("VSeekBar value is " + progress);
			break;
			// TODO Auto-generated method stub
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		Sensor mySensor = event.sensor;
		if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			int gfps = MjpegView.getFps();
			float x = event.values[0];
	        float y = event.values[1];
	        float z = event.values[2];
	        x_t.setText("X value: "+x);
	        y_t.setText("Y value: "+y);
	        z_t.setText("Z value: "+z);
	        fps.setText("FPS: " +Integer.toString(gfps));
	    }
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
}
