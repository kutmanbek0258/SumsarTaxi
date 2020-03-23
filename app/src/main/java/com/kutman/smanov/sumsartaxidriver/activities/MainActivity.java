package com.kutman.smanov.sumsartaxidriver.activities;

import androidx.appcompat.app.AppCompatActivity;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import android.os.Bundle;
import android.widget.Toast;

import com.kutman.smanov.sumsartaxidriver.R;
import com.kutman.smanov.sumsartaxidriver.data.UserData;
import com.kutman.smanov.sumsartaxidriver.session.UserSession;
import com.kutman.smanov.sumsartaxidriver.utils.Constants;

import java.net.URISyntaxException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Socket socket;

    private UserData userData;
    private UserSession session;
    private HashMap<String, String> user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userData = new UserData(getApplicationContext());
        session = new UserSession(getApplicationContext());
        user = userData.getUserDetails();

        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.query = "token=" + user.get("token");
            socket = IO.socket(Constants.BASE_URL,opts);
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        socket.on(Socket.EVENT_CONNECT,onConnect);
        socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        socket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        socket.on("on_hello",onHello);
        socket.emit("hello","Hello socket!");
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    };

    private Emitter.Listener onHello = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),"Hello",Toast.LENGTH_LONG).show();
                }
            });
        }
    };
}
