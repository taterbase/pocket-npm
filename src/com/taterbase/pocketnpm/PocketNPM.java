package com.taterbase.pocketnpm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.taterbase.pocketnpm.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

public class PocketNPM extends Activity {
	private List<String> items = new ArrayList<String>();
	private JSONArray arr;
	private ArrayAdapter<String> adapter;
	private ProgressDialog pg;
	private ListView lv;
	private ImageView img;
	private SearchView searchView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_pocket_npm);
        
        getActionBar().setDisplayShowHomeEnabled(true);
        
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
						intent.putExtra("link", obj.getJSONObject("repository").getString("url").replaceAll("git://", "http://").replaceAll(".git$", ""));
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
        getMenuInflater().inflate(R.menu.activity_pocket_npm, menu);
        
        // Get the SearchView and set the searchable configuration
        
        //This magic right here makes the action bar search and search button work together some how
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Keep actionbar search field always open
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener( ) {
            @Override
            public boolean   onQueryTextChange( String newText ) {
                //Will eventually set up instant search here, need to replicate the NPM db first
            	clearList();
                return true;
            }

			@Override
			public boolean onQueryTextSubmit(String arg0) {
				// TODO Auto-generated method stub
				return false;
			}
        });
        
        
        return true;
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
          
          String query = intent.getStringExtra(SearchManager.QUERY);	//Grab the query
          String[] queryArr = query.split(" ");	//"Explode" the query by its spaces and just use the first word for now. Ghetto I know, we'll think of something later.
          try {
        	pg = ProgressDialog.show(this, "Loading", "Fetching hot javascript modules...");
			
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
    
    private class SearchNPM extends AsyncTask<URL, Integer, Long> {
    	long whyDoINeedThis = 1;	//I have to return a long? Like, it's mandatory? Why?
    	String response = "";	//Initializing the response string
    	
        protected Long doInBackground(URL... urls) {	//Due to the rigidity of doInBackground we have to expect an array of urls
            Log.d("BACK", "I'm in the background son!");	//Oh hey! Look, we're actually doing work now. Thank god.
            try {
				response = HTTPRequest(urls[0]);	//We know we only got one URL, just use that
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return whyDoINeedThis;	//Again, why?
        }

        protected void onPostExecute(Long result) {
            Log.d("DONE", "I'm done dood");
            JSONObject json = null;
            //Now we have our results. Show them to the user
            try {
				json = new JSONObject(response);	//I think this may be a massive bottleneck
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