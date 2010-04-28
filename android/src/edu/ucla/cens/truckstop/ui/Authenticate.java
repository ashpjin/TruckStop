package edu.ucla.cens.truckstop.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.mime.content.StringBody;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.util.Log;

import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.client.OAuthClient;
import net.oauth.client.OAuthResponseMessage;
import net.oauth.client.httpclient4.HttpClient4;

import edu.ucla.cens.truckstop.R;
import edu.ucla.cens.truckstop.utils.BHLanguage;
import edu.ucla.cens.truckstop.utils.PreferencesMgr;

public class Authenticate extends Activity implements Runnable {
    private Context ctx;
    private EditText et_email;
    public EditText et_pass;
    private EditText et_pass2;
    private EditText et_user;
    private CheckBox cbSaveLogin;
    private CheckBox cbNewAccount;
    private Button cbClearUser;

    private String email = "";
    private String user = "";
    private String pass1 = "";
    private String pass2 = "";
    private Button submit;
    private boolean saveLogin = false;

    private static final int LOGIN = 0;
    private static final int REGISTER = 1;
    private int auth_type = LOGIN;

	private static final String TAG = "Authentication";
    private static final int DIALOG_PROGRESS = 1;

    private ProgressDialog mProgressDialog;
    private String auth_fail_string = "login";

    // These stubs are used to create the URL that is accessed to obtain this information
    //  via oauth, and are also used to save and access these resources from the preferences
    //  storage.
    private final static String REQUEST_TOKEN_STUB = "request_token";
    public final static String ACCESS_TOKEN_STUB = "access_token";
    private final static String TOKEN_SECRET_STUB = "token_secret";
    private final static String HACK_AUTHORIZATION_STUB = "authorize_access";


    // Hardcoded, part of oAuth algorithm
    private static String CALLBACK_URL = "http://printer.example.com/request_token_ready";
    private static final String CONSUMER_KEY = "GmbUrXeppHhy6hB8";
    private static final String CONSUMER_SECRET = "MRbbdjqQjDGMW2jS";

    private class token_store {
        public String access_token;
        public String token_secret;
        public String request_token;

        token_store() {
            access_token = "";
            token_secret = "";
            request_token = "";
        }
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.authenticate);
        et_email = (EditText) findViewById(R.id.email);
        et_pass = (EditText) findViewById(R.id.password);
        et_pass2 = (EditText) findViewById(R.id.password2);
        et_user = (EditText) findViewById(R.id.user_input);
        cbNewAccount = (CheckBox) findViewById(R.id.cb_new_account);
        cbSaveLogin = (CheckBox) findViewById(R.id.save_login);
        submit = (Button) findViewById(R.id.login);
        cbClearUser = (Button) findViewById(R.id.cb_clear_user);
        ctx = Authenticate.this;


		TextView t = (TextView) findViewById(R.id.version);
		t.setText(ctx.getString(R.string.versionString) + ctx.getString(R.string.version));

        PreferencesMgr.setBoolean(this, PreferencesMgr.AUTHENTICATED, false);
        PreferencesMgr.setBoolean(this, PreferencesMgr.REGISTERED, false);

        user = PreferencesMgr.getString(this, PreferencesMgr.USER);
        pass1 = PreferencesMgr.getString(this, PreferencesMgr.PASSWORD);
        saveLogin = PreferencesMgr.getBoolean(this, PreferencesMgr.SAVE_LOGIN);

        if (!user.equals("")) {
            if (!pass1.equals("") && saveLogin) {
            	et_user.setText(user);
            	et_pass.setText(pass1);
            	cbSaveLogin.setChecked(saveLogin);
            }
        }

        cbNewAccount.setOnClickListener(registerCB);
        cbClearUser.setOnClickListener(clearUserCB);

        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick (View view) {
                auth_type = LOGIN;
                if (cbNewAccount.isChecked()) {
                    auth_type = REGISTER;
                    email = et_email.getText().toString();
                    pass2 = et_pass2.getText().toString();

                    // We should clear the user data at this point because a new
                    //	user is being created.
                    clearUserData();
                }
                user = et_user.getText().toString();
                pass1 = et_pass.getText().toString();
                saveLogin = cbSaveLogin.isChecked();
                PreferencesMgr.setString(ctx, PreferencesMgr.USER, user);
                PreferencesMgr.setBoolean(ctx, PreferencesMgr.SAVE_LOGIN, saveLogin);

                if (saveLogin)
                	PreferencesMgr.setString(ctx, PreferencesMgr.PASSWORD, pass1);
                showDialog (DIALOG_PROGRESS);
                Thread thread = new Thread(Authenticate.this);
                thread.start();
            }
        });
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Make sure registration screen shows up if the user was trying
		//	to register
		if (cbNewAccount.isChecked()) {
			setupRegisterPage(cbNewAccount);
		}
	}

	// REGISTRATION

    OnClickListener registerCB = new View.OnClickListener() {
        public void onClick(View v) {
        	setupRegisterPage(v);
        }
    };

	private void setupRegisterPage(View v) {
        CheckBox cb = (CheckBox) v;
        int state = cb.isChecked() ? 0 : android.view.View.INVISIBLE;

        findViewById(R.id.tv1).setVisibility(state);
        findViewById(R.id.pass2_label).setVisibility(state);

        et_email.setVisibility(state);
        et_pass2.setVisibility(state);
        if (cb.isChecked()) {
            submit.setText("Register");
        } else {
            submit.setText("Login");
        }
	}

    private boolean register_user (String email, String user, String pass1, String pass2) {
        String stored_pass_hash =
        	PreferencesMgr.getString(ctx, PreferencesMgr.PASS_HASHED);
        if (stored_pass_hash.equals("")) {
            if (!pass1.equals(pass2)) {
                Toast.makeText(ctx, "the passwords you entered do not match", Toast.LENGTH_LONG).show();
                return false;
            }

            Log.d(TAG, "registering new account: " + user);
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost request = new HttpPost(getString(R.string.register_user));

            try {
                MultipartEntity entity = new MultipartEntity();
                entity.addPart("username", new StringBody(user));
                entity.addPart("password", new StringBody(pass1));
                entity.addPart("confirmpassword", new StringBody(pass2));
                entity.addPart("email", new StringBody(email));
                request.setEntity(entity);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return false;
            }

            try {
                HttpResponse response = httpClient.execute(request);
                Log.d(TAG, "Doing AppSpot HTTPS Request");
                int status = response.getStatusLine().getStatusCode();
                if (HttpStatus.SC_OK != status) {
                    Log.d(TAG, "got status: " + status);
                    Log.d(TAG, generateString(response.getEntity().getContent()));
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            Toast.makeText(ctx, "User already exists, check 'Clear User Data' first and " +
            		"then re-register", Toast.LENGTH_LONG);
        }
        return false;
    }

    // LOGIN USER

    private token_store get_access_token_hack(String user, String pass) {
        if (null == user || user.equals("")
            || null == pass || pass.equals(""))
        {
            return null;
        }
        token_store ts = new token_store();

        OAuthServiceProvider provider = new OAuthServiceProvider(makeURL(REQUEST_TOKEN_STUB),
		makeURL(HACK_AUTHORIZATION_STUB),
		makeURL(ACCESS_TOKEN_STUB));
        OAuthConsumer consumer = new OAuthConsumer(CALLBACK_URL, CONSUMER_KEY, CONSUMER_SECRET, provider);
        OAuthAccessor accessor = new OAuthAccessor(consumer);
        OAuthClient client = new OAuthClient(new HttpClient4());

        ArrayList<Map.Entry<String, String>> params = new ArrayList<Map.Entry<String, String>>();
        params.add(new OAuth.Parameter("username", user));
        params.add(new OAuth.Parameter("password", sha1sum(pass).toString()));
        params.add(new OAuth.Parameter("oauth_callback", CALLBACK_URL));

        try {
            OAuthMessage message = client.invoke(accessor, null, makeURL(HACK_AUTHORIZATION_STUB), params);
            if (((OAuthResponseMessage)message).getHttpResponse().getStatusCode() != 200) {
                return null;
            }
            ts.access_token = message.getParameter("oauth_token");
            ts.token_secret = message.getParameter("oauth_token_secret");
            ts.request_token = "";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (OAuthException e) {
            e.printStackTrace();
            return null;
        } catch (java.net.URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        return ts;
    }

    /* user : plain text,
     * pass : plaintext password */
    private boolean login_user (String user, String pass) {
        String stored_pass_hash =
        	PreferencesMgr.getString(ctx, PreferencesMgr.PASS_HASHED);

        if (stored_pass_hash.equals("")) {
            /* login using the internet and our oauth consumer stuff */
            token_store tokens = get_access_token_hack(user, pass);
            if (null == tokens) {
                Log.d(TAG, "couldnt get access token properly");
                return false;
            }

            try {
            	if (tokens.access_token.equals(""))
            		{
            			Log.e(TAG, "Major problem, access_token " +
            				"is empty. Will not be able to upload");
            			return false;
            		}
            }

            catch (Exception e) {
            	Log.e(TAG, "Could not read access_token, cannot login user");
            	return false;
            }

            /* if oauth login was successful then add this user to the db */
            PreferencesMgr.setString(ctx, PreferencesMgr.PASS_HASHED, pass);
            PreferencesMgr.setString(ctx, ACCESS_TOKEN_STUB, tokens.access_token);
            PreferencesMgr.setString(ctx, TOKEN_SECRET_STUB, tokens.token_secret);
            PreferencesMgr.setString(ctx, REQUEST_TOKEN_STUB, tokens.request_token);
            return true;
        }

        else if (stored_pass_hash.equals(pass)) return true;
        Log.w(TAG, "login_user(): failed to login");
        return false;
    }

    // CLEAR USER DATA
	private void clearUserData() {
        PreferencesMgr.setString(ctx, PreferencesMgr.USER, "");
        PreferencesMgr.setString(ctx, PreferencesMgr.PASSWORD, "");
        PreferencesMgr.setString(ctx, PreferencesMgr.PASS_HASHED, "");
        PreferencesMgr.setString(ctx, ACCESS_TOKEN_STUB, "");
        PreferencesMgr.setString(ctx, TOKEN_SECRET_STUB, "");
        PreferencesMgr.setString(ctx, REQUEST_TOKEN_STUB, "");

        PreferencesMgr.setString(ctx, PreferencesMgr.LANGUAGE, BHLanguage.ENGLISH);

        PreferencesMgr.setBoolean(ctx, PreferencesMgr.SAVE_LOGIN, false);
        PreferencesMgr.setBoolean(ctx, PreferencesMgr.AUTHENTICATED, false);
        PreferencesMgr.setBoolean(ctx, PreferencesMgr.REGISTERED, false);
        PreferencesMgr.setBoolean(ctx, PreferencesMgr.PATH_RUNNING, false);
        PreferencesMgr.setBoolean(ctx, PreferencesMgr.PATH_RUN_ONCE, false);
	}

    OnClickListener clearUserCB = new View.OnClickListener() {
        public void onClick(View v) {
            et_user.setText("");
            et_pass.setText("");
         	cbSaveLogin.setChecked(false);
         	cbNewAccount.setChecked(false);

         	clearUserData();
        }
    };


    // GENERAL UTILS
    private String makeURL(String URLStub) {
	    return (getString(R.string.baseurl) + URLStub);
    }

    private String sha1sum (String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte hash[] = md.digest(text.getBytes());
            StringBuffer hexString = new StringBuffer("");
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xFF & hash[i]);
                if (1 == hex.length()) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_PROGRESS:
            mProgressDialog = new ProgressDialog(Authenticate.this);
            mProgressDialog.setTitle("Working...");
            mProgressDialog.setMessage("Authenticating User...");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            return mProgressDialog;
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mProgressDialog != null) {
            dismissDialog(DIALOG_PROGRESS);
            mProgressDialog = null;
        }
    }

    // AUTHENTICATION
    public void run() {
        Looper.prepare();
        Message msg = new Message();
        Bundle b = new Bundle();
        PreferencesMgr.setBoolean(ctx, PreferencesMgr.AUTHENTICATED, auth());
        msg.setData(b);

        handler.sendMessage(msg);
        Looper.loop();
    }

    private Handler handler = new Handler() {

    	@Override
        public void handleMessage(Message msg) {
            if (mProgressDialog != null) mProgressDialog.dismiss();
            switch (auth_type) {
                case LOGIN:
                    Log.d(TAG, "handler(): doing login");
                    if (PreferencesMgr.getBoolean(ctx, PreferencesMgr.AUTHENTICATED)) {
                        ctx.startActivity(new Intent(ctx, Home.class));
                        Authenticate.this.finish();
                        return;
                    } else {
                        Log.d(TAG, "handler(): login failed");
                        auth_fail_string = "login";
                    }
                    break;
                case REGISTER:
                    Log.d(TAG, "handler(): doing register");
                    if (PreferencesMgr.getBoolean(ctx, PreferencesMgr.REGISTERED)) {
                        Log.d(TAG, "handler(): registered successfully");
                        ctx.startActivity(new Intent(ctx, Authenticate.class));
                        Authenticate.this.finish();
                        return;
                    } else {
                        Log.d(TAG, "handler(): register failed");
                        auth_fail_string = "register";
                    }
                    break;
                default:
                    Log.d(TAG, "handler(): this msg should never be seen");
                    auth_failed();
            }
            Log.d(TAG, "handler(): auth failed... calling auth_failed()");
            auth_failed();
        }
    };

    private boolean auth() {
        if (cbNewAccount.isChecked()) {
            if (register_user (email, user, pass1, pass2)) {
            	PreferencesMgr.setBoolean(ctx, PreferencesMgr.REGISTERED, true);
                auth_type = REGISTER;
                Log.d(TAG, "auth(): register_user(): successfully created a new account");
                return true;
            }
        } else if (login_user (user, pass1)) {
            auth_type = LOGIN;
            PreferencesMgr.setBoolean(ctx, PreferencesMgr.AUTHENTICATED, true);
            Log.d(TAG, "auth(): login_user(): successfully logged user in");
            return true;
        }

        Log.d(TAG, "auth(): failed to authenticate (create account / login)");
        return false;
    }

    private void auth_failed() {
        Log.d(TAG, "auth was a failure");
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Could not " + auth_fail_string)
            .setMessage("You must enter valid credentials before you can proceed.")
            .setCancelable(false)
            .setPositiveButton("Go back", new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int id) {
                    ctx.startActivity(new Intent(ctx, Authenticate.class));
                    Authenticate.this.finish();
                }
            });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private String generateString(InputStream stream) {
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader buffer = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();

        try {
            String cur;
            while ((cur = buffer.readLine()) != null) {
                sb.append(cur + "\n");
            }
            stream.close();
        } catch (IOException e) {
            Log.e(TAG, "IO Exception when trying to read HTTP return code");
            e.printStackTrace();
        }

        return sb.toString();
    }
}
