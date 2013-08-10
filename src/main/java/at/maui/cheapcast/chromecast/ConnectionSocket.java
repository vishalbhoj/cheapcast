package at.maui.cheapcast.chromecast;

import android.util.Log;
import at.maui.cheapcast.chromecast.model.ConnectionCommand;
import at.maui.cheapcast.chromecast.model.ConnectionResponse;
import at.maui.cheapcast.service.CheapCastService;
import com.google.gson.Gson;
import org.eclipse.jetty.websocket.WebSocket;

import java.io.IOException;

public class ConnectionSocket implements WebSocket, WebSocket.OnTextMessage, WebSocket.OnFrame {

    public static final String LOG_TAG = "ConnectionSocket";

    protected FrameConnection mFrameConnection;
    protected Connection mConnection;
    private Gson mGson;
    private CheapCastService mService;
    private App mApp;

    public ConnectionSocket(CheapCastService service) {
        mService = service;
        mGson = new Gson();
    }

    public FrameConnection getConnection()
    {
        return mFrameConnection;
    }

    @Override
    public void onMessage(String message) {
        Log.d(LOG_TAG, "ConnectionSocket, Message: " + message);
        ConnectionCommand cmd = mGson.fromJson(message, ConnectionCommand.class);

        if(cmd.getType().equals("REGISTER")) {
            mApp = mService.getApp(cmd.getName().replace("Dev",""));
            mApp.setControlChannel(this);

            ConnectionResponse response = new ConnectionResponse();
            response.setType("CHANNELREQUEST");
            response.setRequestId(0);
            response.setSenderId(0);
            try {
                mConnection.sendMessage(mGson.toJson(response));
                Log.d(LOG_TAG, "replied to REGISTER");
            } catch (IOException e) {
                Log.e(LOG_TAG, "reply Failed", e);
            }
        } else if(cmd.getType().equals("CHANNELRESPONSE")) {
            ConnectionResponse response = new ConnectionResponse();
            response.setType("NEWCHANNEL");
            response.setRequestId(0);
            response.setSenderId(0);
            response.setURL(String.format("ws://localhost:8008/receiver/%s", mApp.getName()));
            try {
                mConnection.sendMessage(mGson.toJson(response));
                Log.d(LOG_TAG, "replied to CHANNELRESPONSE");
            } catch (IOException e) {
                Log.e(LOG_TAG, "reply Failed", e);
            }
        }
    }

    @Override
    public void onOpen(Connection connection) {
        Log.d(LOG_TAG, "ConnectionSocket: onOpen();");
        connection.setMaxIdleTime(Integer.MAX_VALUE);
        connection.setMaxTextMessageSize(64*1024);
        connection.setMaxBinaryMessageSize(64*1024);
        mConnection = connection;
    }

    @Override
    public void onClose(int i, String s) {
        Log.d(LOG_TAG, "ConnectionSocket: onClose();");

        if(mApp != null) {
            mApp.stop();
            mApp.setControlChannel(null);
        }
    }

    @Override
    public boolean onFrame(byte b, byte b2, byte[] bytes, int i, int i2) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onHandshake(FrameConnection frameConnection) {
        mFrameConnection = frameConnection;
    }
}