package com.example.pocketnpm;

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

import android.os.AsyncTask;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
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
import android.support.v4.app.NavUtils;

public class pocket_npm extends Activity {
	List<String> items = new ArrayList<String>();
	JSONArray arr;
	ArrayAdapter<String> adapter;
	ProgressDialog pg;
	ListView lv;
	ImageView img;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_pocket_npm);
        
        getActionBar().setDisplayShowHomeEnabled(true);
        
        //Image we'll use for the shadow under the list
        img = new ImageView(this);
        
        lv = (ListView)findViewById(R.id.package_list);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        
        lv.addFooterView(img);
        
        adapter = new ArrayAdapter<String>(this, R.layout.list_item, items);
        lv.setAdapter(adapter);
        
        lv.setOnItemClickListener(new OnItemClickListener(){
        	@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				Intent intent = new Intent(pocket_npm.this, PackageDetail.class);
				
				JSONObject obj = null;
				try {
					obj = arr.getJSONObject(arg2);
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				try {
					String ver = obj.getJSONObject("value").getJSONObject("dist-tags").getString("latest");
					intent.putExtra("version", ver);
					obj = obj.getJSONObject("value").getJSONObject("versions").getJSONObject(ver);
					intent.putExtra("title", items.get(arg2));
					intent.putExtra("description", obj.getString("description"));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					intent.putExtra("username", obj.getJSONObject("_npmUser").getString("name"));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					intent.putExtra("fullname", obj.getJSONObject("author").getString("name"));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					if(obj.getJSONObject("repository").getString("type").equals("git"))
						intent.putExtra("link", obj.getJSONObject("repository").getString("url").replaceAll("git://", "http://").replaceAll(".git$", ""));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				startActivity(intent);
			}
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_pocket_npm, menu);
        
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        
        return true;
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
          String query = intent.getStringExtra(SearchManager.QUERY);
          query = query.split(" ")[0];
          try {
        	pg = ProgressDialog.show(this, "Loading", "Fetching hot javascript modules...");
			URL url = new URL("http://search.npmjs.org/_list/search/search?startkey=%22" + query + "%22&endkey=%22" + query + "ZZZZZZZZZZZZZZZZZZZ%22&limit=25");
			new SearchNPM().execute(url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			Toast.makeText(getApplicationContext(), "That url was baaaaaaad", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
        }
    }
    
    private class SearchNPM extends AsyncTask<URL, Integer, Long> {
    	long whyDoINeedThis = 1;	//I have to return a long? Like, it's mandatory? Why?
    	String response = "";
    	
        protected Long doInBackground(URL... urls) {
            Log.d("BACK", "I'm in the background son!");	//Oh hey! Look, we're actually doing work now. Thank god.
            try {
				response = HTTPRequest(urls[0]);	//Forced to expect an array of urls...
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return whyDoINeedThis;
        }

        protected void onPostExecute(Long result) {
            Log.d("DONE", "I'm done bitch");
            JSONObject json = null;
            //Now we have our results. Show them to the user
            try {
				json = new JSONObject(response);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
            try {
            	items.clear();
				arr = json.getJSONArray("rows");
				for(int i = 0; i < arr.length(); ++i){
					items.add(arr.getJSONObject(i).getString("key"));
				}
				adapter.notifyDataSetChanged();
				pg.dismiss();
				if(arr.length() > 0){
					
					img.setBackgroundResource(R.drawable.bottom_shadow);
				}
				else
					img.setBackgroundResource(0);
				
				Log.d("LENGTH", arr.getJSONObject(2).getString("key"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
    
    String HTTPRequest(URL url) throws IOException{
    	HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    	String response = "";
    	try {
    		InputStream in = new BufferedInputStream(urlConnection.getInputStream());
    		byte[] contents = new byte[1024];
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
    	
    	return response;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                items.clear();
                adapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
}