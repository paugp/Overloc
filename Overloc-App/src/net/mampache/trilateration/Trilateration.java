package net.mampache.trilateration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

/**
 * Created by Javier López on 7/10/16.
 */
public class Trilateration extends CordovaPlugin {
    private double x = 0.0;
    private double y = 0.0;
    public static ArrayList<AccessPoint> aps = new ArrayList();
    public static Context context;
    public WifiScanner wifiScanner;
    /**
     * Constructor.
     */
    public Trilateration() {
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.context = cordova.getActivity().getApplicationContext();
        wifiScanner = new WifiScanner();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiScanner.broadcastReceiver, intentFilter);
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("actionName".equals(action)) {
            JSONObject r = new JSONObject();
            r.put("data", "esto son datos");
            if(args.length() > 0)
                r.put("param1", args.get(0));
            callbackContext.success(r);
        } else if("setup".equals(action)) {
           this.setup( (JSONArray) args.get(0) );
           callbackContext.success();
        } else if("getPosition".equals(action)) {
          Position pos = this.getPosition();
          JSONObject json = new JSONObject();
          json.put("x",pos.x);
          json.put("y",pos.y);
          callbackContext.success(json);
        } else {
            return false;
        }
        return true;
    }

    public void setup(JSONArray data){
        try {
          for (int i = 0; i < data.length(); i++) {
                  JSONObject ap = data.getJSONObject(i);
                  aps.add(new AccessPoint( ap.getString("bssid"), ap.getDouble("x"), ap.getDouble("y")));
          }
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public Position getPosition(){
      for(AccessPoint ap : Trilateration.aps){
        double sum = 0.0;
        for(double level : ap.bufferDBs){
          sum += level;
        }
        ap.level = (sum / ap.bufferDBs.length);
        ap.distance = Trilateration.calculateDistance(ap.level, ap.frequency);
      }
      //Collections.sort(Trilateration.aps, Collections.reverseOrder());
      AccessPoint ap1 = Trilateration.aps.get(0);
      AccessPoint ap2 = Trilateration.aps.get(1);
      AccessPoint ap3 = Trilateration.aps.get(2);
      return obtainCoordinates(ap1.distance, ap2.distance, ap3.distance, ap1.x, ap1.y, ap2.x, ap2.y, ap3.x, ap3.y);
    }


    public static double calculateDistance(double rssi, double freq) {
        return (Math.pow(10.0d, (27.55d - 40d * Math.log10(freq) + 6.7d - rssi) / 20.0d)) * 1000;
    }

    public static Position obtainCoordinates(double r1, double r2, double r3, double xa, double ya, double xb, double yb, double xc, double yc){
      /* V1
      if(xa < 0.0 || xb < 0.0 || xc < 0.0 || ya < 0.0 || yb < 0.0 || yc < 0.0 || r1 < 0.0 || r2 < 0.0 || r3 < 0.0) throw new Error("Trilateration: negative distance.");
      double d2 = ( Math.pow(xc, 2.0) - Math.pow(xb, 2.0) + Math.pow(yc, 2.0) - Math.pow(yb, 2.0) + Math.pow(r2, 2.0) - Math.pow(r3, 2.0) ) / 2.0;
      double d1 = ( Math.pow(xa, 2.0) - Math.pow(xb, 2.0) + Math.pow(ya, 2.0) - Math.pow(yb, 2.0) + Math.pow(r2, 2.0) - Math.pow(r1, 2.0) ) / 2.0;
      double y = ((d1 * (xb - xc)) - (d2 * (xb - xa))) / (((ya - yb) * (xb - xc)) - ((yc - yb) * (xb - xa)));
      double x = ((y * (ya - yb)) - d1) / (xb - xa);
        /* V2
        xa = 0.0;
        ya = 0.0;
        yb = 0.0;
        double x = ((r1*r1) - (r2*r2) - (xb*xb))/(-2*xb);
        double y = ((r1*r1) - (r3*r3) - (2 * xc * x) + (xc*xc) + (yc *yc)) / (2*yc);

        */
        /* V3*/
        double va = (double)((r2 * r2 - r3 * r3) - (xb * xb - xc * xc) - (yb * yb - yc * yc)) / 2.0;
        double vb = (double)((r2 * r2 - r1 * r1) - (xb * xb - xa * xa) - (yb * yb - ya * ya)) / 2.0;

        double y = (vb * (xc - xb) - va * (xa - xb)) / ((ya - yb) * (xc - xb) - (yc - yb) * (xa - xb));
        double x;
        if((xc - xb)!=0){
            x = (va - y * (yc - yb)) / (xc - xb);
        }
        else{
            x = (vb - y * (ya - yb)) / (xa - xb);
        }

        return new Position(x, y);
    }

}

class Position{
    public double x;
    public double y;
    public Position(double x, double y){
        this.x = x;
        this.y = y;
    }
}

class AccessPoint implements Comparable<AccessPoint>{
    public String bssid;
    public double x;
    public double y;
    public double frequency;
    public double[] bufferDBs;
    public double level;
    public double distance;

    public AccessPoint(String bssid, double x, double y){
      this.bssid = bssid;
      this.x = x;
      this.y = y;
        bufferDBs = new double[5];
      for (int i = 0; i < 5 ; i++) {
        this.bufferDBs[i] = 1;
      }
    }

    public void setFrequency(double fr){
      this.frequency = fr;
    }

    public void setLevel(double level){
      for (int i = 0; i < 4 ; i++) {
        this.bufferDBs[i] = this.bufferDBs[i+1];
      }
      this.bufferDBs[4] = level;
    }

    @Override
    public int compareTo(AccessPoint ap){

      return new Double(this.level).compareTo(ap.level);
    }

  }

class WifiScanner {

  public WifiScanner(){
  }

  private AccessPoint findAccessPointFromVector(String bssid){
    for(AccessPoint ap : Trilateration.aps){
      if(ap.bssid.equals(bssid)) return ap;
    }
    return null;
  }

  public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
          WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
          List<ScanResult> scanResults = wifiManager.getScanResults();
          for(ScanResult scan : scanResults){
              AccessPoint ap = findAccessPointFromVector(scan.BSSID);
              if(ap != null){
                  ap.setLevel(scan.level);
                  ap.setFrequency(scan.frequency);
              }
          }
          wifiManager.startScan();
      }
  };
}
