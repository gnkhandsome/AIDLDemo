// IConnectionService.aidl
package com.qknode.myapplication;

import com.qknode.myapplication.entity.Message;
import com.qknode.myapplication.MessageReceiveListener;

// Declare any non-default types here with import statements

// message service
interface IMessageService {
  void sendMessage(inout Message message);

  void registerMessageReceiveListener(MessageReceiveListener messageReceiveListenr);

    void unRegisterMessageReceiveListener(MessageReceiveListener messageReceiveListenr);
}