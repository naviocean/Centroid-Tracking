/*
 * The source code is implemented base on https://github.com/prat96/Centroid-Object-Tracking
 */
package com.naviocean.centroidtracking;

import android.graphics.RectF;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CentroidTracking {
    private int nextObjectID = 0;
    private int maxDisappeared = 5;
    private Map<Integer, Pair<Integer,Integer>> objects = new HashMap<Integer, Pair<Integer,Integer>>();
    private Map<Integer, Integer> disappeared = new HashMap<Integer, Integer>();

    private void register(Pair<Integer,Integer> centroid) {
        int object_ID = this.nextObjectID;
        this.objects.put(object_ID, centroid);
        this.disappeared.put(object_ID, 0);
        this.nextObjectID +=1;
        if(this.nextObjectID > 9999){
            this.nextObjectID = 100;
        }
    }

    private void deregister(int objectID) {
        this.objects.remove(objectID);
        this.disappeared.remove(objectID);
    }

    private double calDistance(Pair<Integer,Integer> c1, Pair<Integer,Integer> c2) {
        int x = c1.first - c2.first;
        int y = c1.second - c2.second;
        double dist = Math.sqrt((x*x) + (y*y));
        return dist;

    }

    public Pair<Integer, Integer> getCentroid(RectF rect) {
        int cX = (int) ((rect.left + rect.right) / 2.0);
        int cY = (int) ((rect.top + rect.bottom) / 2.0);
        Pair<Integer, Integer> centroid = Pair.create(cX, cY);
        return centroid;
    }

    public Map<Integer, Pair<Integer,Integer>> update(ArrayList<RectF> rects){
        if(rects.size() == 0) {
            if(this.disappeared.size() > 0){
                Iterator<Map.Entry <Integer, Integer>> iter = this.disappeared.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<Integer, Integer> dis = iter.next();
                    int n_disappeared = this.disappeared.get(dis.getKey()) +1;

                    if(n_disappeared > this.maxDisappeared) {
                        iter.remove();
                        this.objects.remove(dis.getKey());
                    }else{
                        this.disappeared.put(dis.getKey(), n_disappeared);
                    }
                }

            }
            return this.objects;
        }

        List<Pair<Integer,Integer>> inputCentorids = new LinkedList<Pair<Integer, Integer>>();
        for (final RectF rect : rects) {
            Pair<Integer, Integer> centroid = getCentroid(rect);
            inputCentorids.add(centroid);
        }


        if(this.objects.size() == 0) {
            for(Pair<Integer,Integer> centroid: inputCentorids){
                this.register(centroid);
            }
        }else{
            ArrayList<Integer> objectIDs = new ArrayList<Integer>(this.objects.keySet());
            Collection<Pair<Integer, Integer>> objectCentroids = this.objects.values();

            // Calculate Distances
            ArrayList<ArrayList<Float>> Distances = new ArrayList<ArrayList<Float>>();
            for(Pair<Integer,Integer> objectCentroid: objectCentroids){
                ArrayList<Float> temp_D = new ArrayList<>();
                for(Pair<Integer,Integer> inputCentroid: inputCentorids){
                    float dist = (float) calDistance(objectCentroid, inputCentroid);
                    temp_D.add(dist);
                }
                Distances.add(temp_D);
            }

            // load rows and cols
            ArrayList<Integer> cols = new ArrayList<>();
            ArrayList<Integer> rows = new ArrayList<>();

            //find indices for cols
            for(ArrayList<Float> v: Distances){
                int temp = v.indexOf(Collections.min(v));
                cols.add(temp);
            }

            //rows calculation
            //sort each mat row for rows calculation
            ArrayList<ArrayList<Float>> D_copy = new ArrayList<ArrayList<Float>>();
            for (ArrayList<Float> v : Distances)
            {
                Collections.sort(v);
                D_copy.add(v);
            }

            // use cols calc to find rows
            // slice first elem of each column
            ArrayList<Pair<Float, Integer>> temp_rows = new ArrayList<>();
            int k = 0;
            for(ArrayList<Float> i: D_copy) {
                Float t = i.get(0);
                Pair<Float, Integer> n = Pair.create(t, k);
                temp_rows.add(n);
                k++;
            }
            //print sorted indices of temp_rows
            for(Pair<Float, Integer> x: temp_rows){
                int n = x.second;
                rows.add(n);
            }

            Set<Integer> usedRows = new HashSet<Integer>();
            Set<Integer> usedCols = new HashSet<Integer>();
            //loop over the combination of the (rows, columns) index tuples
            for(int i = 0; i < rows.size(); i++) {
                //if we have already examined either the row or column value before, ignore it
                if(usedRows.contains(rows.get(i)) || usedCols.contains(cols.get(i))){
                    continue;
                }

                //otherwise, grab the object ID for the current row, set its new centroid,
                // and reset the disappeared counter
                int objectID = objectIDs.get(rows.get(i));
                if(this.objects.containsKey(objectID)){
                    Pair<Integer,Integer> temp = inputCentorids.get(cols.get(i));
                    this.objects.put(objectID, temp);
                }
                this.disappeared.put(objectID, 0);
                usedRows.add(rows.get(i));
                usedCols.add(cols.get(i));
            }

            // compute indexes we have NOT examined yet
            Set<Integer> objRows =  new HashSet<Integer>();
            Set<Integer> inpCols = new HashSet<Integer>();
            //D.shape[0]
            for (int i = 0; i < objectCentroids.size(); i++)
            {
                objRows.add(i);
            }

            //D.shape[1]
            for (int i = 0; i < inputCentorids.size(); i++)
            {
                inpCols.add(i);
            }

            Set<Integer> unusedRows = difference(objRows, usedRows);
            Set<Integer> unusedCols = difference(inpCols, usedCols);

            if (objectCentroids.size() >= inputCentorids.size()){
                for(Integer row: unusedRows){
                    int objectID = objectIDs.get(row);
                    this.disappeared.put(objectID, this.disappeared.get(objectID) + 1);
                    if(this.disappeared.get(objectID) > this.maxDisappeared){
                        this.deregister(objectID);
                    }
                }
            }else{
                for(Integer col: unusedCols){
                    this.register(inputCentorids.get(col));
                }
            }

        }
        return this.objects;
    }

    private static <T> Set<T> difference(final Set<T> setOne, final Set<T> setTwo) {
        Set<T> result = new HashSet<T>(setOne);
        result.removeAll(setTwo);
        return result;
    }

}
