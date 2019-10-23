package com.inyekprint.app;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class DetailActivity extends AppCompatActivity {


    TextView AgentName,TicketNo, Date, purchaseDate, Route, Coach_Time, Seat, BoardingPoint, Phone, TicketPrice, Total;
    Button PrintBtn;
    TextView Title;




    private Spinner deviceName;
    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket socket;
    BluetoothDevice bluetoothDevice;
    OutputStream outputStream;
    InputStream inputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    String value = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        //Title = (TextView) findViewById(R.id.Title);
        deviceName = (Spinner) findViewById(R.id.spinnerDevice);
        //////////////BluetoothConnection------->
        BlutoothConnection();
        //////////////BluetoothConnection<-------
        PrintBtn = (Button) findViewById(R.id.printBtn);
        TicketNo = (TextView) findViewById(R.id.ticket_no);
        Date = (TextView) findViewById(R.id.date);
        purchaseDate = (TextView) findViewById(R.id.purchase);
        Route = (TextView) findViewById(R.id.route);
        Coach_Time = (TextView) findViewById(R.id.coach_time);
        Seat = (TextView) findViewById(R.id.seat);
        BoardingPoint = (TextView) findViewById(R.id.boarding_point);
        Phone = (TextView) findViewById(R.id.phone_number);
        TicketPrice = (TextView) findViewById(R.id.ticket_price);
        Total = (TextView) findViewById(R.id.total);
        //progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        AgentName=(TextView)findViewById(R.id.Title);
        //StandTime=(TextView)findViewById(R.id.Stand_Time);

        AgentName.setText(getIntent().getStringExtra("AgentName"));
        TicketNo.setText(getIntent().getStringExtra("TicketNo"));
        Date.setText(getIntent().getStringExtra("Date"));
        purchaseDate.setText(getIntent().getStringExtra("purchaseDate"));
        Route.setText(getIntent().getStringExtra("Route"));
        Coach_Time.setText(getIntent().getStringExtra("Coach_Time"));
        Seat.setText(getIntent().getStringExtra("Seat"));
        BoardingPoint.setText(getIntent().getStringExtra("BoardingPoint"));
        Phone.setText(getIntent().getStringExtra("Phone"));
        TicketPrice.setText(getIntent().getStringExtra("TicketPrice"));
        Total.setText(getIntent().getStringExtra("Totall"));




        PrintBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                builder.setTitle("Are you sure?")
                        .setMessage("Make sure your printer is on!")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                IntentPrint(AgentName.getText()
                                        + "\n------------------------------------------------"
                                        + "\n" + TicketNo.getText()
                                        + "\n" + Date.getText()
                                        + "\n" + purchaseDate.getText()
                                        + "\n" + Route.getText()
                                        + "\n" + Coach_Time.getText()
                                        + "\n" + Seat.getText()
                                        + "\n" + BoardingPoint.getText()
                                        + "\n" + Phone.getText()
                                        + "\n" + TicketPrice.getText()
                                        + "\n------------------------------------------------"
                                        + "\n" + Total.getText()
                                        + "\n\n\n\n");

                                Intent intent = new Intent(DetailActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }).setNegativeButton("Cancel", null);
                AlertDialog alert = builder.create();
                alert.show();

            }
        });
    }

    public void IntentPrint(String txtvalue) {
        byte[] buffer = txtvalue.getBytes();
        byte[] PrintHeader = {(byte) 0xAA, 0x55, 2, 0};
        PrintHeader[3] = (byte) buffer.length;
        InitPrinter();
        if (PrintHeader.length > 128) {
            value += "\nValue is more than 128 size\n";
            Toast.makeText(this, value, Toast.LENGTH_LONG).show();
        } else {
            try {

                outputStream.write(txtvalue.getBytes());
                Toast.makeText(getApplicationContext(), "Printing", Toast.LENGTH_SHORT).show();
                outputStream.close();
                socket.close();
            } catch (Exception ex) {

                value += "No Devices found";
                Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void InitPrinter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals(deviceName.getSelectedItem())) //Note, you will need to change this to match the name of your device
                    {
                        bluetoothDevice = device;
                        break;
                    }
                }
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
                Method m = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                socket = (BluetoothSocket) m.invoke(bluetoothDevice, 1);
                bluetoothAdapter.cancelDiscovery();
                socket.connect();
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();
                beginListenForData();

            } else {
                value += "No Devices found";
                Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception ex) {

        }
    }

    void beginListenForData() {
        try {
            final Handler handler = new Handler();

            // this is the ASCII code for a newline character
            final byte delimiter = 10;

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            workerThread = new Thread(new Runnable() {
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {

                        try {

                            int bytesAvailable = inputStream.available();

                            if (bytesAvailable > 0) {

                                byte[] packetBytes = new byte[bytesAvailable];
                                inputStream.read(packetBytes);

                                for (int i = 0; i < bytesAvailable; i++) {

                                    byte b = packetBytes[i];
                                    if (b == delimiter) {

                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(
                                                readBuffer, 0,
                                                encodedBytes, 0,
                                                encodedBytes.length
                                        );

                                        // specify US-ASCII encoding
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        // tell the user data were sent to bluetooth printer device
                                        handler.post(new Runnable() {
                                            public void run() {
                                                Log.d("e", data);
                                            }
                                        });

                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }

                        } catch (IOException ex) {
                            stopWorker = true;
                        }

                    }
                }
            });

            workerThread.start();

        } catch (Exception e) {

        }
    }

    void BlutoothConnection() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
                return;
            }
            if (bluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                ArrayList<String> s = new ArrayList<String>();

                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {

                        s.add(device.getName());
                    }
                    ArrayAdapter<String> AllDevice = new ArrayAdapter<String>(this, R.layout.sample_layout, R.id.spinner_View, s);
                    deviceName.setAdapter(AllDevice);
                } else {
                    value += "No Devices found";
                    Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } catch (Exception ex) {

            value += "No Devices found";
            Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
        }
    }
//////////////////////////printing process_end<--------------
}
