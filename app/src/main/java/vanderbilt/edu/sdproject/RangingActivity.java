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

    //int[] poles = new int[2];
    boolean[] poles_on = new boolean[2];
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

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        beacon_found[0] = false;
        beacon_found[1] = false;
        for (Beacon beacon: beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                //call light_up here
                Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                //TODO put api calls here
                if(beacon.getDistance() < 2) {
                    if(instanceId.toHexString().equals("0x000000000001")) {
                        light_up(1);
                        beacon_found[0] = true;
                    }
                    if(instanceId.toHexString().equals("0x000000000002")) {
                        light_up(2);
                        beacon_found[1] = true;
                    }
                }
                Log.d("RangingActivity", "I see a beacon transmitting namespace id: " + namespaceId +
                        " and instance id: " + instanceId +
                        " approximately " + beacon.getDistance() + " meters away.");
                /*runOnUiThread(new Runnable() {
                    public void run() {
                        ((TextView)RangingActivity.this.findViewById(R.id.message)).setText("BLE Beacon is switch on, V Smart Poles!");
                    }
                });*/
            }
        }
        if(!beacon_found[0] && !beacon_found[1]) {
            Log.d("no beacon found", "NO beacons found");
            Log.d("no beacon", "pole 2 state: " + poles_on[1]);
        }
        //Log.d("no beacon found", "NO beacons found");
        if(!beacon_found[0] && poles_on[0]) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView) RangingActivity.this.findViewById(R.id.light_1)).setTextColor(Color.parseColor(dim_light));
                }
            });
            poles_on[0] = false;
        }
        if(!beacon_found[1] && poles_on[1]) {
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

    public void light_up(int pole) {

        //if(poles[pole] <= 0) {
            //poles[pole] = 0;
            //change text color
            Log.d("light up", "got here!!");
            if(pole == 1 && !poles_on[0]) {
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) RangingActivity.this.findViewById(R.id.light_2)).setTextColor(Color.parseColor(bright_light));
                    }
                });
                poles_on[1] = true;
            }
        //}
        //poles[pole]++;
        /*try {
            Thread.sleep(10000); //10 seconds
        } catch (InterruptedException e) {

        }
        //poles[pole]--;
        if(poles[pole] <= 0) {
            poles[pole] = 0;
            //change text color
            if(pole == 0) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) RangingActivity.this.findViewById(R.id.light_1)).setTextColor(R.style.lights_dim);
                    }
                });
            }
            else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView) RangingActivity.this.findViewById(R.id.light_2)).setTextColor(R.style.lights_dim);
                    }
                });
            }
        }*/
    }

    @Override
    public void onPause() {
        super.onPause();
        mBeaconManager.unbind(this);
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
        //poles[0] = 0;
        //poles[1] = 0;
        //below is the code that is currently breaking the app
        /*try {
            token = get_token();
        }
        catch(IOException e) {
            Log.d("RangingActivity", "get token threw exception");
        }
        runOnUiThread(new Runnable() {
            public void run() {
                ((TextView)RangingActivity.this.findViewById(R.id.debugText)).setText(RangingActivity.this.token);
            }
        });
        try {
            heartbeat(token);
        } catch(IOException e) {

        }*/
        /*ToggleButton test = (ToggleButton) findViewById(R.id.toggleButton);
        test.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    ((TextView)findViewById(R.id.light_1)).setTextColor(Color.parseColor(bright_light));
                }
                else{
                    ((TextView)findViewById(R.id.light_1)).setTextColor(Color.parseColor(dim_light));
                }
            }
        });*/

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

    //STEP ONE: connect to server and request access token (returns full string)
    //return string must be parsed for token "access_token": "..."
    public String get_token() throws IOException {
        String body = "grant_type=password&username=DefaultAdmin&password=DefaultAdmin";

        URL obj = new URL(TOKEN_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Content-Type", CONTENT_TYPE);
        con.setRequestProperty("Host", HOST);

        con.setDoOutput(true);
        OutputStream out = new BufferedOutputStream(con.getOutputStream());
        byte [] BODY = body.getBytes("UTF-8");
        out.write(BODY);
        out.flush();
        out.close();

        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader (new InputStreamReader(con.getInputStream()));
        String input;
        StringBuffer response = new StringBuffer();
        while((input = in.readLine()) != null) {
            response.append(input);
        }
        in.close();
        return input;
    }

    //STEP TWO: Heartbeat connects by giving access code maintained in Step One.
    //Should return a 200 Status Code
    public void heartbeat(String access_code) throws IOException {
        URL obj = new URL(HEARTBEAT_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Content-Type", CONTENT_TYPE);
        con.setRequestProperty("Host", HOST);
        //not sure if Authorization is a header type, need to see how to send access code
        con.setRequestProperty("Authorization", access_code.concat("Bearer ")); //check if legit

        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader (new InputStreamReader(con.getInputStream()));
        String input;
        StringBuffer response = new StringBuffer();
        while((input = in.readLine()) != null) {
            response.append(input);
        }
        in.close();
    }
}
