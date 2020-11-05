package com.naviocean.centroidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.RectF;
import android.os.Bundle;
import com.naviocean.centroidtracking.CentroidTracking;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    protected CentroidTracking tracking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        tracking = new CentroidTracking(5);
        RectF r1 = new RectF(114, 182, 152, 240);
        System.out.println(r1.left);
        ArrayList<RectF> list = new ArrayList<>();
        list.add(r1);
        Map<Integer, Pair<Integer,Integer>> tracking_results = tracking.update(list);
        printValue(tracking_results);
        list.clear();
        RectF r3 = new RectF(112, 180, 152, 240);
        list.add(r3);
        tracking_results = tracking.update(list);
        printValue(tracking_results);
    }

    private void printValue(Map<Integer, Pair<Integer, Integer>> result){
        for (Map.Entry<Integer, Pair<Integer, Integer>> r : result.entrySet()) {
            int id = r.getKey();
            Pair<Integer, Integer> centroid = r.getValue();
            System.out.println(String.format("id: %d centerX: %d centerY: %d", id, centroid.first, centroid.second));
        }
    }
}