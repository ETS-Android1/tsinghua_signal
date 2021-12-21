package com.maiji.magkareble40;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class LoginActivity extends Activity implements View.OnClickListener{
    private Button btnSignIn;
    private Button btnSignUp;
    private EditText txtUserName;
    private EditText txtPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initView(){
        btnSignIn = (Button) findViewById(R.id.btnSignIn);
        btnSignIn.setOnClickListener(this);
        btnSignUp = (Button) findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(this);
        txtUserName = (EditText) findViewById(R.id.txtUserName);
        txtPassword = (EditText) findViewById(R.id.txtPassword);
    }

    @Override
    public void onClick(View v){
        String userName = txtUserName.getText().toString();
        String passWord = txtPassword.getText().toString();
        if(userName.equals("")||passWord.equals(""))
        {
            Toast.makeText(getApplicationContext(), "User name and password should not be empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (v.getId()){
            case R.id.btnSignIn:
                sign_in(userName,passWord);
                break;
            case R.id.btnSignUp:
                sign_up(userName,passWord);
                break;
        }
    }

    private void sign_up(String userName, String password) {
        String TAG = "sign_up";
        XBleActivity.mAuth.createUserWithEmailAndPassword(userName, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUser:success");
                            FirebaseUser user = XBleActivity.mAuth.getCurrentUser();
                            updateUI(TAG);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUser:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "SignUp failed.",
                                    Toast.LENGTH_SHORT).show();
//                            updateUI(null);
                        }
                    }
                });
    }

    private void sign_in(String userName, String password){
        String TAG = "sign_in";
        XBleActivity.mAuth.signInWithEmailAndPassword(userName, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signIn:success");
                            FirebaseUser user = XBleActivity.mAuth.getCurrentUser();
                            updateUI(TAG);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signIn:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "SignIn failed.",
                                    Toast.LENGTH_SHORT).show();
//                            updateUI(null);
                        }
                    }
                });
    }

    private void updateUI(String state){
        Intent intent = new Intent();
//        intent.putExtra("user",user);                // 设置结果，并进行传送
        intent.putExtra("user",state);
        this.setResult(2, intent);
        this.finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = XBleActivity.mAuth.getCurrentUser();
        if(currentUser != null){
            reload();
        }
    }

    public  void reload() { }
}
