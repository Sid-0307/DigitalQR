package com.example.dqr;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ForgotPasswordActivity extends AppCompatActivity {
    private Button btn_reset;

    private EditText form_email;

    private TextView tv_login,resetText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        mAuth = FirebaseAuth.getInstance();
        form_email = findViewById(R.id.form_email);
        resetText = findViewById(R.id.resetText);
        tv_login = findViewById(R.id.tv_login);
        btn_reset = findViewById(R.id.btn_reset);

        tv_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ForgotPasswordActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = form_email.getText().toString().trim();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users");

                databaseReference.orderByChild("email").equalTo(email)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    mAuth.sendPasswordResetEmail(email)
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    resetText.setVisibility(View.VISIBLE);
                                                } else {
                                                    Toast.makeText(ForgotPasswordActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                } else {
                                    Toast.makeText(getApplicationContext(), "No user found with this email.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(getApplicationContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
}
