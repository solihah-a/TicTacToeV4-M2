package clarkson.ee408.tictactoev4;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import clarkson.ee408.tictactoev4.client.*;
import clarkson.ee408.tictactoev4.model.*;
import clarkson.ee408.tictactoev4.socket.*;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameField;
    private EditText passwordField;
    private EditText confirmPasswordField;
    private EditText displayNameField;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Getting Inputs
        Button registerButton = findViewById(R.id.buttonRegister);
        Button loginButton = findViewById(R.id.buttonLogin);
        usernameField = findViewById(R.id.editTextUsername);
        passwordField = findViewById(R.id.editTextPassword);
        confirmPasswordField = findViewById(R.id.editTextConfirmPassword);
        displayNameField = findViewById(R.id.editTextDisplayName);

        gson = new GsonBuilder().serializeNulls().create();

        //Adding Handlers
        //et an onclick listener to registerButton to call handleRegister()
        registerButton.setOnClickListener(v -> handleRegister());

        // set an onclick listener to loginButton to call goBackLogin()
        loginButton.setOnClickListener(v -> goBackLogin());
    }

    /**
     * Process registration input and pass it to {@link #submitRegistration(User)}
     */
    public void handleRegister() {
        //Declare local variables for username, password, confirmPassword and displayName. Initialize their values with their corresponding EditText
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();
        String confirmPassword = confirmPasswordField.getText().toString();
        String displayName = displayNameField.getText().toString();

        //Verify that all fields are not empty before proceeding. Toast with the error message
        if (username.isBlank() || password.isBlank() || confirmPassword.isBlank() || displayName.isBlank()){
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        //Verify that password is the same af confirm password. Toast with the error message
        if (!password.equals(confirmPassword)){
            Toast.makeText(this, "Passwords do not match. Please try again", Toast.LENGTH_SHORT).show();
            return;
        }

        //Create User object with username, display name and password and call submitRegistration()
        User user = new User(username, password, displayName, false);
        submitRegistration(user);
    }

    /**
     * Sends REGISTER request to the server
     * @param user the User to register
     */
    void submitRegistration(User user) {
        //Send a REGISTER request to the server, if SUCCESS reponse, call goBackLogin(). Else, Toast the error message
        AppExecutors.getInstance().networkIO().execute(()  -> {
            try {
                String userJson = gson.toJson(user);

                Request request = new Request(Request.RequestType.REGISTER, userJson);

                Response response = SocketClient.getInstance().sendRequest(request, Response.class);

                if (response != null && response.getStatus() == Response.ResponseStatus.SUCCESS) {
                    AppExecutors.getInstance().mainThread().execute(() -> {
                        Toast.makeText(RegisterActivity.this, "Successful Registration", Toast.LENGTH_SHORT).show();
                        goBackLogin();
                    });
                } else {
                    String errorMessage = (response != null && response.getMessage() != null)
                            ? response.getMessage()
                            : "Registration failed";
                    AppExecutors.getInstance().mainThread().execute(() -> {
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    Toast.makeText(RegisterActivity.this, "There was an error registering user", Toast.LENGTH_SHORT).show();
                });
                Log.e("MainActivity", "There was an error registering user", e);
            }
        });
    }

    /**
     * Change the activity to LoginActivity
     */
    private void goBackLogin() {
        finish();
    }

}