package com.msmworks.shuber;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerLoginActivity extends AppCompatActivity {
    private EditText mEnterPassword;
    private EditText mEnterEmail;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener FirebaseAuthListener;
    private CardView mLoginButton;
    private CardView mRegisterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login_activity);
        mEnterEmail = (EditText) findViewById(R.id.enter_email);
        mEnterPassword = (EditText) findViewById(R.id.enter_password);
        mLoginButton = (CardView) findViewById(R.id.login_button);
        mRegisterButton = (CardView) findViewById(R.id.register_button);
        mAuth = FirebaseAuth.getInstance();

        FirebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = mAuth.getCurrentUser();
                if(user!=null){
                    Intent intent =  new Intent(CustomerLoginActivity.this, CustomerMapActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEnterEmail.getText().toString();
                final String password = mEnterPassword.getText().toString();
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(CustomerLoginActivity.this, "Sign up failed!", Toast.LENGTH_SHORT).show();
                        }else{
                            String user_id = mAuth.getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("users").child("customers").child(user_id);
                            current_user_db.setValue(true);
                            startActivity(new Intent(CustomerLoginActivity.this, DriverMapActivity.class));
                        }
                    }
                });

            }
        });

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEnterEmail.getText().toString();
                final String password = mEnterPassword.getText().toString();
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(CustomerLoginActivity.this, "Login failed!", Toast.LENGTH_SHORT).show();
                        }else{
                            startActivity(new Intent(CustomerLoginActivity.this, DriverMapActivity.class));
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(FirebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(FirebaseAuthListener);
    }
}
