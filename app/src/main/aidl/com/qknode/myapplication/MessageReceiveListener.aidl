// MessageReceiveListener.aidl
package com.qknode.myapplication;

import com.qknode.myapplication.entity.Message;
// Declare any non-default types here with import statements

interface MessageReceiveListener {
  void onReceiveMessage(in Message message);
}