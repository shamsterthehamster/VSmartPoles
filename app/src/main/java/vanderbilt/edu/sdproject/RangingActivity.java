package vanderbilt.edu.sdproject;

import android.graphics.Color;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Switch;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;


public class RangingActivity extends ActionBarActivity implements BeaconConsumer, RangeNotifier {

    private BeaconManager mBeaconManager;

    // pole1 and pole2 are buffers to keep track of the last 5 seconds worth of data from the beacon
    // a cell in the array will be set to true if it was found that second and false if it was not
    // this way any noise can be eliminated from the signal. i keeps track of the index in the arrays
    boolean[] pole1 = new boolean[5];
    boolean[] pole2 = new boolean[5];
    int i = 0;

    // poles_on keeps track of the current status of the poles. This way if the pole is already on and 
    // we detect the bluetooth signal, we will not request to turn it on again. this is also useful for
    // determining when to turn off the poles (current condition for turning off the poles is when all
    // values in the poleX array are false and poles_on[x] is true)
    boolean[] poles_on = new boolean[2];

    // beacon_found stores whether or not the beacon was detected in that 1 second cycle
    boolean[] beacon_found = new boolean[2];

    public static final String dim_light = "#b5b29b";//"#f2ecae";
    public static final String bright_light = "#edd802";

    static String USER_AGENT = "Mozilla/5.0";
    static String TOKEN_URL = "https://www.sic-desigocc.com:8443/api/token";
    static String HEARTBEAT_URL = "https://www.sic-desigocc.com:8443/api/Heartbeat?api_key=DefaultAdmin%3ADefaultAdmin";
    static String CONTENT_TYPE = "application/x-www-form-urlencoded";
    static String HOST = "sspct2540";

    String token = "";

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBeaconServiceConnect() {
        Region region = new Region("all-beacons-region", null, null, null);
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.setRangeNotifier(this);
    }
 
    // didRangeBeaconsInRegion is a callback function that is triggered every second by
    // the android eddystone api we used. Each second it loops through an array that stores
    // information about each (if any) beacon that was found that second
    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        beacon_found[0] = false;
        beacon_found[1] = false;
        for (Beacon beacon: beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                // 1.5 meters is the current range we set for beacon detection. to make the range
                // of detection wider, simply change this number
                if(beacon.getDistance() < 1.5) { 
                    if(instanceId.toHexString().equals("0x000000000001")) {
                        light_up(1);
                        beacon_found[0] = true;
                        pole1[i] = true;
                    }
                    if(instanceId.toHexString().equals("0x000000000002")) {
                        light_up(2);
                        beacon_found[1] = true;
                        pole2[i] = true;
                    }
                }
                Log.d("RangingActivity", "I see a beacon transmitting namespace id: " + namespaceId +
                        " and instance id: " + instanceId +
                        " approximately " + beacon.getDistance() + " meters away.");
            }
        }
        if(!beacon_found[0]) {
            pole1[i] = false;
        }
        if(!beacon_found[1]) {
            pole2[i] = false;
        }
        i = (i+1)%5;
        // if(!beacon_found[0] && !beacon_found[1]) { //this is for debugging purposes
        //     Log.d("no beacon found", "NO beacons found");
        //     Log.d("no beacon", "pole 2 state: " + poles_on[1]);
        // }

        // turn the light pole dimmer if the beacon was not found this cycle and the pole is currently on
        // and the buffer of the past 5 seconds is all false
        if(!beacon_found[0] && poles_on[0] && allFalse(pole1)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView) RangingActivity.this.findViewById(R.id.light_1)).setTextColor(Color.parseColor(dim_light));
                }
            });
            poles_on[0] = false;
        }
        if(!beacon_found[1] && poles_on[1] && allFalse(pole2)) {
            Log.d("turn off", "beacon 2 should turn OFF");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView) RangingActivity.this.findViewById(R.id.light_2)).setTextColor(Color.parseColor(dim_light));
                }
            });
            poles_on[1] = false;
        }
    }

    boolean allFalse(boolean[] pole) {
        for(boolean b : pole) if(b) return false;
        return true;
    }


    // this function lights up the light poles if their bluetooth signal was detected and the light is not already on
    public void light_up(int pole) {

            //change text color
            Log.d("light up", "got here!!");
            if(pole == 1 && !poles_on[0]) {
                //TODO put api calls here. This is where the siemens api should be called to light up light poles
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) RangingActivity.this.findViewById(R.id.light_1)).setTextColor(Color.parseColor(bright_light));
                    }
                });
                poles_on[0] = true;
            }
            else if (pole == 2 && !poles_on[1]){
                Log.d("turn on", "beacon 2 should turn ON");
                //TODO put api calls here. This is where the siemens api should be called to light up light poles
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) RangingActivity.this.findViewById(R.id.light_2)).setTextColor(Color.parseColor(bright_light));
                    }
                });
                poles_on[1] = true;
            }
    }

    @Override
    public void onPause() {
        super.onPause();
        //mBeaconManager.unbind(this);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mBeaconManager.unbind(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging);
        for(int i = 0; i < 5; i++) {
            pole1[i] = false;
            pole2[i] = false;
        }

        // when the feature switch is checked to be on, initialize the bluetooth manager
        Switch bluetoothSwitch = (Switch) findViewById(R.id.bluetoothSwitch);
        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) { // switch = on
                    poles_on[0] = false;
                    poles_on[1] = false;
                    mBeaconManager = BeaconManager.getInstanceForApplication(RangingActivity.this.getApplicationContext());
                    // Detect the main Eddystone-UID frame:
                    mBeaconManager.getBeaconParsers().add(new BeaconParser().
                            setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));
                    mBeaconManager.bind(RangingActivity.this);
                }
                else { // switch = off
                    mBeaconManager.unbind(RangingActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) RangingActivity.this.findViewById(R.id.light_1)).setTextColor(Color.parseColor(dim_light));
                        }
                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView) RangingActivity.this.findViewById(R.id.light_2)).setTextColor(Color.parseColor(dim_light));
                        }
                    });
                    poles_on[0] = false;
                    poles_on[1] = false;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ranging, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    //STEP ONE: connect to server and request access token (returns full string)
    //return string must be parsed for token "access_token": "..."
    public String get_token() throws IOException {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(APPLICATION_FORM_URLENCODED_TYPE, "grant_type=password&username=DefaultAdmin&password=DefaultAdmin");

        Request request = new Request.Builder()
        .url("https://www.sic-desigocc.com:8443/api/token")
        .post(body)
        .addHeader("Content-Type", "application/x-www-form-urlencoded")
        .addHeader("Host", "sspct2540")
        .addHeader("Cache-Control", "no-cache")
        .addHeader("Postman-Token", "6868035e-4a27-40d9-b600-3a657a432264")
        .build();

        Response response = client.newCall(request).execute();
        return response.toString();
    }

    //STEP TWO: Heartbeat connects by giving access code maintained in Step One.
    //Should return a 200 Status Code
    public void heartbeat(String access_code) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
        .url("https://www.sic-desigocc.com:8443/api/Heartbeat?api_key=DefaultAdmin%3ADefaultAdmin")
        .post(null)
        .addHeader("Content-Type", "application/x-www-form-urlencoded")
        .addHeader("Host", "sspct2540")
        .addHeader("Authorization", "Bearer " + access_code)
        .addHeader("Cache-Control", "no-cache")
        .addHeader("Postman-Token", "a413c251-4e81-4441-94f4-2cf7bcd23fd3")
        .build();

        Response response = client.newCall(request).execute(); //200 means it's working
    }

    //STEP THREE: Turn light pole on (corresponding off method below) 
    public void lightpole_on(String ObjectId, String access_code) throws IOException {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, 
            "[\n\t{\n\t\t\"Name\" : \"Value\", \n\t\t\"DataType\" : \"ExtendedEnum\", \n\t\t\"Value\" : \" 8\"\n\t}\n\t\n]");
        Request request = new Request.Builder()
        .url("https://www.sic-desigocc.com:8443/api/commands/" + ObjectId + 
            ".Present_Value/Write?api_key=DefaultAdmin%3ADefaultAdmin")
        .post(body)
        .addHeader("Content-Type", "application/json")
        .addHeader("Host", "sspct2540")
        .addHeader("Accept", "application/json")
        .addHeader("Authorization", "Bearer " + access_code)
        .addHeader("Cache-Control", "no-cache")
        .addHeader("Postman-Token", "0b738c7c-e599-4773-b3d4-a9e227e76544")
        .build();

        Response response = client.newCall(request).execute();
    }

    //STEP FOUR: Turn light pole off (can adjust value in body, replace '0' with 1, 2, or 3 to dim)
    public void lightpole_off(String ObjectId, String access_code) throws IOException {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, 
            "[\n\t{\n\t\t\"Name\" : \"Value\", \n\t\t\"DataType\" : \"ExtendedEnum\", \n\t\t\"Value\" : \" 0\"\n\t}\n\t\n]");
        Request request = new Request.Builder()
        .url("https://www.sic-desigocc.com:8443/api/commands/" + ObjectId + 
            ".Present_Value/Write?api_key=DefaultAdmin%3ADefaultAdmin")
        .post(body)
        .addHeader("Content-Type", "application/json")
        .addHeader("Host", "sspct2540")
        .addHeader("Accept", "application/json")
        .addHeader("Authorization", "Bearer " + access_code)
        .addHeader("Cache-Control", "no-cache")
        .addHeader("Postman-Token", "0b738c7c-e599-4773-b3d4-a9e227e76544")
        .build();

        Response response = client.newCall(request).execute();
    }*/
}
