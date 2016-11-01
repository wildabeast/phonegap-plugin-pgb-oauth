package com.phonegap.build.oauth;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.cordova.*;
import org.json.*;

import android.util.Base64;
import android.util.Log;

public class PhonegapBuildOauth extends CordovaPlugin {
	
	private String CLIENT_ID = "";
	private String CLIENT_SECRET = "";
	private final String HOSTNAME = "https://build.phonegap.com";
	
	CallbackContext cb;
	
	@Override
	public void pluginInitialize() {
		// override CLIENT_ID with value set as plugin parameter
		int id = cordova.getActivity().getResources().getIdentifier("PGB_CLIENT_ID", "string", cordova.getActivity().getPackageName());
		String pgb_client_id = cordova.getActivity().getResources().getString(id);
		if (pgb_client_id != null && !pgb_client_id.isEmpty()) {
			CLIENT_ID = pgb_client_id;
		}

		// override CLIENT_SECRET with value set as plugin parameter
		id = cordova.getActivity().getResources().getIdentifier("PGB_CLIENT_SECRET", "string", cordova.getActivity().getPackageName());
		String pgb_client_secret = cordova.getActivity().getResources().getString(id);
		if (pgb_client_secret != null && !pgb_client_secret.isEmpty()) {
			CLIENT_SECRET = pgb_client_secret;
		}
	}

	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) {

		this.cb = callbackContext;

		if (action.equals("login")) {
			String username, password;
			try {
				username = (String) args.get(0);
				password = (String) args.get(1);
				login(username, password);
			} catch (JSONException e1) {
				fail(e1.getMessage());
				return true;
			}
		}

		if (action.equals("getClientID")) {
			PluginResult r = new PluginResult(PluginResult.Status.OK, CLIENT_ID);
			r.setKeepCallback(false);
			cb.sendPluginResult(r);
		}
		
		if (action.equals("authorizeByCode")) {
			String code;
			try {
				code = (String) args.get(0);
				authorizeByCode(code);
			} catch (JSONException e1) {
				fail(e1.getMessage());
				return true;
			}
		}

		PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
		r.setKeepCallback(true);
		cb.sendPluginResult(r);
		return true;
	}
	
	private void login(String username, String password) {
		String base64EncodedCredentials = "Basic "
				+ Base64.encodeToString(
						(username + ":" + password).getBytes(),
						Base64.NO_WRAP);

		try {
			String json = postData(HOSTNAME + "/token", base64EncodedCredentials, null);
	
			if (json != null) {
				JSONObject obj;
				obj = new JSONObject(json);
				String token = obj.getString("token");
				Log.d("PhonegapBuildOauth", token);
				authorize(token);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	private void fail(String message) {
		PluginResult r = new PluginResult(PluginResult.Status.ERROR, message);
		r.setKeepCallback(false);
		cb.sendPluginResult(r);
	}
	
	private void authorize(String token) {
		HashMap<String, String> hm = new HashMap<String, String>();
		hm.put("client_id", CLIENT_ID);
		hm.put("client_secret", CLIENT_SECRET);
		hm.put("auth_token", token);
		try {
			String json = postData(HOSTNAME + "/authorize", null, hm);
			Log.d("PhonegapBuildOauth", json);
			JSONObject obj = new JSONObject(json);
			PluginResult r = new PluginResult(PluginResult.Status.OK, obj);
			r.setKeepCallback(false);
			cb.sendPluginResult(r);
		}  catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	private void authorizeByCode(String code) {
		HashMap<String, String> hm = new HashMap<String, String>();
		hm.put("client_id", CLIENT_ID);
		hm.put("client_secret", CLIENT_SECRET);
		hm.put("code", code);
		try {
			String json = postData(HOSTNAME + "/authorize/token", null, hm);
			JSONObject obj = new JSONObject(json);
			PluginResult r = new PluginResult(PluginResult.Status.OK, obj);
			r.setKeepCallback(false);
			cb.sendPluginResult(r);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private String postData(String url, String creds, HashMap<String, String> postDataParams) throws IOException {
		
		URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        try {
	        if (creds != null) {
	        	conn.setRequestProperty("Authorization", creds);
	        }
	        conn.setDoOutput(true);
	        conn.setChunkedStreamingMode(0);
	
	        OutputStream out = new BufferedOutputStream(conn.getOutputStream());
            out.write( getPostDataString(postDataParams).getBytes() );
            out.flush();
            out.close();
            
	        int rc = conn.getResponseCode();
            if (rc == HttpURLConnection.HTTP_OK)
            {
                //no http response code er xror
                //read the result from the server
            	StringBuilder total = new StringBuilder();
            	InputStream in = new BufferedInputStream(conn.getInputStream());
            	String line = "";

				// Wrap a BufferedReader around the InputStream
				BufferedReader rd = new BufferedReader(new InputStreamReader(
						in));

				// Read response until the end
				while ((line = rd.readLine()) != null) {
					total.append(line);
				}
				Log.d("PhonegapBuildOauth", total.toString());
				in.close();
				return total.toString();
            }
            else
            {
                fail("http response code error: "+rc+"\n");
            }
        }
        finally {
        	conn.disconnect();
        }
        return null;
	}
	
	private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException{
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

}
