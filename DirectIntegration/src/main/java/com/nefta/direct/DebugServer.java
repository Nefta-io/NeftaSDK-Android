package com.nefta.direct;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.nefta.sdk.NAd;
import com.nefta.sdk.NBanner;
import com.nefta.sdk.NInterstitial;
import com.nefta.sdk.NRewarded;
import com.nefta.sdk.NeftaEvents;
import com.nefta.sdk.NeftaPlugin;
import com.nefta.sdk.Placement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class DebugServer {
    private final String TAG = "DS";
    private final int _broadcastPort = 12010;

    private Activity _activity;
    private String _name;
    private String _version;
    private final Handler _mainHandler;
    private final HandlerThread _backgroundThread;
    private final Handler _backgroundHandler;
    private Thread _broadcastThread;
    private InetAddress _broadcastAddress;
    private DatagramSocket _broadcastSocket;
    private Runnable _broadcastRunnable;
    private final List<String> _logLines;

    public DebugServer(Activity activity) {
        _activity = activity;
        _mainHandler = new Handler(Looper.getMainLooper());

        _backgroundThread = new HandlerThread("NetworkThread");
        _backgroundThread.start();
        _backgroundHandler = new Handler(_backgroundThread.getLooper());

        _name = Build.MANUFACTURER + " " + Build.MODEL;

        int buildNumber = 0;
        try {
            PackageManager pm = _activity.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(_activity.getPackageName(), 0);
            buildNumber = packageInfo.versionCode;
        } catch (Exception ignored) {

        }

        _version = NeftaPlugin._instance._info._bundleVersion + "." + buildNumber;
        _logLines = new ArrayList<>();

        NeftaPlugin.OnLog = (String log) -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            LocalDateTime now = LocalDateTime.now();
            _logLines.add(now.format(formatter) +" "+ log);
        };

        startBroadcastServer();
    }

    public void Destroy() {
        StopBroadcastServer();
        _activity = null;
    }

    private void startBroadcastServer() {
        _broadcastRunnable = new Runnable() {
            @Override
            public void run() {
                SendState(_broadcastAddress, _broadcastPort, "master");
                _backgroundHandler.postDelayed(this, 5000);
            }
        };

        try {
            _broadcastSocket = new DatagramSocket();
            _broadcastSocket.setBroadcast(true);
            String ip = GetBroadcastIp();
            _broadcastAddress = InetAddress.getByName(ip);
            Log.i(TAG, "Broadcast server started: " + ip +":"+ _broadcastSocket.getLocalPort());
            _backgroundHandler.post(_broadcastRunnable);
        } catch (Exception e) {
            Log.i(TAG, "Exc: " + e.getMessage());
        }

        _broadcastThread = new Thread(() -> {
            byte[] buffer = new byte[10240];

            while (_broadcastThread != null) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    _broadcastSocket.receive(packet);
                } catch (SocketException e) {
                    Log.i(TAG, "Socket Exc: "+ e.getMessage());
                    return;
                } catch (Exception e) {
                    Log.i(TAG, "Exc: "+ e.getMessage());
                }
                String message = new String(packet.getData(), 0, packet.getLength());

                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                Log.i(TAG, "Received broadcast: "+ message);

                String[] segments = message.split("\\|");
                String sourceName = segments[0];
                if (_name.equals(sourceName)) {
                    continue;
                }
                String control = segments[3];
                String pId;
                String aId;
                int aIdH;
                switch (control) {
                    case "get_logs":
                        int line = Integer.parseInt(segments[4]);
                        for (int i = line; i < _logLines.size(); i++) {
                            SendUdp(address, port, sourceName, "log|"+ i +"|"+ _logLines.get(i));
                        }
                        break;
                    case "set_time_offset":
                        String offsetString = segments[4];
                        NeftaPlugin.SetDebugTime(Long.parseLong(offsetString));
                        SendUdp(address, port, sourceName, "return|set_time_offset");
                        break;
                    case "state":
                        SendState(address, port, sourceName);
                        break;
                    case "create":
                        pId = segments[4];
                        aId = null;
                        for (Map.Entry<String, Placement> p : NeftaPlugin._instance._placements.entrySet()) {
                            if (p.getKey().equals(pId)) {
                                Placement placement = p.getValue();
                                if (placement._type == Placement.Types.Banner) {
                                    NBanner banner = new NBanner(pId, NBanner.Position.TOP);
                                    aId = Integer.toString(banner.hashCode());
                                } else if (placement._type == Placement.Types.Interstitial) {
                                    NInterstitial interstitial = new NInterstitial(pId);
                                    aId = Integer.toString(interstitial.hashCode());
                                } else if (placement._type == Placement.Types.Rewarded) {
                                    NRewarded rewarded = new NRewarded(pId);
                                    aId = Integer.toString(rewarded.hashCode());
                                }
                            }
                        }
                        SendUdp(address, port, sourceName, "return|create|" + aId);
                        break;
                    case "partial_bid":
                        aId = segments[4];
                        aIdH = Integer.parseInt(aId);
                        String bidResponse = null;
                        for (NAd ad : NeftaPlugin._instance._ads) {
                            if (ad.hashCode() == aIdH) {
                                bidResponse = ad.GetPartialBidRequest().toString();
                                break;
                            }
                        }
                        SendUdp(address, port, sourceName, "return|partial_bid|" + aId + "|" + bidResponse);
                        break;
                    case "bid":
                        aId = segments[4];
                        aIdH = Integer.parseInt(aId);
                        for (NAd ad : NeftaPlugin._instance._ads) {
                            if (ad.hashCode() == aIdH) {
                                ad.Bid();
                            }
                        }
                        SendUdp(address, port, sourceName, "return|bid " + aId);
                        break;
                    case "custom_load":
                        aId = segments[4];
                        aIdH = Integer.parseInt(aId);
                        String bid = segments[5];
                        for (NAd ad : NeftaPlugin._instance._ads) {
                            if (ad.hashCode() == aIdH) {
                                ad.LoadWithBidResponse(bid);
                            }
                        }
                        SendUdp(address, port, sourceName,"return|custom_load|" + aId);
                        break;
                    case "load":
                        aId = segments[4];
                        aIdH = Integer.parseInt(aId);
                        for (NAd ad : NeftaPlugin._instance._ads) {
                            if (ad.hashCode() == aIdH) {
                                ad.Load();
                            }
                        }
                        SendUdp(address, port, sourceName, "return|load " + aId);
                        break;
                    case "show":
                        aId = segments[4];
                        aIdH = Integer.parseInt(aId);
                        for (NAd ad : NeftaPlugin._instance._ads) {
                            if (ad.hashCode() == aIdH) {
                                _mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ad.Show(_activity);
                                    }
                                });
                                break;
                            }
                        }
                        SendUdp(address, port, sourceName, "return|show|" + aId);
                        break;
                    case "close":
                        aId = segments[4];
                        aIdH = Integer.parseInt(aId);
                        for (NAd ad : NeftaPlugin._instance._ads) {
                            if (ad.hashCode() == aIdH) {
                                _mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ad.Close();
                                    }
                                });
                                break;
                            }
                        }
                        SendUdp(address, port, sourceName, "return|show|" + aId);
                        break;
                    case "add_event": {
                        String name = null;
                        long value = 0;
                        String customPayload = null;

                        if ("progression".equals(segments[4])) {
                            NeftaEvents.ProgressionStatus status = ToProgressionStatus(segments[5]);
                            NeftaEvents.ProgressionType type = ToProgressionType(segments[6]);
                            NeftaEvents.ProgressionSource source = ToProgressionSource(segments[7]);
                            if (segments.length > 8) {
                                name = segments[8];
                            }
                            if (segments.length > 9) {
                                value = Long.parseLong(segments[9]);
                            }
                            if (segments.length > 10) {
                                customPayload = segments[10];
                            }
                            NeftaPlugin.Events.AddProgressionEvent(status, type, source, name, value, customPayload);
                        } else if ("receive".equals(segments[4])) {
                            NeftaEvents.ResourceCategory category = ToResourceCategory(segments[5]);
                            NeftaEvents.ReceiveMethod method = ToReceiveMethod(segments[6]);
                            if (segments.length > 7) {
                                name = segments[7];
                            }
                            if (segments.length > 8) {
                                value = Long.parseLong(segments[8]);
                            }
                            if (segments.length > 9) {
                                customPayload = segments[9];
                            }
                            NeftaPlugin.Events.AddReceiveEvent(category, method, name, value, customPayload);
                        } else if ("spend".equals(segments[4])) {
                            NeftaEvents.ResourceCategory category = ToResourceCategory(segments[5]);
                            NeftaEvents.SpendMethod method = ToSpendMethod(segments[6]);
                            if (segments.length > 7) {
                                name = segments[7];
                            }
                            if (segments.length > 8) {
                                value = Long.parseLong(segments[8]);
                            }
                            if (segments.length > 9) {
                                customPayload = segments[9];
                            }
                            NeftaPlugin.Events.AddSpendEvent(category, method, name, value, customPayload);
                        } else if ("revenue".equals(segments[4])) {
                            name = segments[5];
                            value = (long) (Double.parseDouble(segments[6]) * 1000000);
                            String currency = segments[7];
                            if (segments.length > 8) {
                                customPayload = segments[8];
                            }
                            NeftaPlugin.Events.AddPurchaseEvent(name, value, currency, customPayload);
                        }
                        SendUdp(address, port, sourceName, "return|add_event");
                        break;
                    }
                    case "add_unity_event": {
                        int type = Integer.parseInt(segments[4]);
                        int category = Integer.parseInt(segments[5]);
                        int subCategory = Integer.parseInt(segments[6]);
                        String name = segments[7];
                        long value = Long.parseLong(segments[8]);
                        String customPayload = null;
                        if (segments.length > 9) {
                            customPayload = segments[9];
                        }
                        NeftaPlugin._instance.Record(type, category, subCategory, name, value, customPayload);
                        SendUdp(address, port, sourceName, "return|add_unity_event");
                        break;
                    }
                    case "add_external_mediation_request": {
                        String provider = segments[4];
                        int type = Integer.parseInt(segments[5]);
                        double requestedFloor = Double.parseDouble(segments[6]);
                        double calculatedFloor = Double.parseDouble(segments[7]);
                        String adUnitId = segments[8];
                        double revenue = Double.parseDouble(segments[9]);
                        String precision = segments[10];
                        int status = Integer.parseInt(segments[11]);
                        NeftaPlugin.OnExternalMediationRequest(provider, type, requestedFloor, calculatedFloor, adUnitId, revenue, precision, status);
                        SendUdp(address, port, sourceName, "return|add_ad_load");
                        break;
                    }
                    case "set_override":
                        String app_id = segments[4];
                        String rest_url = segments[5];
                        if (rest_url.isEmpty() || rest_url.equals("null")) {
                            rest_url = null;
                        }

                        NeftaPlugin._instance._info._appId = app_id;
                        NeftaPlugin.SetOverride(rest_url);
                        NeftaPlugin._instance._placements = null;
                        NeftaPlugin._instance._cachedInitResponse = null;
                        if (segments.length > 6 && segments[6].length() > 0) {
                            NeftaPlugin._instance._state._nuid = segments[6];
                        }

                        SendUdp(address, port, sourceName, "return|set_override");
                        break;
                    case "create_file":
                        String path = segments[4];
                        String content = segments[5];

                        File f = new File(_activity.getApplicationInfo().dataDir, path);
                        try {
                            File parentDir = f.getParentFile();
                            if (parentDir != null && !parentDir.exists()) {
                                if (parentDir.mkdirs()) {
                                    Log.i(TAG, "Parent directories created successfully");
                                }
                            }

                            FileOutputStream outputStream = new FileOutputStream(f);
                            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
                            outputStream.close();

                            Log.i(TAG, "Wrote '"+ content + "' to "+ f.getAbsolutePath());

                            SendUdp(address, port, sourceName, "return|create_file");
                        } catch (Exception exception) {
                            Log.i(TAG, "Error writing to '"+ f.getAbsolutePath() + ": "+ exception.getMessage());
                        }
                        break;
                    default:
                        Log.i(TAG, "Unrecognized command: " + control + " m:" + message);
                        break;
                }
            }

        });
        _broadcastThread.start();
    }

    private void StopBroadcastServer() {
        if (_broadcastRunnable != null) {
            _backgroundHandler.removeCallbacks(_broadcastRunnable);
            _broadcastRunnable = null;
        }

        if (_broadcastThread != null) {
            _broadcastThread.interrupt();
            _broadcastThread = null;
        }

        _broadcastSocket.close();
        _broadcastSocket = null;
    }

    private void SendUdp(InetAddress address, int port, String to, String message) {
        try {
            byte[] buffer = (_name +"|"+ to +"|"+ message).getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            _broadcastSocket.send(packet);
        } catch (Exception e) {
            Log.i(TAG, "Exc: "+ e.getMessage());
        }
    }

    private void SendState(InetAddress address, int port, String to) {
        String stateString = null;
        try {
            JSONObject json = new JSONObject();
            JSONArray adUnits = new JSONArray();
            json.put("app_id", NeftaPlugin._instance._info._appId);
            json.put("rest_url", NeftaPlugin._efUrl);
            json.put("nuid", NeftaPlugin._instance._state._nuid);
            json.put("ad_units", adUnits);

            for (Map.Entry<String, Placement> p : NeftaPlugin._instance._placements.entrySet()) {
                Placement placement = p.getValue();
                JSONObject placementJson = new JSONObject();
                placementJson.put("id", p.getKey());
                placementJson.put("type", Placement.Types.ToString(placement._type));
                adUnits.put(placementJson);

                JSONArray ads = new JSONArray();
                for (NAd ad : NeftaPlugin._instance._ads) {
                    if (ad._placement == placement) {
                        JSONObject adJson = new JSONObject();
                        adJson.put("id", ad.hashCode());
                        adJson.put("state", ad._state);
                        String crid = "";
                        if (ad._bid != null) {
                            crid = ad._bid._creativeId;
                        }
                        adJson.put("crid", crid);
                        ads.put(adJson);
                    }
                }
                placementJson.put("ads", ads);
            }

            stateString = json.toString();
        } catch (Exception ignored) {

        }

        String message = "state|android|"+ NeftaPlugin._instance._info._bundleId +"|"+ _version +"|"+ _logLines.size() +"|"+ stateString;
        SendUdp(address, port, to, message);
    }

    private NeftaEvents.ProgressionStatus ToProgressionStatus(String name) {
        switch (name) {
            case "start":
                return NeftaEvents.ProgressionStatus.Start;
            case "complete":
                return NeftaEvents.ProgressionStatus.Complete;
            default:
                return NeftaEvents.ProgressionStatus.Fail;
        }
    }

    private NeftaEvents.ProgressionType ToProgressionType(String name) {
        switch (name) {
            case "achievement":
                return NeftaEvents.ProgressionType.Achievement;
            case "gameplay_unit":
                return NeftaEvents.ProgressionType.GameplayUnit;
            case "item_level":
                return NeftaEvents.ProgressionType.ItemLevel;
            case "unlock":
                return NeftaEvents.ProgressionType.Unlock;
            case "player_level":
                return NeftaEvents.ProgressionType.PlayerLevel;
            case "task":
                return NeftaEvents.ProgressionType.Task;
            default:
                return NeftaEvents.ProgressionType.Other;
        }
    }

    private NeftaEvents.ProgressionSource ToProgressionSource(String name) {
        for (NeftaEvents.ProgressionSource c : NeftaEvents.ProgressionSource.values()) {
            if (name.equals(c._name)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Non existing ProgressionSource: " + name);
    }

    private NeftaEvents.ResourceCategory ToResourceCategory(String name) {
        for (NeftaEvents.ResourceCategory c : NeftaEvents.ResourceCategory.values()) {
            if (c._name.equals(name)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Non existing ResourceCategory: " + name);
    }

    private NeftaEvents.ReceiveMethod ToReceiveMethod(String name) {
        for (NeftaEvents.ReceiveMethod m : NeftaEvents.ReceiveMethod.values()) {
            if (name.equals(m._name)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Non existing ReceiveMethod: " + name);
    }

    private NeftaEvents.SpendMethod ToSpendMethod(String name) {
        for (NeftaEvents.SpendMethod m : NeftaEvents.SpendMethod.values()) {
            if (name.equals(m._name)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Non existing SpendMethod: " + name);
    }

    private String GetBroadcastIp() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface networkInterface : Collections.list(interfaces)) {
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(addresses)) {
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                        ip = inetAddress.getHostAddress();

                        ip = ip.substring(0, ip.lastIndexOf(".") + 1) + "255";
                    }
                }
            }
        } catch (Exception e) {
            Log.i(TAG, "Error getting ip address: " + e.getMessage());
        }
        return ip;
    }
}
