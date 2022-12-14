package com.example.firestoreexample;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.util.Arrays;
import java.util.List;

public class Login extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123;
    //RC_SIGN in is the request code
    // you will assign for starting the new activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // Si el usuario ya esta logueado pasar a la actividad principal
            Intent homeIntent = new Intent(this, MainActivity.class);
            startActivity(homeIntent);
        } else {
            // Si no mostrar el FirebaseUI
            showFirebaseUI();
        }

    }

    public void showFirebaseUI() {


        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.AnonymousBuilder().build());
        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false) //Desactiviar incio automatico
                .build(), RC_SIGN_IN);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Intent homeIntent = new Intent(this, MainActivity.class);
                startActivity(homeIntent);

            } else {
                finish();
            }
        }
    }


}