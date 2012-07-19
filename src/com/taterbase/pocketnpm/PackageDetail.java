package com.taterbase.pocketnpm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class PackageDetail extends SherlockActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.package_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);	//Adding back ability to home icon in actionbar
        
        //Grabbing all necessary strings from the bundle
        Bundle extras = getIntent().getExtras();
        String username = extras.getString("username");
        String fullname = extras.getString("fullname");
        String title = extras.getString("title");
        String description = extras.getString("description");
        String version = extras.getString("version");
        String link = extras.getString("link");
        
        //If the username is the same as the full name, or only a username was provided, just use the username
        if(fullname == null || fullname.equals(username)){
        	fullname = username;
        	username = "";
        }
        
        //Grabbing our views
        TextView detailTitle = (TextView) findViewById(R.id.detail_title);
        TextView detailUsername = (TextView) findViewById(R.id.detail_author);
        TextView detailFullName = (TextView) findViewById(R.id.detail_author_name);
        TextView detailDescription = (TextView) findViewById(R.id.detail_description);
        TextView detailVersion = (TextView) findViewById(R.id.detail_version);
        TextView detailLink = (TextView) findViewById(R.id.detail_link);
        
        //Setting up our views
        detailTitle.setText(title);
        detailUsername.setText(username);
        detailFullName.setText(fullname);
        detailDescription.setText(description);
        detailVersion.setText("latest("+version+")");
        detailLink.setText(link);
        
        setTitle("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_package_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Go back in the activity stack, more importantly to the list of packages yay!!
                Intent intent = new Intent(this, PocketNPM.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
}
