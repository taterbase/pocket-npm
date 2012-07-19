package com.taterbase.pocketnpm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.taterbase.widget.MenuItemSearchAction;
import com.taterbase.widget.SearchPerformListener;

public class PocketNPM extends SherlockActivity implements SearchPerformListener {
	private List<String> items = new ArrayList<String>();
	private JSONArray arr;
	private ArrayAdapter<String> adapter;
	private ProgressDialog pg;
	private ListView lv;
	private ImageView img;
	private MenuItemSearchAction searchView;
	private Random randInt = new Random();
	
	private final String QUERY = "QUERY";
	
	private String[] verses = {
			"For Ryan Dahl so loved the world, that he gave his only begotten event loop, that whosoever performed async IO could have eternal callbacks.",
	        "There is no event loop except for the Event Loop alone; and Isaacs is it's messenger.",
	        "I can code all things through Node who events my IO.",
	        "Ask not what Node can do for you, but how far you can nest your callbacks for Node.",
	        "Happiness is when what you think, what you say, and what you do are in NodeJS.",
	        "JavaScript is fun and so Node is fun. #jifasnif"
	};
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_pocket_npm);
       
        //Image we'll use for the shadow under the list
        img = new ImageView(this);
        
        //Building the list view 
        lv = (ListView)findViewById(R.id.package_list);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        //Even though we haven't set a drawable yet, we want to set the footer for later
        lv.addFooterView(img);
        
        //Creating and setting the adapter to our items array of strings (titles of packages)
        adapter = new ArrayAdapter<String>(this, R.layout.list_item, items);
        lv.setAdapter(adapter);
        
        //Adding onclick listener to show details of packages when clicked
        lv.setOnItemClickListener(new OnItemClickListener(){
        	@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
        		//Create intent for detail page
				Intent intent = new Intent(PocketNPM.this, PackageDetail.class);
				
				JSONObject obj = null;
				
				/* The reason for all of the try catches is for hackiness (for now). We're not sure
				 * which elements of the JSON will actually be there. In the future we'll use introspection
				 * to check for keys we want and group everything into a larger try catch
				 */
				try {
					obj = arr.getJSONObject(arg2);	//Grabbing the object we want from the array based on index of click
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				try {
					String ver = obj.getJSONObject("value").getJSONObject("dist-tags").getString("latest");	//Grabbing latest version
					intent.putExtra("version", ver);	//Setting latest version in intent info
					obj = obj.getJSONObject("value").getJSONObject("versions").getJSONObject(ver);	//Grabbing latest package data based on latest version
					intent.putExtra("title", items.get(arg2));	//Setting title from items array (The same we use to populate our list)
					intent.putExtra("description", obj.getString("description"));	//Grab description
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					intent.putExtra("username", obj.getJSONObject("_npmUser").getString("name"));	//Grab username if it exists
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					intent.putExtra("fullname", obj.getJSONObject("author").getString("name"));	//Grab full name if it exists
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					//If the repository is listed and is a git repo, grab the url and use regex to remove git specific portions so we can link to it
					if(obj.getJSONObject("repository").getString("type").equals("git"))
						intent.putExtra("link", obj.getJSONObject("repository").getString("url").replaceAll("git://", "http://").replaceAll("git@", "http://").replaceAll("github.com:", "github.com/").replaceAll(".git$", ""));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//Start the activity! See you in PackageDetail
				startActivity(intent);
			}
        });
    }

    @Override	//Creating our options!
    public boolean onCreateOptionsMenu(Menu menu) {
    	searchView = new MenuItemSearchAction(getApplicationContext(), menu, this);
        return true;
    }
    
	@Override
	public void performSearch(String query) {
		clearList();
		Intent searchIntent = new Intent(this, PocketNPM.class);
		searchIntent.setAction(Intent.ACTION_SEARCH);
		searchIntent.putExtra(QUERY, query);
		
		startActivity(searchIntent);
	}
    
    @Override	//Where we're expecting search intents to come through
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {	//When we get a search
          searchView.clearFocus();	//Make the keyboard go down
          clearList();
          
          String query = intent.getStringExtra(QUERY);	//Grab the query
          String[] queryArr = query.split(" ");	//"Explode" the query by its spaces and just use the first word for now. Ghetto I know, we'll think of something later.
          try {
        	int rand = randInt.nextInt((verses.length - 1));
        	String message = verses[rand];
        	
        	pg = ProgressDialog.show(this, "Loading", message);
			
        	// Build our search query
//        	URL url = new URL("http://search.npmjs.org/_list/search/search?startkey=%22" + query + "%22&endkey=%22" + query + "ZZZZZZZZZZZZZZZZZZZ%22&limit=25");
			String str = new String("http://pocketnpm.jit.su/search?0=");
			for(int i = 0; i < queryArr.length; ++i){
				str+= queryArr[i];
				str+= "&"+ String.valueOf(i + 1) + "=";
			}
			Log.d("QUERY", str);
        	URL url = new URL(str);
        	new SearchNPM().execute(url);	//Running our async task to grab results
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			Toast.makeText(getApplicationContext(), "That url was baaaaaaad", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
        }
    }
    
    private class SearchNPM extends AsyncTask<URL, Integer, Void> {
    	String response = "";	//Initializing the response string
    	
        protected Void doInBackground(URL... urls) {	//Due to the rigidity of doInBackground we have to expect an array of urls
            Log.d("BACK", "I'm in the background son!");	//Oh hey! Look, we're actually doing work now.
            try {
				response = HTTPRequest(urls[0]);	//We know we only got one URL, just use that
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return null;
        }

        protected void onPostExecute(Void none) {
            Log.d("DONE", "I'm done dood");
            JSONObject json = null;
            //Now we have our results. Show them to the user
            try {
				json = new JSONObject(response);	//This is slow. Slim down the JSON on the server as much as possible before this point
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//Now that we have our JSON object, let's parse out our packages
            try {
            	//Grab the array of packages
				arr = json.getJSONArray("rows");
				
				//Loop through and store all of the titles
				for(int i = 0; i < arr.length(); ++i){
					items.add(arr.getJSONObject(i).getString("key"));
				}
				
				//Udate the listview and dismiss the progress dialog
				adapter.notifyDataSetChanged();
				pg.dismiss();
				if(arr.length() > 0){
					//If we have a list, set the shadow for depth
					img.setBackgroundResource(R.drawable.bottom_shadow);
				}
				else //Otherwise remove any drawable
					img.setBackgroundResource(0);
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
    
    //Our sick request function
    String HTTPRequest(URL url) throws IOException{
    	HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    	String response = "";
    	try {
    		//Grab the input stream from urlconnection
    		InputStream in = new BufferedInputStream(urlConnection.getInputStream());
    		byte[] contents = new byte[1024];	//Instantiate a byte array to read in the data
    		int bytesRead=0;
    		while( (bytesRead = in.read(contents)) != -1){ 
    			response += new String(contents, 0, bytesRead);               
    		}
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	finally {
    	    urlConnection.disconnect();
    	}
    	
    	return response;	//Send back that response string holmes!
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // I want this to empty the list, doesn't work right now :(
                items.clear();
                adapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void clearList(){
    	items.clear();
    	adapter.notifyDataSetChanged();
    	img.setBackgroundResource(0);
    }
}