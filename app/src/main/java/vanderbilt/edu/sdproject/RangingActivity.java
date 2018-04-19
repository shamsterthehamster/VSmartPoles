package vanderbilt.edu.sdproject;

import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Switch;

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
        for (Beacon beacon: beacons) {
            if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                // This is a Eddystone-UID frame
                Identifier namespaceId = beacon.getId1();
                Identifier instanceId = beacon.getId2();
                //TODO put api calls here
                Log.d("RangingActivity", "I see a beacon transmitting namespace id: " + namespaceId +
                        " and instance id: " + instanceId +
                        " approximately " + beacon.getDistance() + " meters away.");
                runOnUiThread(new Runnable() {
                    public void run() {
                        ((TextView)RangingActivity.this.findViewById(R.id.message)).setText("BLE Beacon is switch on, V Smart Poles!");
                    }
                });
            }
        }
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
        //below is the code that is currently breaking the app
        try {
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

        }

        Switch bluetoothSwitch = (Switch) findViewById(R.id.bluetoothSwitch);
        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) { // switch = on
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
        OutputStream os = con.getOutputStream();
        byte [] BODY = body.getBytes("UTF-8");
        os.write(BODY);
        os.flush();
        os.close();

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
