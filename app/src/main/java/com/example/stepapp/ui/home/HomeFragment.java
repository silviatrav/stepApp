package com.example.stepapp.ui.home;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.stepapp.R;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static android.widget.Toast.makeText;

public class HomeFragment extends Fragment {
    MaterialButtonToggleGroup materialButtonToggleGroup;
    RadioGroup radioButtonGroup;

    // Text view and Progress Bar variables
    public TextView stepsCountTextView;
    public ProgressBar stepsCountProgressBar;

    private SensorEventListener listener;

    // TODO 1: ACC sensors.
    private Sensor mSensorACC;
    private SensorManager mSensorManager;

    // Step Detector sensor
    private Sensor mSensorSD;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // TODO 9: Initialize the TextView variable
        stepsCountTextView = (TextView) root.findViewById(R.id.stepsCount);

        // Initialize ProgressBar variable
        stepsCountProgressBar = (ProgressBar) root.findViewById(R.id.progressBar);

        // TODO 2: Get an instance of the sensor manager.
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensorACC = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        // Get an instance of the sensor manager for the step detector
       mSensorSD = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        // TODO 11
        // Instantiate the StepCounterListener
        listener = new StepCounterListener(stepsCountTextView, stepsCountProgressBar);

        // Toggle group button
        materialButtonToggleGroup = (MaterialButtonToggleGroup) root.findViewById(R.id.toggleButtonGroup);
        materialButtonToggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (group.getCheckedButtonId() == R.id.toggleStart) {

                    //Place code related to Start button
                    makeText(getContext(), "START", Toast.LENGTH_SHORT).show();

                    // TODO 3: Check if the Accelerometer sensor exists
                    if (mSensorACC != null){
                        // Register the ACC listener
                        mSensorManager.registerListener(listener, mSensorACC, SensorManager.SENSOR_DELAY_NORMAL);
                    }else{
                        Toast.makeText(getContext(),R.string.acc_not_available, Toast.LENGTH_SHORT).show();
                    }

                    // Check if the Step detector sensor exists
                    if (mSensorSD != null){
                        mSensorManager.registerListener(listener, mSensorSD, SensorManager.SENSOR_DELAY_NORMAL);
                    }else{
                        Toast.makeText(getContext(), R.string.step_not_available, Toast.LENGTH_SHORT).show();
                    }

                } else if (group.getCheckedButtonId() == R.id.toggleStop) {
                    Toast.makeText(getContext(), "STOP", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getContext(), "You made " + stepsCountTextView.getText() + " steps!", Toast.LENGTH_SHORT).show();

                    // TODO 4: Unregister the listener
                    mSensorManager.unregisterListener(listener);
                }
            }
        });

        // RadioGroup button
        radioButtonGroup = (RadioGroup) root.findViewById(R.id.toggleRadioButtonGroup);
        radioButtonGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId) {
                    case R.id.Accelerometer:
                        StepCounterListener.resetSteps();
                        StepCounterListener.enable_acc();
                        makeText(getContext(), "Enabled Accelerometer mode", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.stepDetector:
                        StepCounterListener.resetSteps();
                        StepCounterListener.enable_sd();
                        makeText(getContext(), "Enabled Step Detector mode", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        return root;
    }
}

// Sensor event listener
class StepCounterListener implements SensorEventListener {

    public static boolean using_acc;
    private long lastUpdate = 0;

    // ACC Step counter
    public static int mACCStepCounter = 0;
    ArrayList<Integer> mACCSeries = new ArrayList<Integer>();
    private double accMag = 0;
    private int lastXPoint = 1;
    int stepThreshold = 6;

    // Android step detector
    public static int mAndroidStepCount = 0;

    // TextView
    private static TextView stepsCountTextView;
    // ProgressBar
    private static ProgressBar stepsCountProgressBar;

    //TODO 10
    public StepCounterListener(TextView tv, ProgressBar pb){
        stepsCountTextView = tv;
        stepsCountProgressBar = pb;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {

            // TODO 5: Get the sensor type
            case Sensor.TYPE_LINEAR_ACCELERATION:

            // TODO 6: Get sensor's values
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            //////////////////////////// -- PRINT ACC VALUES -- ////////////////////////////////////
            // TODO 7: Uncomment the following code

            // Timestamp
            long timeInMillis = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000;

            // Convert the timestamp to date
            SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            jdf.setTimeZone(TimeZone.getTimeZone("GMT+2"));
            String date = jdf.format(timeInMillis);

            // print a value every 1000 ms
            long curTime = System.currentTimeMillis();
            if ((curTime - lastUpdate) > 1000) {
                lastUpdate = curTime;

                Log.d("ACC", "X: " + String.valueOf(x) + " Y: " + String.valueOf(y) + " Z: "
                        + String.valueOf(z) + " t: " + String.valueOf(date));

            }
            ////////////////////////////////////////////////////////////////////////////////////////

            // TODO 8: Compute the ACC magnitude

            accMag = Math.sqrt(Math.pow(x,2) + Math.pow(y,2) + Math.pow(z, 2));
            //Update the Magnitude series
            mACCSeries.add((int) accMag);

            /// STEP COUNTER ACC ////
            peakDetection();

            break;

            // Case Step detector
            case Sensor.TYPE_STEP_DETECTOR:
                countSteps(event.values[0]);

            break;


        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private void peakDetection() {
        int windowSize = 20;

        /* Peak detection algorithm derived from: A Step Counter Service for Java-Enabled Devices Using a Built-In Accelerometer, Mladenov et al.
         */
        int highestValX = mACCSeries.size(); // get the length of the series
        if (highestValX - lastXPoint < windowSize) { // if the segment is smaller than the processing window skip it
            return;
        }

        List<Integer> valuesInWindow = mACCSeries.subList(lastXPoint,highestValX);

        lastXPoint = highestValX;

        int forwardSlope = 0;
        int downwardSlope = 0;

        List<Integer> dataPointList = new ArrayList<Integer>();

        for (int p =0; p < valuesInWindow.size(); p++){
            dataPointList.add(valuesInWindow.get(p));
        }


        for (int i = 0; i < dataPointList.size(); i++) {
            if (i == 0) {
            }
            else if (i < dataPointList.size() - 1) {
                forwardSlope = dataPointList.get(i + 1) - dataPointList.get(i);
                downwardSlope = dataPointList.get(i)- dataPointList.get(i - 1);

                if (forwardSlope < 0 && downwardSlope > 0 && dataPointList.get(i) > stepThreshold ) {
                    mACCStepCounter += 1;
                    Log.d("ACC STEPS: ", String.valueOf(mACCStepCounter));

                    //TODO 12: update the text view
                    stepsCountTextView.setText(String.valueOf(mACCStepCounter));
                    stepsCountProgressBar.setProgress(mACCStepCounter);

                }
            }
        }
    }

    // Calculate the number of steps from the step detector
    private void countSteps(float step) {
        mAndroidStepCount += 1;
        Log.d("Step Detector ", "steps: " + String.valueOf(mAndroidStepCount));

    }

    public static void enable_acc(){
        stepsCountTextView.setText(String.valueOf(mACCStepCounter));
        stepsCountProgressBar.setProgress(mACCStepCounter);
    };

    public static void enable_sd(){
        stepsCountTextView.setText(String.valueOf(mAndroidStepCount));
        stepsCountProgressBar.setProgress(mAndroidStepCount);
    };

    public static void resetSteps(){
        mACCStepCounter = 0;
        mAndroidStepCount = 0;
    };
}


