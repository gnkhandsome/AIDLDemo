// IServiceManage.aidl
package com.qknode.myapplication;

// Declare any non-default types here with import statements

interface IServiceManage {

  IBinder getService(String serviceName);
}