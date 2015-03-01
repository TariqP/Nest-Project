package com.example.tariq.nestproject;

import android.content.IntentSender;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import android.provider.Settings.System;
import android.view.View;


public class MainActivity extends ActionBarActivity {

    private static final int REQUEST_OAUTH = 1;
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;
    private GoogleApiClient fitClient = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        //checks if there's previously saved data
//        if (savedInstanceState != null) {
//            authInProgress = savedInstanceState.getBoolean("authInProgress");
//        }

        buildFitnessClient();
        //the Firebase client is used to read/write data to the Nest
        Firebase.setAndroidContext(this);
    }

    //all this code is from the Google developer site. It's used to set up the client that will read Google Fit data
    private void buildFitnessClient() {
        fitClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.v("Fitness", "Connected!!");
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.v("Fitness", "Network lost");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.v("Fitness", "Service disconnected");
                                }

                            }
                        }
                )
                .addOnConnectionFailedListener(
                        new GoogleApiClient.OnConnectionFailedListener() {

                            @Override
                            public void onConnectionFailed(ConnectionResult connectionResult) {
                                Log.v("Fitness", "Connection failed. Cause: " + connectionResult);
                                if (!connectionResult.hasResolution()) {
                                    Log.v("Fitness", "No fail resolution");
                                    return;
                                }
                                if (!authInProgress) {
                                    try {
                                        authInProgress = true;
                                        connectionResult.startResolutionForResult(MainActivity.this, REQUEST_OAUTH);
                                    } catch (IntentSender.SendIntentException e) {
                                        Log.v("Fitness", "Exception while trying to resolve failure");
                                    }
                                }

                            }
                        }
                )
                .build();

    }

    //This requests the data from the Google Fit client
    public void getFitnessInfo(View view) {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.HOUR_OF_DAY, -1);
        long startTime = cal.getTimeInMillis();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                //the first parameter is the type of data I want. The aggregate function finds the data source for this type of data
                //and then USING that source it returns the type of data I want outputted (second parameter)
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                //make buckets of data for every 1 minute
                .bucketByTime(1, TimeUnit.MINUTES)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        new DataReadTask().execute(fitClient, readRequest);

    }

    //used to asynchronously request and read the data from the google fit client
    private class DataReadTask extends AsyncTask {

        protected void onPostExecute(DataReadResult readResult) {
            dumpDataSet(readResult.getDataSet(DataType.AGGREGATE_STEP_COUNT_DELTA));
        }

        @Override
        protected DataReadResult doInBackground(Object[] params) {
            DataReadResult dataReadResult =
                    Fitness.HistoryApi.readData((GoogleApiClient)params[0], (DataReadRequest)params[1]).await(1, TimeUnit.MINUTES);
            return dataReadResult;
        }
    }

    //gets the data from the read and prints it out to the console
    private void dumpDataSet(DataSet dataSet) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(System.DATE_FORMAT);

        for (DataPoint dp : dataSet.getDataPoints()) {
            String dataPoint = "\nData point: " +
                    "\t Type: " + dp.getDataType().getName() +
                    "\t Start: " + dateFormatter.format(dp.getStartTime(TimeUnit.MILLISECONDS)) +
                    "\t End: " + dateFormatter.format(dp.getEndTime(TimeUnit.MILLISECONDS));
            for (Field field : dp.getDataType().getFields()) {
                dataPoint += "\tField: " + field.getName()  +
                        " Value: " + dp.getValue(field);
            }

            Log.v("Data", dataPoint);
        }
    }

    //builds the client to connect to the Firebase app (what is used to control Nest)
    public void buildFirebaseRef(View view) {
        Firebase myFireBaseRef = new Firebase("https://crackling-inferno-1632.firebaseio.com/");
        myFireBaseRef.child("message").setValue("Do you have any friends?");
    }
}
