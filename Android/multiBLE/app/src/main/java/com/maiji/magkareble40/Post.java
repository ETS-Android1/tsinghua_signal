package com.maiji.magkareble40;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Post {
    public Map<String,Integer> motionTypeCnt= new HashMap<String, Integer>();
    public int timerTotal = 0;

    public Post(){}

    public Post(Map<Integer, String> motionType, Map<Integer, Integer> motionCnt, int timer){
        for(Integer key:motionType.keySet()){
            motionTypeCnt.put(motionType.get(key), motionCnt.get(key));
        }
        timerTotal = timer;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        for(String key:motionTypeCnt.keySet()){
            result.put(key, motionTypeCnt.get(key));
        }
        result.put("timerTotal", timerTotal);
        return result;
    }

    public Map<String, Integer> getMotionTypeCnt(){
        return motionTypeCnt;
    }

    public int getTimerTotal(){
        return timerTotal;
    }
}
