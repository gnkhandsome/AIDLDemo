// IConnectionService.aidl
package com.qknode.myapplication;

// Declare any non-default types here with import statements

// connect service
interface IConnectionService {
  oneway void connect();
  void disConnect();
  boolean isConnected();
}