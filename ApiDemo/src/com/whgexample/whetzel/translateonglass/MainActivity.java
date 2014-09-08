package com.whgexample.whetzel.translateonglass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.glass.app.Card;
import com.google.android.glass.sample.apidemo.R;
import com.google.android.glass.sample.apidemo.R.layout;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends Activity {
	 private static final String TAG = MainActivity.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		displaySpeechRecognizer();		
		//setContentView(R.layout.activity_main);
	}

	//See: https://developers.google.com/glass/develop/gdk/input/voice#starting_glassware
    //See: http://developer.android.com/reference/android/speech/SpeechRecognizer.html
    final int SPEECH_REQUEST = 0;
    private void displaySpeechRecognizer() {
        String prompt = "What word or phrase \nwould you like to translate?"; //Add newline tp help format text and not display over speech prompt
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // http://developer.android.com/reference/android/speech/RecognizerIntent.html#EXTRA_PROMPT
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 10000);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        startActivityForResult(intent, SPEECH_REQUEST);
    }
	
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        AsyncTask translate = null;
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            String spokenText = results.get(0);
            Log.d(TAG, "Speech Input: " + spokenText);
            //Call AsyncTask, TODO: change to SyncAdapter
            if (null == translate || translate.isCancelled()) {
                translate = new TranslateTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, spokenText);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    public class TranslateTask extends AsyncTask<String, Void, String> {
        private final String TAG = TranslateTask.class.getSimpleName();

        private String getTranslationFromJson(String results, String spokenText) throws JSONException {

            final String DATA = "data";
            final String TRANSLATIONS = "translations";
            final String TRANSLATED_TEXT = "translatedText";

            //final String translation = spokenText;
            JSONObject textTranslationJson = new JSONObject(results);
            Log.v(TAG, "JSON "+textTranslationJson);
            JSONObject data = textTranslationJson.getJSONObject(DATA);
            JSONObject translations = data.getJSONArray(TRANSLATIONS).getJSONObject(0);
            String translatedText = translations.getString(TRANSLATED_TEXT);

            return translatedText;
        }

		@Override
		protected String doInBackground(String... params) {
			if (params.length == 0) {
			return null;
		}
			   HttpURLConnection urlConnection = null;
	            BufferedReader reader = null;

	            String spokenText = params[0];

	            Log.v(TAG, "Spoken Text: " + spokenText);
	            final String KEY_PARAM ="key";
	            final String API_KEY = "AIzaSyBOb_kV7YV9atkfYZrlkwNx54MRbCxEUGg";
	            final String QUERY_PARAM = "q";
	            final String SOURCE_PARAM = "source";
	            final String SOURCE = "en";
	            final String TARGET_PARAM = "target";
	            final String TARGET = "es";
	            String results = null;

	            try {
	                final String BASE_URL = "https://www.googleapis.com/language/translate/v2";
	                //https://www.googleapis.com/language/translate/v2?key=INSERT-YOUR-KEY&q=hello%20world&source=en&target=de
	                //https://www.googleapis.com/language/translate/v2?key=AIzaSyBOb_kV7YV9atkfYZrlkwNx54MRbCxEUGg&q=hello%20world&source=en&target=es
	                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
	                        .appendQueryParameter(KEY_PARAM, API_KEY)
	                        .appendQueryParameter(QUERY_PARAM, spokenText)
	                        .appendQueryParameter(SOURCE_PARAM, SOURCE)
	                        .appendQueryParameter(TARGET_PARAM, TARGET)
	                        .build();

	                URL url = new URL(builtUri.toString());
	                Log.v(TAG, "Query URL: " + url.toString());

	                // Create the request to Google Translate, and open the connection
	                urlConnection = (HttpURLConnection) url.openConnection();
	                urlConnection.setRequestMethod("GET");
	                urlConnection.connect();

	                // Read the input stream into a String
	                InputStream inputStream = urlConnection.getInputStream();
	                StringBuffer buffer = new StringBuffer();
	                if (inputStream == null) {
	                    // Nothing to do.
	                    results = null;
	                }
	                reader = new BufferedReader(new InputStreamReader(inputStream));

	                String line;
	                while ((line = reader.readLine()) != null) {
	                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
	                    // But it does make debugging a *lot* easier if you print out the completed
	                    // buffer for debugging.
	                    buffer.append(line + "\n");
	                }

	                if (buffer.length() == 0) {
	                    // Stream was empty.  No point in parsing.
	                    results = null;
	                }
	                results = buffer.toString();
	            } catch (IOException e) {
	                Log.e(TAG, "Error ", e);
	                // If the code didn't successfully get the weather data, there's no point in attempting
	                // to parse it.
	                results = null;
	            } finally {
	                if (urlConnection != null) {
	                    urlConnection.disconnect();
	                }
	                if (reader != null) {
	                    try {
	                        reader.close();
	                    } catch (final IOException e) {
	                        Log.e(TAG, "Error closing stream", e);
	                    }
	                }
	            }

	            try {
	                return getTranslationFromJson(results, spokenText);
	            } catch (JSONException e) {
	                Log.e(TAG, e.getMessage(), e);
	                e.printStackTrace();
	            }
	            return null;
	        }
		
		 @Override
	        protected void onPostExecute(String result) {
	            super.onPostExecute(result);
	            Log.d(TAG, "LOG onPostExecute " + result);
	            processValue(result);
	        }
    }
    
    private void getValue()
    {
        new TranslateTask().execute();
    }
    
    private void processValue(String myValue)
    {
        Log.d(TAG, "LOG processValue: "+myValue);
        //handle value
        Card card = new Card(this);
        card.setText(myValue);
        View cardView = card.getView();
        setContentView(cardView);
        //TODO: Read text on Card so it is also audible
    }      	
} 
