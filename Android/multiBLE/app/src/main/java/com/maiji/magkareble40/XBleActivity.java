package com.maiji.magkareble40;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.blakequ.bluetooth_manager_lib.BleManager;
import com.blakequ.bluetooth_manager_lib.BleParamsOptions;
import com.blakequ.bluetooth_manager_lib.connect.BluetoothSubScribeData;
import com.blakequ.bluetooth_manager_lib.connect.ConnectConfig;
import com.blakequ.bluetooth_manager_lib.connect.ConnectState;
import com.blakequ.bluetooth_manager_lib.connect.ConnectStateListener;
import com.blakequ.bluetooth_manager_lib.connect.multiple.MultiConnectManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.eventbus.Subscribe;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * @author xqx
 * @email djlxqx@163.com
 * blog:http://www.cnblogs.com/xqxacm/
 * createAt 2017/9/6
 * description:  ble 4.0 多设备连接
 */

public class XBleActivity extends Activity implements View.OnClickListener {

    private Button btnSelectDevice;  //选择需要绑定的设备
    private Button btnStartConnect;  //开始连接按钮

    private TextView txtContentMac; //获取到的数据解析结果显示

    private final int REQUEST_CODE_PERMISSION = 1; // 权限请求码  用于回调

    MultiConnectManager multiConnectManager;  //多设备连接
    private BluetoothAdapter bluetoothAdapter;   //蓝牙适配器


    private ArrayList<String> connectDeviceMacList; //需要连接的mac设备集合
    ArrayList<BluetoothGatt> gattArrayList; //设备gatt集合

    private TextView txtShowRes; //显示预测结果
    private TextView txtMotionName; //显示预测结果
    private static Context Context = null;
    private ArrayList<ArrayList<Float>> imuData;
    final private int maxLen = 100;
    private ClfModel svm;

    private Map<Integer, String> motionType;
    private int winLen = 5;
    private int startTh = 15;
    private int stopTh = 5;
    private boolean isStart = false;
    private int startCnt = 1;
    private int numCnt = 0;
    private boolean isFirstMotion = true;
    private int motionTypeFinal = 14;
    private boolean resetPredict = false;
    private String headphoneState = "Put on";
    private int motionTypeRaw = 14;
    private Map<Integer, Integer> motionCnt;
    private int timerTotal = 0;

    private Handler handler;


    Button button,button_cc,Record;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xble);

        Context = getApplicationContext();

        initVariables();
        initView();
        requestWritePermission();
        initConfig();  // 蓝牙初始设置
        EventBus.getDefault().register(this);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case 0:
                        int res = msg.arg1;
                        if(res > 5){
                            res -= 4;
                        }
                        txtShowRes.setText(motionType.get(res)+' '+msg.arg2+"\n\n");
                        break;
                    case 1:
                        txtShowRes.setText(motionType.get(10)+"\n\n");
                }

                for(int i=0;i<10;i++){
                    txtShowRes.append(motionCnt.get(i)+" groups"+"\n");
                }
                txtShowRes.append(timerTotal + " seconds");
            }
        };

        stillTimeCounter();

    }

    public static Context getContext() {
        return Context;
    }


    private void initVariables() {
        connectDeviceMacList = new ArrayList<>();
        gattArrayList = new ArrayList<>();

        imuData = new ArrayList<ArrayList<Float>>();
        for(int i=0;i<6;i++){
//            ArrayList<Float> tmp = new ArrayList<Float>(Collections.nCopies(maxLen, 0.0f));
            ArrayList<Float> tmp = new ArrayList<Float>();
            imuData.add(tmp);
        }

        motionType = new HashMap<Integer, String>();
        motionType.put(0,"turn left");
        motionType.put(1, "turn right");
        motionType.put(2, "up");
        motionType.put(3, "down");
        motionType.put(4, "tile left");
        motionType.put(5, "tile right");
        motionType.put(6, "circle clockwise");
        motionType.put(7, "circle counterclockwise");
        motionType.put(8, "eight left");
        motionType.put(9, "eight right");
        motionType.put(10, "none");

        svm = new ClfModel();

        motionCnt = new HashMap<Integer, Integer>();
        for(int i=0;i<15;i++){
            motionCnt.put(i,0);
        }
    }


    private void initView() {
//        btnSelectDevice = (Button) findViewById(R.id.btnSelectDevice);
//        btnStartConnect = (Button) findViewById(R.id.btnStartConnect);
        txtContentMac = (TextView) findViewById(R.id.txtContentMac);
        TextView title = (TextView) findViewById(R.id.tv_center);
        title.setText("Demo");
        TextView tv_ble = (TextView) findViewById(R.id.tv_right);
        tv_ble.setText("BLE");
        tv_ble.setOnClickListener(this);
        button = (Button) findViewById(R.id.bt_AccCali);button.setOnClickListener(this);
        Record=(Button) findViewById(R.id.bt_Record);Record.setOnClickListener(this);
        button_cc = (Button) findViewById(R.id.bt_MagCali);button_cc.setOnClickListener(this);//磁场校准
        txtMotionName = (TextView) findViewById(R.id.txtMotionName);
        txtMotionName.setText("current motion:\n\n");
        txtShowRes = (TextView) findViewById(R.id.txtShowRes);
        txtShowRes.setText("None\n\n");
        for(int i=0;i<10;i++){
            txtMotionName.append(motionType.get(i)+":\n");
            txtShowRes.append(motionCnt.get(i)+" groups\n");
        }
        txtMotionName.append("stay still:\n");
        txtShowRes.append("0 seconds");
    }


    boolean isChecked = false;
    byte[] cmdSave = new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x00, (byte) 0x00, (byte) 0x00};//保存校准
    byte[] cmdQuitCali = new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x00, (byte) 0x00};//退出校准
    byte[] cmdAccCali = new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x01, (byte) 0x00};//加计校准
    byte[] cmdMagCali = new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x07, (byte) 0x00};//磁场校准
    boolean bIdle = true;
    public boolean writeBytes(byte[] value)
    {
        if (bIdle == false){
            Toast.makeText(getApplicationContext(), getString(R.string.busywrite), Toast.LENGTH_SHORT).show();
            return false;
        }
        bIdle = false;
        int iDelay = 500;
        if ((value[2]== (byte)0x01)&&(value[3]==(byte)0x01)) iDelay = 6000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bIdle = true;
            }
        }, iDelay);
        multiConnectManager.cleanSubscribeData();
        multiConnectManager.addBluetoothSubscribeData(
                new BluetoothSubScribeData.Builder().setCharacteristicWrite(
                        UUID.fromString("0000ffe9-0000-1000-8000-00805f9a34fb"), value).build());
        for (int i = 0; i < gattArrayList.size(); i++) {
            multiConnectManager.startSubscribe(gattArrayList.get(i));
        }
        multiConnectManager.startConnect();
        return true;
    }

    public boolean isRxd = true;//记录了标志位

    public EditText editText;
    String Save_path,Global_path;
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_right:
                Intent intent = new Intent(this, SelectDeviceActivity.class);
                startActivityForResult(intent, 1);
                break;
            case R.id.bt_Record:
                if (isRxd) {
                    new AlertDialog.Builder(this).setTitle(getString(R.string.input_path))
                            .setView(editText=new EditText(this)).setPositiveButton("OK", new DialogInterface.OnClickListener()

                    {
                        public void onClick(DialogInterface arg0, int arg1) {
                            SimpleDateFormat Datatime = new SimpleDateFormat("yyyyMMddHHmmss");
                            Date TimeData = new Date(System.currentTimeMillis());//获取当前时间
                            String Tie = Datatime.format(TimeData);
                            Save_path = editText.getText().toString();
                            //不允许为空
                            if (Save_path.length()>0) {
                                String FEATURE_FILE_PATH = Environment.getExternalStorageDirectory() + "/Record/"+Save_path;
                                if(Save_path.length()>=0)Toast.makeText(XBleActivity.this,"Save path:"+FEATURE_FILE_PATH,Toast.LENGTH_LONG ).show();
                                File filestring = new File(FEATURE_FILE_PATH);
                                if (filestring.exists()) {
                                } else {
                                    filestring.mkdirs();
                                }
                                try {
                                    myFile = new XBleActivity.MyFile("/mnt/sdcard/Record/" +Save_path+"/Record"+ Tie + ".xls");
                                    Global_path = "/mnt/sdcard/Record/" +Save_path+"/Record"+ Tie + ".xls";
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    String str = "Time";
                                    for (int i=0;i<connectDeviceMacList.size();i++){
                                        str +="\t"+"No"+"\t"+ "MAC"  + "\t" +"ax" + "\t" + "ay"+ "\t" +"az"
                                                + "\t" + "wx" + "\t" +"wy" + "\t" + "wz"
                                                + "\t" + "AngleX" + "\t" +"AngleY" + "\t" + "AngleZ";
                                    }
                                    str+="\n";
                                    myFile.Write(str);
                                    Record.setText(getString(R.string.menu_stop));
                                    //关闭啦
                                    isRxd = false;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            else
                            {
                                Toast.makeText(XBleActivity.this,"路径不能为空",Toast.LENGTH_LONG ).show();//测试用
                            }
                        }

                    })
                            .setNegativeButton("Cancel", null).show();
                } else {
                    if(Save_path.length()>0) {
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss ");
                        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
                        String stops = getString(R.string.stop_time) + formatter.format(curDate) + "\r\n";
                        try {
                            myFile.Write(stops + "\t\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Record.setText(getString(R.string.menu_start));
                        isRxd = true;
                        try {
                            myFile.Close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        new AlertDialog.Builder(XBleActivity.this)
                                .setTitle(getString(R.string.hint))
                                .setIcon(android.R.drawable.ic_dialog_alert)
//                            .setMessage(getString(R.string.data_record) + Tie+"Record.txt\n" + getString(R.string.open_file))
                                .setMessage(getString(R.string.data_record) + Global_path)
                                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        try {
                                            //获取手机根目录
//                                        File gen= Environment.getRootDirectory();
//                                        File myFile = new File(gen+"/Record.txt");Global_path
                                            File myFile = new File(Global_path);
                                            Log.i("lca", "地址：" + myFile);
                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            intent.setData(Uri.fromFile(myFile));
                                            startActivity(intent);
                                        } catch (Exception err) {
                                        }
                                    }
                                })
                                .setNegativeButton(getString(R.string.Cancel), null)
                                .show();
                    }
                }
            case R.id.bt_AccCali:
                    if (writeBytes(cmdAccCali))
                        Toast.makeText(getApplicationContext(), "加计校准中，请保持水平静止5s", Toast.LENGTH_LONG).show();
                break;
            case R.id.bt_MagCali:
                if (!isChecked) {
                    if (writeBytes(cmdMagCali)) {
                        Toast.makeText(getApplicationContext(), "磁场校准开始", Toast.LENGTH_SHORT).show();
                        button_cc.setText(R.string.accomplish);
                        isChecked = true;
                    }
                } else {
                    button_cc.setText(R.string.Magnetic_calibration);
                    isChecked = false;
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            try {
                                if (writeBytes(cmdQuitCali)){
                                    Toast.makeText(getApplicationContext(), "磁场校准完成", Toast.LENGTH_SHORT).show();
                                    Thread.sleep(500);//休眠500毫秒
                                    writeBytes(cmdSave);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                    break;
                }
                break;
        }
    }


    /**
     * 连接需要连接的传感器
     *
     * @param
     */
    private void connentBluetooth() {

        String[] objects = connectDeviceMacList.toArray(new String[connectDeviceMacList.size()]);
        multiConnectManager.addDeviceToQueue(objects);
        multiConnectManager.addConnectStateListener(new ConnectStateListener() {
            @Override
            public void onConnectStateChanged(String address, ConnectState state) {
                switch (state) {
                    case CONNECTING:
                        Log.e("connectStateX", "设备:" + address + "连接状态:" + "正在连接");
                        break;
                    case CONNECTED:
                        Log.e("connectStateX", "设备:" + address + "连接状态:" + "成功");
                        break;
                    case NORMAL:
                        Log.e("connectStateX", "设备:" + address + "连接状态:" + "失败");
                        break;
                }
            }
        });

        /**
         * 数据回调
         */

        multiConnectManager.setBluetoothGattCallback(new BluetoothGattCallback() {
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                dealCallDatas(gatt, characteristic);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                byte[] value = characteristic.getValue();
                String sdata = "";
                for (int i = 0; i < value.length; i++) {
                    sdata = sdata + String.format("%02x", (0xff & value[i]));
                }
                Log.e("--", "校准指令 = " + sdata);
            }
        });

        multiConnectManager.setServiceUUID("0000ffe5-0000-1000-8000-00805f9a34fb");
        multiConnectManager.addBluetoothSubscribeData(
                new BluetoothSubScribeData.Builder().setCharacteristicNotify(UUID.fromString("0000ffe4-0000-1000-8000-00805f9a34fb")).build());

        //还有读写descriptor
        //start descriptor(注意，在使用时当回调onServicesDiscovered成功时会自动调用该方法，所以只需要在连接之前完成1,3步即可)
        for (int i = 0; i < gattArrayList.size(); i++) {
            multiConnectManager.startSubscribe(gattArrayList.get(i));
        }

        multiConnectManager.startConnect();

    }

    /**
     * 处理回调的数据
     *
     * @param gatt
     * @param characteristic
     */

    float[][] floats = new float[7][30];

    private void dealCallDatas(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        int position = connectDeviceMacList.indexOf(gatt.getDevice().getAddress());
        //第一个传感器数据
        byte[] value = characteristic.getValue();

        if (value[0] != 0x55) {
            return; //开头不是0x55的数据删除
        }
        switch (value[1]) {

            case 0x61:
                //加速度数据
                floats[position][3] = ((((short) value[3]) << 8) | ((short) value[2] & 0xff)) / 32768.0f * 16;   //x轴
                floats[position][4] = ((((short) value[5]) << 8) | ((short) value[4] & 0xff)) / 32768.0f * 16;   //y轴
                floats[position][5] = ((((short) value[7]) << 8) | ((short) value[6] & 0xff)) / 32768.0f * 16;   //z轴
                //角速度数据
                floats[position][6] = ((((short) value[9]) << 8) | ((short) value[8] & 0xff)) / 32768.0f * 2000;  //x轴
                floats[position][7] = ((((short) value[11]) << 8) | ((short) value[10] & 0xff)) / 32768.0f * 2000;  //x轴
                floats[position][8] = ((((short) value[13]) << 8) | ((short) value[12] & 0xff)) / 32768.0f * 2000;  //x轴
                //角度
                floats[position][9] = ((((short) value[15]) << 8) | ((short) value[14] & 0xff)) / 32768.0f * 180;   //x轴
                floats[position][10] = ((((short) value[17]) << 8) | ((short) value[16] & 0xff)) / 32768.0f * 180;   //y轴
                floats[position][11] = ((((short) value[19]) << 8) | ((short) value[18] & 0xff)) / 32768.0f * 180;   //z轴

                for(int i=0;i<6;i++){
                    if (imuData.get(i).size() == maxLen){
                        imuData.get(i).remove(0);
                    }
                    imuData.get(i).add(floats[position][i+3]);
                }
                break;
            case 0x62:

                break;
        }
        if (position == 0) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss.SSS");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            String str = formatter.format(curDate);
            for (int i = 0; i < connectDeviceMacList.size(); i++) {
                str += "\t" + position + "\t" + connectDeviceMacList.get(position) + "\t" + String.format("%.3f", floats[position][3]) + "\t" + String.format("%.3f", floats[position][4]) + "\t" + String.format("%.3f", floats[position][5])
                        + "\t" + String.format("%.3f", floats[position][6]) + "\t" + String.format("%.3f", floats[position][6]) + "\t" + String.format("%.3f", floats[position][8])
                        + "\t" + String.format("%.3f", floats[position][9]) + "\t" + String.format("%.3f", floats[position][10]) + "\t" + String.format("%.3f", floats[position][11]);

            }
            str += "\n";
            if (!isRxd) {
                try {
                    myFile.Write(str);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        EventBus.getDefault().post(new RefreshDatas()); // 发送消息，更新UI 显示数据
    }



    /**
     * 对蓝牙的初始化操作
     */
    private void initConfig() {
        multiConnectManager = BleManager.getMultiConnectManager(this);
        // 获取蓝牙适配器

        try {
            // 获取蓝牙适配器
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            //
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
                return;
            }

            // 蓝牙没打开的时候打开蓝牙
            if (!bluetoothAdapter.isEnabled())
                bluetoothAdapter.enable();
        } catch (Exception err) {
        }
        ;
        BleManager.setBleParamsOptions(new BleParamsOptions.Builder()
                .setBackgroundBetweenScanPeriod(5 * 60 * 1000)
                .setBackgroundScanPeriod(10000)
                .setForegroundBetweenScanPeriod(2000)
                .setForegroundScanPeriod(10000)
                .setDebugMode(BuildConfig.DEBUG)
                .setMaxConnectDeviceNum(7)            //最大可以连接的蓝牙设备个数
                .setReconnectBaseSpaceTime(1000)
                .setReconnectMaxTimes(Integer.MAX_VALUE)
                .setReconnectStrategy(ConnectConfig.RECONNECT_LINE_EXPONENT)
                .setReconnectedLineToExponentTimes(5)
                .setConnectTimeOutTimes(20000)
                .build());
    }

    /**
     * @author xqx
     * @email djlxqx@163.com
     * blog:http://www.cnblogs.com/xqxacm/
     * createAt 2017/8/30
     * description:  权限申请相关，适配6.0+机型 ，蓝牙，文件，位置 权限
     */

    private String[] allPermissionList = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    /**
     * 遍历出需要获取的权限
     */
    private void requestWritePermission() {
        ArrayList<String> permissionList = new ArrayList<>();
        // 将需要获取的权限加入到集合中  ，根据集合数量判断 需不需要添加
        for (int i = 0; i < allPermissionList.length; i++) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, allPermissionList[i])) {
                permissionList.add(allPermissionList[i]);
            }
        }

        String permissionArray[] = new String[permissionList.size()];
        for (int i = 0; i < permissionList.size(); i++) {
            permissionArray[i] = permissionList.get(i);
        }
        if (permissionList.size() > 0)
            ActivityCompat.requestPermissions(this, permissionArray, REQUEST_CODE_PERMISSION);
    }


    /**
     * 权限申请的回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            for (int i = 0;i<permissions.length;i++){
                if (grantResults[i]== PackageManager.PERMISSION_DENIED)
                    Toast.makeText(XBleActivity.this, "Permission "+permissions[i]+" denied, the software will not work properly.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            switch (requestCode) {
                case 1:
                    connectDeviceMacList = data.getStringArrayListExtra("data");
                    Log.e("---", "需要连接的mac" + connectDeviceMacList.toString());
                    //获取设备gatt对象
                    for (int i = 0; i < connectDeviceMacList.size(); i++) {
                        BluetoothGatt gatt = bluetoothAdapter.getRemoteDevice(connectDeviceMacList.get(i)).connectGatt(this, false, new BluetoothGattCallback() {
                        });
                        gattArrayList.add(gatt);
                        Log.e("---", "添加了" + connectDeviceMacList.get(i));
                    }
                    connentBluetooth();
                    break;
            }
        }
    }

    public void resetParam(){
        isStart = false;
        startCnt = 1;
        numCnt = 0;
        isFirstMotion = true;
        motionTypeRaw = 14;
        resetPredict = false;
    }

    public double [] interpolation(int dataLen){
        Log.i("dataLen:", String.valueOf(dataLen));
        dataLen = Math.min(dataLen, maxLen);
        double[] feature = new double[maxLen*imuData.size()];
        for(int i=0;i<6;i++){
            double[] y = new double[dataLen];
            double[] x = new double[dataLen];
            int j = 0;
            for(float num : imuData.get(i).subList(imuData.get(i).size()-dataLen,imuData.get(i).size())){
                y[j] = num;
                x[j] = j;
                j++;
            }
//            System.out.println(Arrays.toString(y));

            SplineInterpolator sp = new SplineInterpolator();
            PolynomialSplineFunction f = sp.interpolate(x, y);
            double delta = (double)(dataLen-1)/(maxLen-1);
//            double[] tmp = new double[maxLen];
            for(int k=0;k<maxLen;k++){
//                System.out.print(k*delta);
//                System.out.print(' ');
//                System.out.print(f.value(k*delta));
//                System.out.print(' ');
                double v = k*delta;
                if(k == maxLen-1){
                    double max_v = f.getKnots()[f.getKnots().length-1];
                    v = max_v;
                }
                feature[maxLen*i+k] = f.value(v);
//                tmp[k] = f.value(k*delta);
            }
//            System.out.println(Arrays.toString(tmp));
        }
//        System.out.println();
//        System.out.println(Arrays.toString(feature));
//        System.out.println(feature.length);
        return feature;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEventAsync(RefreshDatas event){
        if (resetPredict){
            resetParam();
        }
        if (imuData.get(0).size() > winLen && headphoneState == "Put on"){
            int arrayLen = imuData.get(0).size();
            if (!isStart){
                for(int i=3;i<6;i++){
                    if(Math.abs(imuData.get(i).get(arrayLen-1)-imuData.get(i).get(arrayLen-1-winLen)) > startTh){
                        isStart = true;
                        break;
                    }
                }
            }
            else{
                if (startCnt > 5){
                    boolean is_static = true;
                    for(int i=3;i<6;i++){
                        float avg = 0;
                        for(float num:imuData.get(i).subList(arrayLen-1-winLen,arrayLen-1)){
                            avg += Math.abs(num);
                        }
                        avg = avg/winLen;
                        if(avg > stopTh){
                            is_static = false;
                            break;
                        }
                    }
                    if(is_static){
                        double[] feature = interpolation(winLen+startCnt);
                        motionTypeRaw = svm.predictMotion(feature);
//                        txtShowRes.setText(motionType.get(motionTypeRaw));
//                        Message message = handler.obtainMessage();
//                        message.what = motionTypeRaw;
//                        handler.sendMessage(message);

                        if(motionTypeRaw >=10){
                            if(motionTypeRaw == motionTypeFinal){
                                numCnt += 1;
                            }
                            else{
                                motionTypeFinal = motionTypeRaw;
                                numCnt = 1;
                            }
                            isFirstMotion = true;
                            Message message = handler.obtainMessage();
                            message.what = 0;
                            message.arg1 = motionTypeFinal;
                            message.arg2 = numCnt;
                            handler.sendMessage(message);
                        }
                        else{
                            if(isFirstMotion){
                                if(motionTypeFinal != motionTypeRaw){
                                    motionTypeFinal = motionTypeRaw;
                                    numCnt = 0;
                                }
                            }
                            else{
                                if(motionTypeFinal == motionTypeRaw + (int)Math.pow(-1,motionTypeRaw%2)){
                                    numCnt += 1;
                                    Message message = handler.obtainMessage();
                                    message.what = 0;
                                    message.arg1 = motionTypeFinal;
                                    message.arg2 = numCnt;
                                    handler.sendMessage(message);
                                }
                                else{
                                    Log.i("refresh data:", "predict wrong");
                                }
                            }
                            isFirstMotion = !isFirstMotion;
                        }
                        if(numCnt == 5){
                            int res = motionTypeFinal;
                            if(motionTypeFinal > 5){
                                res -= 4;
                            }
                            motionCnt.put(res,motionCnt.get(res) + 1);
                            numCnt = 0;
                        }

                        isStart = false;
                        startCnt = 0;
                    }


                }
                startCnt += 1;
            }
        }
    }

    int delta_total_time = 0;
    long start_time = System.currentTimeMillis();
    private void stillTimeCounter() {
        final Handler handler_new = new Handler();
        handler_new.post(new Runnable() {
                         @Override
                         public void run() {
                             if(isStart){
                                 if(delta_total_time > 10){
                                     resetParam();
                                 }
                                 delta_total_time = 0;
                             }
                             else {
                                 long end_time = System.currentTimeMillis();
                                 if(end_time-start_time > 1000){
                                     delta_total_time += 1;
                                     start_time = System.currentTimeMillis();
                                     if(delta_total_time > 15){
                                         timerTotal += 1;
                                         motionTypeRaw = 10;
                                         motionTypeFinal = 10;
                                         Message message = handler.obtainMessage();
                                         message.what = 1;
                                         handler.sendMessage(message);
//                                         System.out.println("stay still");
                                     }
                                 }

                             }

                             handler_new.postDelayed(this, 10);

                         }
                     }
        );
    }

    @ Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RefreshDatas event) {
        txtContentMac.setText("");
        for (int i = 0; i < connectDeviceMacList.size(); i++) {
            txtContentMac.append("---------------------------------------------------------------------" + "\n");
            txtContentMac.append("Mac:" + connectDeviceMacList.get(i) + "\n");
            txtContentMac.append("加速度:" + "X:" + String.format("%.2fg", floats[i][3]) + "   " + "Y:" + String.format("%.2fg", floats[i][4])
                    + "   " + "Z:" + String.format("%.2fg", floats[i][5]) + "\n");
            txtContentMac.append("角速度:" + "X:" + String.format("%.2f°/s", floats[i][6]) + "   " + "Y:" + String.format("%.2f°/s", floats[i][6])
                    + "   " + "Z:" + String.format("%.2f°/s", floats[i][8]) + "\n");
            txtContentMac.append("角    度:" + "X:" + String.format("%.2f°", floats[i][9]) + "   " + "Y:" + String.format("%.2f°", floats[i][10])
                    + "   " + "Z:" + String.format("%.2f°", floats[i][11]) + "\n");
        }



    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
    MyFile myFile;

    class MyFile {
        FileOutputStream fout;

        public MyFile(String fileName) throws FileNotFoundException {
            fout = new FileOutputStream(fileName, false);
        }

        public void Write(String str) throws IOException {
            byte[] bytes = str.getBytes();
            fout.write(bytes);
        }

        public void Close() throws IOException {
            fout.close();
            fout.flush();
        }
    }
}
