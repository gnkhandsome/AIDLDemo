package com.qknode.myapplication.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Messenger;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qknode.myapplication.IConnectionService;
import com.qknode.myapplication.IMessageService;
import com.qknode.myapplication.IServiceManage;
import com.qknode.myapplication.MessageReceiveListener;
import com.qknode.myapplication.entity.Message;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 管理和提供子进程的链接和消息服务
 */
public class RemoteService extends Service {

    private boolean isConnected = false;

    private RemoteCallbackList<MessageReceiveListener> messageReceiveListeners = new RemoteCallbackList<>();
//    private ArrayList<MessageReceiveListener> messageReceiveListeners = new ArrayList<>();


    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            // 设置Message的类加载器，防止出现序列化异常。
            bundle.setClassLoader(Message.class.getClassLoader());
            Message message = bundle.getParcelable("message");
            Toast.makeText(RemoteService.this, message.getContent(), Toast.LENGTH_SHORT).show();

            // 获取客户端用于回复消息的Messenger
            Messenger messenger = msg.replyTo;
            android.os.Message message1 = new android.os.Message();
            Bundle bundle1 = new Bundle();
            Message message2 = new Message();
            message2.setContent("send message from remote by Messenger");
            bundle1.putParcelable("message", message2);
            message1.setData(bundle1);
            try {
                messenger.send(message1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    private ScheduledFuture scheduledFuture;

    @Override
    public void onCreate() {
        super.onCreate();
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
    }

    private Messenger messenger = new Messenger(handler);

    // 方法实现是在binder线程中执行的
    private IConnectionService iConnectionService = new IConnectionService.Stub() {
        @Override
        public void connect() throws RemoteException {
            try {
                // 耗时操作会阻塞主线程
                Thread.sleep(5000);
                isConnected = true;
                Log.i("currentThread:connect", Thread.currentThread().getName());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RemoteService.this, "connect", Toast.LENGTH_SHORT).show();
                    }
                });

                // 模拟建立连接成功后每隔5秒钟会受到一条消息
                scheduledFuture = scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        int size = messageReceiveListeners.beginBroadcast();
                        for (int i = 0; i < size; i++) {
                            Message message = new Message();
                            message.setContent("message from remote");
                            message.setSendSuccess(true);
                            try {
                                messageReceiveListeners.getBroadcastItem(i).onReceiveMessage(message);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        messageReceiveListeners.finishBroadcast();
                    }
                }, 5000, 5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void disConnect() throws RemoteException {
            Log.i("currentThread:dis", Thread.currentThread().getName());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RemoteService.this, "disConnect", Toast.LENGTH_SHORT).show();
                }
            });
            isConnected = false;
            scheduledFuture.cancel(true);
        }

        @Override
        public boolean isConnected() throws RemoteException {
            Log.i("currentThread:isCon", Thread.currentThread().getName());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RemoteService.this, String.valueOf(isConnected), Toast.LENGTH_SHORT).show();
                }
            });
            return isConnected;
        }
    };

    private IMessageService iMessageService = new IMessageService.Stub() {
        @Override
        public void sendMessage(Message message) throws RemoteException {
            int size = messageReceiveListeners.beginBroadcast();
            if (size <= 0) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(RemoteService.this, "未注册消息监听！", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            if (!isConnected) {
                message.setSendSuccess(false);
                message.setContent("connect is not ready!");
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(RemoteService.this, "connect is not ready!", Toast.LENGTH_SHORT).show();
//                    }
//                });
            } else {
                message.setSendSuccess(true);
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(RemoteService.this, message.getContent(), Toast.LENGTH_SHORT).show();
//                    }
//                });
            }


            for (int i = 0; i < size; i++) {
                try {
                    messageReceiveListeners.getBroadcastItem(i).onReceiveMessage(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            messageReceiveListeners.finishBroadcast();
        }

        @Override
        public void registerMessageReceiveListener(MessageReceiveListener messageReceiveListener) throws RemoteException {
            if (messageReceiveListeners != null) {
//                messageReceiveListeners.clear();
                messageReceiveListeners.register(messageReceiveListener);
            }
        }

        @Override
        public void unRegisterMessageReceiveListener(MessageReceiveListener messageReceiveListener) throws RemoteException {
            if (messageReceiveListeners != null) {
                messageReceiveListeners.unregister(messageReceiveListener);
            }
        }
    };


    IServiceManage iServiceManage = new IServiceManage.Stub() {
        @Override
        public IBinder getService(String serviceName) throws RemoteException {
            if (IConnectionService.class.getSimpleName().equals(serviceName)) {
                return iConnectionService.asBinder();
            } else if (IMessageService.class.getSimpleName().equals(serviceName)) {
                return iMessageService.asBinder();
            } else if (Messenger.class.getSimpleName().equals(serviceName)) {
                return messenger.getBinder();
            } else {
                return null;
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iServiceManage.asBinder();
    }
}
