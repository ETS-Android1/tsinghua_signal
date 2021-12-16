package com.maiji.magkareble40;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity implements View.OnClickListener{
    private Button btnSignIn;
    private Button btnSignUp;
    private EditText txtUserName;
    private EditText txtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
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
                
                break;
            case R.id.btnSignUp:
                break;
        }
    }
}
