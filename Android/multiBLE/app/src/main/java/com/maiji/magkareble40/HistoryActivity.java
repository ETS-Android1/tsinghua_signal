package com.maiji.magkareble40;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.icu.lang.UCharacter;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {
    public static FirebaseAuth mAuth = XBleActivity.mAuth;
    private String userId;
    private DatabaseReference mDatabase;
    private ArrayList<String> dateList;
    private Map<String, Integer> cntList;
    private Map<String, Double> timeList;
    private GraphView graphCnt;
    private GraphView graphTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dateList = new ArrayList<String>();
        cntList = new HashMap<String, Integer>();
        timeList = new HashMap<String, Double>();
        initValue();
        initView();

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            userId = currentUser.getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            loadHistory();
        }
    }

    private void initValue(){
        for(int i=6;i>=0;i--) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -i);
            String yesterday = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
            dateList.add(yesterday);
            cntList.put(yesterday,0);
            timeList.put(yesterday,0.0);
        }
    }

    private void initView(){
        graphCnt = (GraphView) findViewById(R.id.graphCnt);
        graphTime = (GraphView) findViewById(R.id.graphTime);
        DataPoint dataCnt[] = new DataPoint[7];
        DataPoint dataTime[] = new DataPoint[7];
        for(int i=0;i<7;i++){
            double x = (double) i;
            dataCnt[i] = new DataPoint(x,0.0);
            dataTime[i] = new DataPoint(x,0.0);
        }
        BarGraphSeries<DataPoint> seriesCnt = new BarGraphSeries<DataPoint>(dataCnt);
        BarGraphSeries<DataPoint> seriesTime = new BarGraphSeries<DataPoint>(dataTime);
        seriesCnt.setSpacing(20);
        seriesTime.setSpacing(20);

        graphCnt.addSeries(seriesCnt);
        graphTime.addSeries(seriesTime);
        graphCnt.setTitle("Exercise Times");
        graphTime.setTitle("Sitting Time");


    }

    private void loadHistory(){
        for(String date:dateList){
            Log.i("firebase",date);
            mDatabase.child("users").child(userId).child(date).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                    }
                    else {
                        Log.d("firebase", "load successfully");
                        GenericTypeIndicator<Map<String ,Integer>> t = new GenericTypeIndicator<Map<String ,Integer>>() {};
                        Map<String,Integer> map= task.getResult().getValue(t);
                        int cnt = 0;
                        double totalTime = 0;
                        if(map != null){
                            for(String key : map.keySet()){
                                if(key.equals("timerTotal")){
                                    totalTime = map.get(key)/60;
                                }
                                else{
                                    cnt += map.get(key)*5;
                                }
                            }
                            cntList.put(date,cnt);
                            timeList.put(date,totalTime);
                            updateUI();
                        }

                    }
                }
            });
        }

    }

    private void updateUI(){
        System.out.println(String.valueOf(cntList));
        System.out.println(String.valueOf(timeList));
        DataPoint dataCnt[] = new DataPoint[7];
        DataPoint dataTime[] = new DataPoint[7];
        for(int i=0;i<7;i++){
            dataCnt[i] = new DataPoint((double)i,(double)cntList.get(dateList.get(i)));
            dataTime[i] = new DataPoint((double)i, (double)timeList.get(dateList.get(i)));
        }
        BarGraphSeries<DataPoint> seriesCnt = new BarGraphSeries<DataPoint>(dataCnt);
        BarGraphSeries<DataPoint> seriesTime = new BarGraphSeries<DataPoint>(dataTime);
        seriesCnt.setSpacing(20);
        seriesTime.setSpacing(20);
        graphCnt.removeAllSeries();
        graphTime.removeAllSeries();;
        graphCnt.addSeries(seriesCnt);
        graphTime.addSeries(seriesTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}