package com.inyekprint.app;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {


    public static TextView Test,SearchTicket;
    Button Search;
    BluetoothAdapter bluetoothAdapter;
    public static String Data;
    private ProgressBar progressBar;
    int progress;
    String data = "";
    String AgentName,TicketNoo, Date, purchaseDate, Coach_Time, Seat, BoardingPoint, Phone, TicketPrice, Totall, Route;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!isConnected(MainActivity.this)) buildDialog(MainActivity.this).show();
        else {

            setContentView(R.layout.activity_main);

            Search = (Button) findViewById(R.id.search);
            SearchTicket = (TextView) findViewById(R.id.SearchTicket);
            progressBar = (ProgressBar) findViewById(R.id.progressBar1);
            progressBar.setVisibility(View.INVISIBLE);
            CheckBlutoothConnection();

            Search.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isConnected(MainActivity.this)) buildDialog(MainActivity.this).show();

                    else {
                        if (SearchTicket.getText().toString().equals("")) {
                            Toast.makeText(MainActivity.this, "Please Enter a Ticket Number", Toast.LENGTH_SHORT).show();

                        } else {



                            new mytask().execute();

                            progressBar.setVisibility(View.VISIBLE);
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    doWork();

                                }

                            });
                            thread.start();


                        }

                    }


                }
            });
        }
    }

    public void doWork() {


        for (progress = 1; progress < 100; progress = progress + 40) {
            try {

                Thread.sleep(1000);
                progressBar.setProgress(progress);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        progressBar.setProgress(0);


        progressBar.setVisibility(View.INVISIBLE);

    }


    ////getData fromApi

    public class mytask extends AsyncTask<String,Integer,String>
    {

        @Override
        protected String doInBackground(String... strings) {
            int call = 0;

            try {
                ////Api connecting------>
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
                RequestBody body = RequestBody.create(mediaType, "app_key=%24%406182%268392kdml&undefined=");
                Request request = new Request.Builder()
                        .url("https://app.inyek.com/app_api/api_extra/all_order.php")
                        .post(body)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .addHeader("app_key", "$@6182&8392kdml")
                        .addHeader("cache-control", "no-cache")
                        .addHeader("Postman-Token", "e331c457-4cec-406e-a3eb-0a5c415fbe6c")
                        .build();
                Response response = client.newCall(request).execute();
                /////////Api Connecting _End<---------
                /////Read and get data from api-------->
                InputStream inputStream = response.body().byteStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = "";
                while (line != null) {
                    line = bufferedReader.readLine();
                    data = data + line;
                }
                JSONArray JA = new JSONArray(data);
                for (int i = 0; i < JA.length(); i++) {
                    JSONObject JO = (JSONObject) JA.get(i);
                    if (JO.get("ticket_number").equals(SearchTicket.getText().toString()) || JO.get("phone_number").equals(MainActivity.SearchTicket.getText().toString())) {
                        AgentName=""+JO.get("agent_name");
                        TicketNoo = "Ticket Number -" + JO.get("ticket_number");
                        Date = "Journey Date " + JO.get("journey_date");
                        purchaseDate = "Purchase Date " + JO.get("ticket_issue_time");
                        Route = "" + JO.get("route_source")+" To "+ JO.get("route_destination");
                        BoardingPoint = "Boarding Point- " + JO.get("boarding_point");
                        Coach_Time = "" + JO.get("bus_condition") + " " + JO.get("journey_start_time");
                        Seat = "Seats :" + JO.get("seat");
                        Phone = "Phone Number " + JO.get("phone_number");
                        TicketPrice = "Ticket Price :" + JO.get("ticket_price");
                        Totall = "Total :" + JO.get("total_price");
                        call++;
                    }
                }

                if (call <= 0) {

                    Coach_Time = "No Data Found";

                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            try{

                if ((Coach_Time.equals("No Data Found")))
                {
                    Toast.makeText(getApplicationContext(), "No Data Found", Toast.LENGTH_LONG).show();
                }
                else {

                    progressBar.setVisibility(View.INVISIBLE);
                    Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                    intent.putExtra("AgentName",AgentName);
                    intent.putExtra("TicketNo",TicketNoo);
                    intent.putExtra("Date",Date);
                    intent.putExtra("purchaseDate",purchaseDate);
                    intent.putExtra("Route",Route);
                    intent.putExtra("BoardingPoint",BoardingPoint);
                    intent.putExtra("Coach_Time",Coach_Time);
                    intent.putExtra("Seat",Seat);
                    intent.putExtra("Phone",Phone);
                    intent.putExtra("TicketPrice",TicketPrice);
                    intent.putExtra("Totall",Totall);

                    startActivity(intent);


                }

            }catch (Exception e)
            {
                Toast.makeText(getApplicationContext(), "Mobile Data may not be available", Toast.LENGTH_LONG).show();

            }




        }

    }
    ////get data from api_end


    ////Internet check--->
    public boolean isConnected(Context context) {



            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netinfo = cm.getActiveNetworkInfo();

            if (netinfo != null && netinfo.isConnectedOrConnecting()) {
                android.net.NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                android.net.NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

                if ((mobile != null && mobile.isConnectedOrConnecting()) || (wifi != null && wifi.isConnectedOrConnecting())) {
                    return true;
                } else {
                    return false;
                }
            } else
                return false;




    }

    public AlertDialog.Builder buildDialog(Context c) {

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("No Internet Connection");
        builder.setMessage("Turn on Mobile Data or wifi.\n\nPress ok to Exit");

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                finish();
            }
        });

        return builder;
    }

    /////Internet check_end<----
    ///Bltutooth check--->

    public void CheckBlutoothConnection() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
                return;
            }

        } catch (Exception ex) {

        }
    }
    ////blutooth check_end<------
}
