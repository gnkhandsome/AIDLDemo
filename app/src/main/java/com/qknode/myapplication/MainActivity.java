package com.qknode.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.qknode.myapplication.entity.Message;
import com.qknode.myapplication.service.RemoteService;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private IConnectionService iConnectionServiceProxy;
    private IMessageService iMessageService;
    private IServiceManage serviceManageBinder;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            // 设置Message的类加载器，防止出现序列化异常。
            bundle.setClassLoader(Message.class.getClassLoader());
            Message message = bundle.getParcelable("message");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, message.getContent(), Toast.LENGTH_SHORT).show();
                }
            }, 3000);
        }
    };

    private Messenger clientMessenager = new Messenger(handler);


    private MessageReceiveListener messageReceiveListener = new MessageReceiveListener.Stub() {
        @Override
        public void onReceiveMessage(Message message) throws RemoteException {
            Log.i("onReceiveMessage", Thread.currentThread().getName());
            Log.i("onReceiveMessage", String.valueOf(message.isSendSuccess()));
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, message.getContent(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private Messenger messengerProxy;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn_connect = findViewById(R.id.btn_connect);
        Button btn_disconnect = findViewById(R.id.btn_disconnect);
        Button btn_status = findViewById(R.id.btn_status);
        Button btn_send_message = findViewById(R.id.btn_send_message);
        Button btn_register_message = findViewById(R.id.btn_register_message);
        Button btn_unregister_message = findViewById(R.id.btn_unregister_message);
        Button btn_messenger = findViewById(R.id.btn_messenger);
        TextView tv_status = findViewById(R.id.tv_status);
        // 启动子进程
        Intent intent = new Intent(MainActivity.this, RemoteService.class);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // 主进程获取子进程的代理服务
                serviceManageBinder = IServiceManage.Stub.asInterface(service);
                try {
                    iConnectionServiceProxy = IConnectionService.Stub.asInterface(serviceManageBinder.getService(IConnectionService.class.getSimpleName()));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                try {
                    iMessageService = IMessageService.Stub.asInterface(serviceManageBinder.getService(IMessageService.class.getSimpleName()));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                try {
                    messengerProxy = new Messenger(serviceManageBinder.getService(Messenger.class.getSimpleName()));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, Context.BIND_AUTO_CREATE);
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    iConnectionServiceProxy.connect();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    iConnectionServiceProxy.disConnect();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        btn_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    boolean isConnected = iConnectionServiceProxy.isConnected();
                    tv_status.setText(String.valueOf(isConnected));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        btn_send_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = new Message();
                message.setContent("message from main");
                try {
                    iMessageService.sendMessage(message);
                    Log.i("sendMessage", String.valueOf(message.isSendSuccess()));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        btn_register_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    iMessageService.registerMessageReceiveListener(messageReceiveListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        btn_unregister_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    iMessageService.unRegisterMessageReceiveListener(messageReceiveListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        btn_messenger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = new Message();
                message.setContent("send message from main by Messenger");

                android.os.Message message1 = new android.os.Message();
                Bundle bundle = new Bundle();
                bundle.putParcelable("message", message);
                message1.setData(bundle);
                // 提供给子进程回复消息的Messenger
                message1.replyTo = clientMessenager;
                try {
                    messengerProxy.send(message1);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }


}