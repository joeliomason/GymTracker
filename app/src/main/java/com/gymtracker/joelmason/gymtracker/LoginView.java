package com.gymtracker.joelmason.gymtracker;

/**
 * Created by JoelioMason on 21/02/15.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginView extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener
{
    Button btnSignIn;
    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;
    private SignInButton mSignInButton;

    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mFirebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        android.app.ActionBar actionBar = LoginView.this.getActionBar();

        mSignInButton = findViewById(R.id.login_button);

        mSignInButton.setOnClickListener(this);

        if (actionBar != null) {
            actionBar.hide();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance();

        // create an instance of SQLite Database
        //loginDatabaseAdapter =new LoginDatabaseAdapter(this);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_button:
                signIn();
                break;
            default:
                return;
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed
                Log.e(TAG, "Google Sign In failed.");
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(LoginView.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            //startActivity(new Intent(LoginView.this, MainMenuActivity.class));
                            Toast.makeText(LoginView.this, "Congrats: Login Successful", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    /**This is what the old LoginView used to do, it was connected to a local database which was
     * completely wrong. I am now using FireBase to handle the database and authentication.
     * Hopefully this works....

    // Methods to handleClick Event of Sign In Button
    public void signIn2()
    {

        // get the References of views
        final  EditText editTextUserName=(EditText) findViewById(R.id.emailAddress);
        final  EditText editTextPassword=(EditText) findViewById(R.id.passwordText);
        //reset errors
        editTextUserName.setError(null);
        editTextPassword.setError(null);
        boolean cancel = false;
        View focusView = null;
        // get The User name and Password
        String email=editTextUserName.getText().toString();
        String password=editTextPassword.getText().toString();

        // fetch the Password form database for respective user name
        String storedPassword= loginDatabaseAdapter.getPassword(email);

        // Check for a valid password or if the user entered one.
        if(TextUtils.isEmpty(password)) {
            editTextUserName.setError("This field is required");
            focusView = editTextUserName;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            editTextPassword.setError("This password is too short");
            focusView = editTextPassword;
            cancel = true;
        }

        // Check for a valid email address or if the user entered one.
        if (TextUtils.isEmpty(email)) {
            editTextUserName.setError("This field is required");
            focusView = editTextUserName;
            cancel = true;
            Toast.makeText(LoginView.this, "password empty", Toast.LENGTH_LONG).show();
        } else if (!isEmailValid(email)) {
            editTextUserName.setError("This email doesn't exist");
            focusView = editTextUserName;
            cancel = true;

        }
        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(password)) {
            editTextUserName.setError("This field is required");
            editTextPassword.setError("This field is required");
            focusView = editTextUserName;
            cancel = true;

        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        }

        // check if the Stored password matches with  Password entered by user
        if(password.equals(storedPassword) && !cancel)
        {
            Toast.makeText(LoginView.this, "Congrats: Login Successful", Toast.LENGTH_LONG).show();
            Intent create = new Intent(LoginView.this, MainMenuView2.class);
            startActivity(create);
        }
        else
        {
            editTextPassword.setError("This password is incorrect");
            editTextPassword.requestFocus();
            Toast.makeText(LoginView.this, "doesnt match " + storedPassword, Toast.LENGTH_LONG).show();
        }

    }

    private boolean isEmailValid(String email) {
        return email.length() > 4;
    }

    private boolean isPasswordValid(String password) {
        //need to add numbers too
        return password.length() > 4;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close The Database
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    **/
}
