package com.example.bluetooththird;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    ListView listView;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btArray;

    SendReceive sendReceive;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;

    static int REQUEST_ENABLE_BLUETOOTH = 1;

    private static final String APP_NAME = "BTChat";
    private static final UUID MY_UUID = UUID.fromString("8ce255c0-224a-11e0-ac64-0803450c9a66");
    Context context;
    public ArrayList<String> deviceNames = new ArrayList<>();


    private static final int SAMPLING_RATE_IN_HZ = 44100;

    private static final int REQUEST_BABY = 7;

    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final int BUFFER_SIZE_FACTOR = 2;

    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;

    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);

    private AudioRecord recorder = null;

    private Thread recordingThread = null;

    private Button startButton;

    double difference_calculated = 0;

    boolean delaythemedia = true;

    private Button stopButton;

    String tempCalcResult = "";

    File file2 = new File("/storage/emulated/0/samu.wav");
    File file1 = new File("/storage/emulated/0/recording.pcm");


    double[] result_snipped = SoundDataUtils.load16BitPCMRawDataFileAsDoubleArray(file2);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getString("message") != null) {
                Bundle bun = savedInstanceState;
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                String msg = extras.getString("message");
                sendReceive.write(msg.getBytes());
                finish();
                return;
            }
 /*           else if(extras.getString("getMessage")!= null){
                Intent intent = new Intent();
                intent.putExtra("answer", messageFromBluetooth);
                setResult(RESULT_OK, intent);
                finish();
                return;
            }*/
        }

        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        findViewByIdes();


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btArray = new BluetoothDevice[1];
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            requestDiscover();
        }

        implementListeners();


    }


    public void requestDiscover() {
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
        startActivityForResult(discoverableIntent, 60);
    }

    public void fillDeviceArray(int i) {
        int index = 0;
        BluetoothDevice[] currentList = btArray;
        btArray = new BluetoothDevice[i];
        for (BluetoothDevice currentDevice : currentList) {
            btArray[index] = currentDevice;
            index++;
        }
    }

    private BroadcastReceiver mBroadcastDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            int test = 0;
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null) {

                    for (String name : deviceNames) {
                        if (name.equals(device.getName())) {
                            test = 1;
                        }
                    }
                    if (test == 0) {
                        deviceNames.add(device.getName());
                        fillDeviceArray(deviceNames.size());
                        btArray[deviceNames.size() - 1] = device;
                    }
                }
                ArrayAdapter<String> mBTDevices = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNames);

                listView.setAdapter(mBTDevices);
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                bluetoothAdapter.startDiscovery();
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                switch (resultCode) {
                    case 0:
                        CharSequence text = "Bluetooth aktivieren und App neustarten!";
                        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                        toast.show();
                        finish();
                        return;
                    default:

                        try {
                            requestDiscover();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
                break;
            case 60:
                switch (resultCode) {
                    case 0:
                        CharSequence text = "Sie sind nicht sichtbar!";
                        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                        toast.show();
                        checkPermissions();
                        break;
                    case 60:
                        checkPermissions();
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == 0) {
                    discoverBluetooth();
                }
                break;
            default:
                CharSequence text = "Standort aktivieren und App neustarten!";
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                toast.show();
                finish();
                break;
        }
    }

    private void discoverBluetooth() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();

            bluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastDeviceReceiver, discoverDevicesIntent);
            IntentFilter restartDiscover = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mBroadcastDeviceReceiver, restartDiscover);
        }
        if (!bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastDeviceReceiver, discoverDevicesIntent);
            IntentFilter restartDiscover = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mBroadcastDeviceReceiver, restartDiscover);

        }
        ServerClass serverClass = new ServerClass();

        serverClass.start();

    }

    private void checkPermissions() {
        int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

    }

    private void implementListeners() {

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                ClientClass clientClass = new ClientClass(btArray[i]);
                clientClass.start();
                listView.setAdapter(null);
            }
        });
    }

    Handler deleteList = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            listView.setAdapter(null);
            return true;

        }
    });

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            CharSequence text;
            Toast toast;
            switch (msg.what) {
                case STATE_LISTENING:
                    break;
                case STATE_CONNECTING:
                    break;
                case STATE_CONNECTED:
                    text = "Verbunden";
                    toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                    toast.show();
                    setContentView(R.layout.second_view);
                    impelemntSecondView();
                    break;
                case STATE_CONNECTION_FAILED:
                    text = "Verbindung fehlgeschlagen, versuchen Sie es mit einem Neustart!";
                    toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                    toast.show();

                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuffer = (byte[]) msg.obj;
                    String tempMsg = new String(readBuffer, 0, msg.arg1);
                    sometry(tempMsg);


                    break;
            }
            return true;
        }
    });

    private void findViewByIdes() {
        listView = (ListView) findViewById(R.id.listview);

    }

    private class ServerClass extends Thread {
        private BluetoothServerSocket serverSocket;

        public ServerClass() {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);

            } catch (IOException e) {

            }
        }

        public void run() {
            BluetoothSocket socket = null;

            while (socket == null) {
                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();

                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }
                if (socket != null) {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                    Message delete = Message.obtain();
                    delete.what = STATE_CONNECTED;
                    deleteList.sendMessage(delete);
                    cancel();
                    try {
                        unregisterReceiver(mBroadcastDeviceReceiver);

                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                    bluetoothAdapter.cancelDiscovery();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientClass extends Thread {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device1) {
            device = device1;
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                try {
                    unregisterReceiver(mBroadcastDeviceReceiver);

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
                bluetoothAdapter.cancelDiscovery();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();

                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendReceive extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);

                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
    //


    public void impelemntSecondView() {
        requestAllPermissions();

        startButton = (Button) findViewById(R.id.btnStart);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                startButton.setEnabled(false);
                stopButton.setEnabled(true);

                try {
                    String string = "start";
                    sendReceive.write(string.getBytes());

                } catch (Exception e) {

                }

                startRecording();
            }
        });

        stopButton = (Button) findViewById(R.id.btnStop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                delaythemedia = true;

                startButton.setEnabled(true);
                stopButton.setEnabled(false);


                try {
                    String string = "stop";
                    sendReceive.write(string.getBytes());
                } catch (Exception e) {

                }

                stopRecording();
            }

        });


        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }


    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    int PERMISSION_ALL = 1;

    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO
    };


    public void requestAllPermissions() {
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }

    /*
        @Override
        protected void onResume() {
            super.onResume();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    */
    @Override
    protected void onPause() {
        super.onPause();
        stopRecording();
    }


    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        recorder.startRecording();

        difference_calculated = 0;
        recordingInProgress.set(true);




        recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
        recordingThread.start();


        //1200 zu 200
        if (delaythemedia == true) {
            try {
                recordingThread.sleep(2000);
                final MediaPlayer mo = MediaPlayer.create(this, R.raw.samu);
                mo.start();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        } else {
            try {
                recordingThread.sleep(1000);
                final MediaPlayer mo = MediaPlayer.create(this, R.raw.samu);
                mo.start();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void stopRecording() {
        if (null == recorder) {
            return;
        }

        recordingInProgress.set(false);

        recorder.stop();

        recorder.release();

        recorder = null;

        recordingThread = null;


        detect();


        try {
            recordingThread.sleep(300);

            try {
                String tempMsg = String.valueOf(difference_calculated);
                sendReceive.write(tempMsg.getBytes());

            } catch (Exception e) {
            }


        } catch (Exception e) {
        }
    }


    private double detect() {
        double[] result = SoundDataUtils.load16BitPCMRawDataFileAsDoubleArray(file1);
        difference_calculated = Detect.isit(result, result_snipped);
        return difference_calculated;
    }


    private void distance(double difference_received) {

        while (difference_calculated == 0) {
            distance(difference_received);
        }
        double difference_all = Math.abs(difference_received - difference_calculated);
        //S5 und S8 0.03
        double constantDistance = 0.03;
        double speedOfSound = 343.2;

        double endergebnis = ((difference_all) / 44100) * speedOfSound / 2 + constantDistance;

        final TextView textViewToChange = (TextView) findViewById(R.id.textViewCorr);
        String newtext = "Korrelation fehlgeschlagen";
        if (endergebnis > 15.0) {
            newtext = "Wert liegt über 15 Metern!";
        } else if (endergebnis < 0) {
            newtext = "Negativer Wert!";
        }else{
            newtext = Double.toString(endergebnis);
        }

        textViewToChange.setText(newtext);
    }

    private class RecordingRunnable implements Runnable {

        @Override
        public void run() {

            final File file = new File(Environment.getExternalStorageDirectory(), "recording.pcm");
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            try (final FileOutputStream outStream = new FileOutputStream(file)) {
                while (recordingInProgress.get()) {
                    int result = recorder.read(buffer, BUFFER_SIZE);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio buffer failed: " +
                                getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                }
            } catch (IOException e) {
                throw new RuntimeException("Writing of recorded audio failed", e);
            }
        }
        /*

            //final File file = new File(Environment.getExternalStorageDirectory(), "recording.pcm");
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);


            //try (final FileOutputStream outStream = new FileOutputStream(file)) {


            while (recordingInProgress.get()) {
                int result = recorder.read(buffer, BUFFER_SIZE);
                if (result < 0) {
                    throw new RuntimeException("Reading of audio buffer failed: " +
                            getBufferReadFailureReason(result));
                }
                //dooooooooomed
                //outStream.write(buffer.array(), 0, BUFFER_SIZE);
                //buffer.array().get(bytes,0,BUFFER_SIZE);
                baos.write(buffer.array(), 0, BUFFER_SIZE);
                //byte_size++;
                buffer.clear();
            }


            //} catch (IOException e) {
            //throw new RuntimeException("Writing of recorded audio failed", e);
            //}
        }*/

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }

    private void sometry(String temp) {


        switch (temp) {
            case "start":
                delaythemedia = false;

                startButton.setEnabled(false);
                stopButton.setEnabled(true);

                startRecording();
                break;
            case "stop":
                delaythemedia = true;

                startButton.setEnabled(true);
                stopButton.setEnabled(false);

                stopRecording();
                break;
            case "calc":
                break;
            default:
                double difference = Double.parseDouble(temp);
                distance(difference);
                break;
        }


    }


}