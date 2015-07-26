package com.mobshep.shepherdlogin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.internal.widget.AppCompatPopupWindow;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * This file is part of the Security Shepherd Project.
 *
 * The Security Shepherd project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.<br/>
 *
 * The Security Shepherd project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.<br/>
 *
 * You should have received a copy of the GNU General Public License
 * along with the Security Shepherd project.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Sean Duggan
 */

public class MainActivity extends ActionBarActivity {

    EditText username, password;
    SharedPreferences storedPref;
    SharedPreferences.Editor toEdit;

    TextView tvResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        referenceXML();

        //TODO
        //Remove this.
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //end remove

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String address = SP.getString("server_preference", "NA");

        Toast addressTest = Toast.makeText(MainActivity.this, "Server Address : " + address, Toast.LENGTH_LONG);
        addressTest.show();

    }

    public void submitClicked(View v) {

        if (username.getText().toString() == "" || password.getText().toString() == ""){
            Toast blank = Toast.makeText(MainActivity.this, "Blank Fields Detected!", Toast.LENGTH_SHORT);
            blank.show();
            return;
        }


        ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("login", username.getText().toString()));
        postParameters.add(new BasicNameValuePair("pwd", password.getText().toString()));

        try {

            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String address = SP.getString("server_preference", "NA");

            String res = CustomHttpClient.executeHttpPost(address , postParameters);

            JSONObject jObject = new JSONObject(res);

            String response = (String) jObject.getString("JSESSIONID");

            System.out.println("SessionId: " + response);

            response = response.replaceAll("\\s+", "");

            Toast responseError = Toast.makeText(MainActivity.this,
                    response, Toast.LENGTH_SHORT);
            responseError.show();


            if (response.contains("Value ERROR")==true){
                tvResponse.setText("Invalid username or password");
            }
            else{
                tvResponse.setText(response);
            }

            if (res!=null) {
                Toast valid = Toast.makeText(MainActivity.this,
                        "Logged In!", Toast.LENGTH_SHORT);
                valid.show();

                storedPref = getSharedPreferences("Sessions", MODE_PRIVATE);
                toEdit = storedPref.edit();
                toEdit.putString("sessionId", response);
                toEdit.commit();

                Intent intent = new Intent(MainActivity.this, LoggedIn.class);
                startActivity(intent);

            } else {

                Toast invalid = Toast.makeText(MainActivity.this,
                        "Invalid Credentials!", Toast.LENGTH_SHORT);
                invalid.show();
            }
        } catch (Exception e) {

            Toast responseError = Toast.makeText(MainActivity.this,
                    e.toString(), Toast.LENGTH_LONG);
            responseError.show();

            tvResponse.setText(e.toString());

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {

            case R.id.action_settings:

                Intent goToSettings = new Intent(this, Preferences.class);
                startActivity(goToSettings);
                return true;

            case R.id.action_exit:

                this.finish();

            break;

            default:

        }
        return super.onOptionsItemSelected(item);

    }

    private void referenceXML() {

        username = (EditText) findViewById(R.id.etUsername);
        password = (EditText) findViewById(R.id.etPassword);
        tvResponse = (TextView) findViewById(R.id.tvResponse);

    }


}
