package com.maiji.magkareble40;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blakequ.bluetooth_manager_lib.BleManager;
import com.blakequ.bluetooth_manager_lib.scan.BluetoothScanManager;
import com.blakequ.bluetooth_manager_lib.scan.ScanOverListener;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanCallbackCompat;
import com.blakequ.bluetooth_manager_lib.scan.bluetoothcompat.ScanResultCompat;
import com.chad.library.adapter.base.BaseQuickAdapter;

import java.util.ArrayList;
import java.util.List;

/**
* @author xqx
* @email djlxqx@163.com
* blog:http://www.cnblogs.com/xqxacm/
* createAt 2017/9/6
* description:  扫描蓝牙设备  选择需要连接的传感器
*/

public class SelectDeviceActivity extends Activity implements View.OnClickListener {

    private Button btnScan;        //开始扫描按钮
    private Button btnStopScan;   //停止扫描按钮
    private Button btnOk;   //选择好了需要连接的mac设备

    BluetoothScanManager scanManager ;


    /* 列表相关 */
    private RecyclerView recyclerView ; //列表
    private ScanDeviceAdapter adapter;
    private ArrayList<String> deviceMacs ; // 数据源 ： 所有扫描到的设备mac地址
    private ArrayList<String> deviceList ; // 数据源 ： 所有扫描到的设备mac地址
    private ArrayList<String> selectDeviceMacs; // 选择的需要连接的设备的mac集合
    private int GPS_REQUEST_CODE = 10;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device);
        deviceMacs = new ArrayList<>();
        deviceList = new ArrayList<>();
        selectDeviceMacs = new ArrayList<>();
        initView();
        initEvent();
        initBle();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
            }
            if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3);
            }
        }
    }

    private void initBle() {
        scanManager = BluetoothScanManager.getInstance(this);//BleManager.getScanManager(this);
        Log.i("lc","scanManager="+scanManager);
        scanManager.setScanOverListener(new ScanOverListener() {
            @Override
            public void onScanOver() {
            }
        });


        scanManager.setScanCallbackCompat(new ScanCallbackCompat() {
            @Override
            public void onBatchScanResults(List<ScanResultCompat> results) {
                super.onBatchScanResults(results);
                Log.i("lc","onBatchScanResults");
            }

            @Override
            public void onScanFailed(final int errorCode) {
                super.onScanFailed(errorCode);
                Log.i("lc","onScanFailed"+errorCode);
                if (errorCode == SCAN_FAILED_LOCATION_CLOSE){
                    Toast.makeText(getApplicationContext(), "Location is closed, you should open first", Toast.LENGTH_LONG).show();
                }else if(errorCode == SCAN_FAILED_LOCATION_PERMISSION_FORBID){
                    Toast.makeText(getApplicationContext(), "You have not permission of location", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(), "Other exception", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onScanResult(int callbackType, ScanResultCompat result) {
                super.onScanResult(callbackType, result);
                String strName = result.getLeDevice().getName();
                if (strName==null) return;
                if (strName.contains("WT")){
                    String strMAC =  result.getLeDevice().getAddress();
                    int iRssi = result.getLeDevice().getRssi();
                    String strdevice = String.format("%s [%s %ddb]",strName,strMAC,iRssi);
                    if (!deviceMacs.contains(strMAC)) {
                        deviceMacs.add(strMAC);
                        deviceList.add(strdevice);
                        adapter.notifyDataSetChanged();
                    }
                    else
                    {
                        int iIndex = deviceMacs.indexOf(strMAC);
                        deviceList.remove(iIndex);
                        deviceList.add(iIndex,strdevice);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        });

    }

    private void initEvent() {
//        btnScan.setOnClickListener(this);
//        btnStopScan.setOnClickListener(this);
        btnOk.setOnClickListener(this);
    }

    private void initView() {
//        btnScan = (Button) findViewById(R.id.btnScan);
//        btnStopScan = (Button) findViewById(R.id.btnStopScan);
        btnOk = (Button) findViewById(R.id.btnOk);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        TextView title= (TextView) findViewById(R.id.tv_center);
        title.setText("设备列表");
        TextView tv_scan= (TextView) findViewById(R.id.tv_right);
        tv_scan.setText("扫描");
        tv_scan.setOnClickListener(this);



        // 列表相关初始化
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScanDeviceAdapter(deviceList);

        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (scanManager.isScanning()) {
                    scanManager.stopCycleScan();
                }
                if (!selectDeviceMacs.contains(deviceMacs.get(position))){
                    //如果改item的mac不在已选中的mac集合中 说明没有选中，添加进已选中mac集合中，状态改为"已选择"
                    selectDeviceMacs.add(deviceMacs.get(position));
                    ((TextView)view.findViewById(R.id.txtState)).setText("已选择");
                }else {
                    selectDeviceMacs.remove(deviceMacs.get(position));
                    ((TextView)view.findViewById(R.id.txtState)).setText("未选择");
                }
            }
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_right:
                //开始 扫描
//                scanManager.startCycleScan(); //不会立即开始，可能会延时
//                scanManager.startScanNow(); //立即开始扫描
                openGPSSettings();
                break;
//
//            case R.id.btnStopScan:
//                // 如果正在扫描中 停止扫描
//                if (scanManager.isScanning()) {
//                    scanManager.stopCycleScan();
//                }
//                break;
            case R.id.btnOk:
                Intent intent = new Intent();
                intent.putExtra("data",selectDeviceMacs);                // 设置结果，并进行传送
                this.setResult(1, intent);
                this.finish();
                break;
            case R.id.tv_left:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        openGPSSettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 如果正在扫描中 停止扫描
        if (scanManager.isScanning()) {
            scanManager.stopCycleScan();
        }
    }
    private boolean checkGPSIsOpen() {
        boolean isOpen;
        LocationManager locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        isOpen = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isOpen;
    }

    private void openGPSSettings() {
        if (checkGPSIsOpen()) {
            scanManager.startScanNow(); //立即开始扫描
        } else {
            //没有打开则弹出对话框
            new AlertDialog.Builder(this)
                    .setTitle(R.string.notifyTitle)
                    .setMessage(R.string.gpsNotifyMsg)
                    // 拒绝, 退出应用
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })

                    .setPositiveButton(R.string.setting,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //跳转GPS设置界面
                                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivityForResult(intent, GPS_REQUEST_CODE);
                                }
                            })

                    .setCancelable(false)
                    .show();

        }
    }
}
