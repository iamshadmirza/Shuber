package com.msmworks.shuber;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.QuickContactBadge;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CustomerSettingsActivity extends AppCompatActivity {

    private EditText mNameField, mPhoneField;
    private Button mConfirmButton, mBackButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseReference;
    private String userID;
    private String mPhone;
    private String mName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);
        mNameField = (EditText) findViewById(R.id.name_field);
        mPhoneField = (EditText) findViewById(R.id.number_field);
        mConfirmButton = (Button) findViewById(R.id.confirm_button);
        mBackButton = (Button) findViewById(R.id.back_button);
        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users").child("customers").child(userID);

        getUserInfo();

        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInformation();
            }
        });

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });

    }

    private void getUserInfo() {
        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map = (Map<String, Object>)dataSnapshot.getValue();
                    if (map.get("name")!=null) {
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                    }
                    if (map.get("phone")!=null) {
                        mPhone = map.get("phone").toString();
                        mPhoneField.setText(mPhone);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveUserInformation() {
        mName = mNameField.getText().toString();
        mPhone = mPhoneField.getText().toString();
        Map userInfo = new HashMap();
        userInfo.put("name", mName);
        userInfo.put("phone", mPhone);
        mDatabaseReference.updateChildren(userInfo);

    }
}
