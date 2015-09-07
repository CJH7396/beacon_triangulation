package com.example.hyeonseob.beacontriangulation.Activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hyeonseob.beacontriangulation.Class.Beacon;
import com.example.hyeonseob.beacontriangulation.Class.DBManager;
import com.example.hyeonseob.beacontriangulation.Class.DeviceManager;
import com.example.hyeonseob.beacontriangulation.Class.FileManager;
import com.example.hyeonseob.beacontriangulation.Class.KalmanFilter;
import com.example.hyeonseob.beacontriangulation.Class.LocationEstimation;
import com.example.hyeonseob.beacontriangulation.Class.TransCoordinate;
import com.example.hyeonseob.beacontriangulation.Intro.MainActivity;
import com.example.hyeonseob.beacontriangulation.R;
import com.example.hyeonseob.beacontriangulation.RECO.RECOActivity;
import com.perples.recosdk.RECOBeacon;
import com.perples.recosdk.RECOBeaconRegion;
import com.perples.recosdk.RECOErrorCode;
import com.perples.recosdk.RECORangingListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class MultipleUserActivity extends RECOActivity implements RECORangingListener {
    public final static int WINDOW_SIZE = 20;
    private final static int INITIAL_STATE = 0, BEACON_AND_SENSOR_STATE = 1, SENSOR_ONLY_STATE = 2, IN_SECTION_STATE = 3;
    private final static int[] ROTATION_REVISION = {240, 240, 240, 240};
    private final static float[] MD_CONST_REVISION = {0.6f, 0.6f, 0.68f, 0.65f};
    private final static int[][] LOCATION = {{5,62},{8,62},{12,62},{16,62},{20,63},{24,63},{28,63},{32,63},{37,63},{42,63},{46,63},{49,63},{53,73},{60,79},
            {43,59},{43,56},{44,52},{44,49},{44,45},{44,42},{44,38},{44,35},{44,31},{44,27},{44,24},{44,19},{44,14},{44,10},{44,5},{16,26},{16,29},{16,33}};

    private int mCurrentState = INITIAL_STATE;
    private float mX, mY, mDX, mDY;
    private int mMajor, mMinor, mRSSI, mMapHeight, mMapWidth, mRangeSize, mRangeMargin;
    private int mCWBound, mCHBound, mBWBound, mBHBound, mRWBound, mRHBound;
    private double mBlockWidth, mBlockHeight;
    private int[][] mBeaconFlag = {{0,0,0}, {0,0,0}, {0,0,0}, {0,0,0}, {0,0,0}};
    private int[][][] mFingerprint;
    private int[] mResult;
    private double[] mResult2;
    //private float[][][] mSectionRegion = {{{12,0},{36,20}},{{48,0},{70,57}}};
    private Vector<Beacon> mBeaconList;
    private List<RECOBeacon> mBeaconCollect;

    private ImageView mMapImageView, mRangeView;
    private StringBuffer mStrBuff;
    private RelativeLayout mMapLayout;
    private TextView mRSSITextView, mStatusTextView;
    private RelativeLayout.LayoutParams mLayoutParams;

    private LocationEstimation mLocEst;
    private TransCoordinate mTransCoord;
    private DBManager mDBManager;
    private FileManager mFileManager;
    private DeviceManager mDeviceManager;
    private int mDeviceNum;

    //위치그리기
    private RelativeLayout Linear;
    private LocationView lm;

    float wid_dis;
    float hei_dis;

    //함수 내부 전역변수
    //하이패스필터
    float HPFconst = 0.01f;
    float HPF_prev = 10000;

    //무빙 에버리지 필터
    float[] MAF_Data;
    private int windowSize = 5;
    int MAF_count = 0;
    int MAF_num = 0;

    //Max_Min Check
    float MM_input_prev = 1000;
    float MM_output_prev = 0;
    float MM_output_value = 0;
    float MM_const = 0;

    //이동거리 함수
    float MD_input_prev = 0.0f;
    float MD_dis_prev = 0.0f;//이전 이동거리
    int MD_num = 0; //걸음수

    //float MD_Const = 0.78f; //걸음 거리 상수 K

    float MD_Const = 0.55f; //노한민 노트4값
    //float MD_Const = 0.78f //빌린 S4 값
    double MD_Min_const = 0.005;

    //로우패스필터
    float LPF_input_prev = 10000;
    float LPF_output_prev = 10000;

    //Mapworking
    float MW_prev_x = 0;
    float MW_prev_y = 0;
    float MW_prev_dis = 0;


    float[] Outputdata = new float[2];
    //변수 선언부

    SensorManager sm;
    SensorEventListener accL;
    SensorEventListener magL;
    SensorEventListener gyroL;

    Sensor accSensor; // 가속도
    Sensor magSensor; // 자기
    Sensor gyroSensor; //회전

    float[] Mag_data = new float[3]; //지자기 데이터
    float[] Kalmag_data = new float[3];
    float[] Acc_data = new float[3]; // 칼만 가속 데이터
    float[] Kalacc_data = new float[3];
    float[] Gyro_data = new float[3]; //자이로 데이터
    float[] Ori_data = new float[3]; //방향 데이터
    float[] KalOri_data = new float[3]; //칼만 방향 데이터

    private KalmanFilter[] Kalman_acc = new KalmanFilter[3];
    private KalmanFilter[] Kalman_ori = new KalmanFilter[3];
    private KalmanFilter[] Kalman_mag = new KalmanFilter[3];


    int width, height; //화면의 폭과 높이
    float mCurrentX, mCurrentY; //이미지 현재 좌표
    float mBeaconX, mBeaconY, mSensorX, mSensorY;
    float mDegree, mDegree2;
    float dx, dy; //캐릭터가 이동할 방향과 거리
    int mCW, mCH, mBW, mBH; //캐릭터의 폭과 높이
    Bitmap mCharacterBitmap, mCRotateBitmap, mBeaconBitmap, mSensorBitmap;
    int Naviflag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_multiple_user);

        // Add LocationView
        Naviflag = 0;
        lm = new LocationView(this);
        Linear = (RelativeLayout) findViewById(R.id.Linear1);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        Linear.addView(lm, params);
        Linear.bringChildToFront(findViewById(R.id.statusLayout));
        Linear.invalidate();

        // Initilalize
        mDegree = 0.0f;
        mCurrentX = mCurrentY = /*mBeaconX = mBeaconY = mSensorX = mSensorY =*/ 0.0f;
        MAF_Data = new float[10];
        Kalman_acc[0] = new KalmanFilter(0.0f);
        Kalman_acc[1] = new KalmanFilter(0.0f);
        Kalman_acc[2] = new KalmanFilter(0.0f);
        Kalman_ori[0] = new KalmanFilter(0.0f);
        Kalman_ori[1] = new KalmanFilter(0.0f);
        Kalman_ori[2] = new KalmanFilter(0.0f);
        Kalman_mag[0] = new KalmanFilter(0.0f);
        Kalman_mag[1] = new KalmanFilter(0.0f);
        Kalman_mag[2] = new KalmanFilter(0.0f);

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);

        accSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD); // 자력

        //gyroSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE); // 회전

        accL = new accListener();
        magL = new magListener();
        //gyroL = new gyroListener();

        mMapLayout = (RelativeLayout) findViewById(R.id.mapLayout);
        mMapImageView = (ImageView) findViewById(R.id.mapImageView);
        mRSSITextView = (TextView) findViewById(R.id.RSSITextView);
        mStatusTextView = (TextView) findViewById(R.id.statusTextView);

        // Add range view
        mLayoutParams = new RelativeLayout.LayoutParams(10,10);
        mRangeView = new ImageView(this);
        mRangeView.setImageResource(R.drawable.image_range);
        mRangeView.setAlpha(0.5f);
        mRangeView.setLayoutParams(mLayoutParams);
        mMapLayout.addView(mRangeView);

        mStrBuff = new StringBuffer();
        mTransCoord = new TransCoordinate();
        mDBManager = new DBManager();


        mMapImageView.post(new Runnable() {
            @Override
            public void run() {
                if (mMapHeight != 0)
                    return;

                mMapHeight = mMapImageView.getMeasuredHeight();
                mMapWidth = mMapImageView.getMeasuredWidth();
                Log.w("MAP", "" + mMapHeight + "," + mMapWidth);

                mTransCoord.setMapSize(mMapWidth, mMapHeight);
                mBlockWidth = mTransCoord.getBlockWidth();
                mBlockHeight = mTransCoord.getBLockHeight();
            }
        });


        // Check android id
        mDeviceManager = new DeviceManager(getApplicationContext());
        mDeviceNum = mDeviceManager.getDeviceNum();

        // Read mFingerprint from text file
        mFileManager = new FileManager(mDeviceManager.getDeviceString());
        mFingerprint = mFileManager.readFile();


        mLocEst = new LocationEstimation(mDeviceManager.getDeviceNum());
        mLocEst.setFingerprint(mFingerprint);
    }


    public void updatedata()
    {
        float Vectordata = GetEnergy(Kalacc_data[0], Kalacc_data[1], Kalacc_data[2]);
        mDegree = ((float)(Math.toDegrees(KalOri_data[0]) +360 +ROTATION_REVISION[mDeviceNum]) % 360);
//      mDegree = ((float)(Math.toDegrees(Ori_data[0]) +360 + 240) % 360);
        mDegree = GetLPFdata(mDegree);

        Vectordata = GetHPFdata(Vectordata);
        Vectordata = MovingAverageFilter(Vectordata);
        Vectordata = Max_Min_check(Vectordata);
        Vectordata = Moving_Distance(Vectordata);

        /*
        // Revise degree in section area
        if( (mSectionRegion[0][0][0] <= mCurrentX && mCurrentX <= mSectionRegion[0][1][0] && mSectionRegion[0][0][1] <= mCurrentY && mCurrentY <= mSectionRegion[0][1][1]) ||
                (mSectionRegion[1][0][0] <= mCurrentX && mCurrentX <= mSectionRegion[1][1][0] && mSectionRegion[1][0][1] <= mCurrentY && mCurrentY <= mSectionRegion[1][1][1]) ) {
            mStatusTextView.setText("섹션 영역 안입니다.");
            mCurrentState = IN_SECTION_STATE;
            if(mDegree <= 90)
                mDegree2 = 0;
            else if(mDegree <= 270)
                mDegree2 = 180;
            else
                mDegree2 = 0;
        }
        else{
            mCurrentState = BEACON_AND_SENSOR_STATE;
            mDegree2 = mDegree;
        }
        */

        Outputdata = Cal_Mapworking(Vectordata, mDegree);
        dx = ((Outputdata[0]) /  wid_dis);
        dy = ((Outputdata[1]) /  hei_dis);
        //  if(dx != 0 || dy!= 0){
        //   Log.i("Tag", dx + " " + dy + " ");
        //   }

    }

    @Override
    public void onResume(){
        super.onResume();
        sm.registerListener(accL, accSensor, SensorManager.SENSOR_DELAY_GAME);
        //sm.registerListener(magL, magSensor, SensorManager.SENSOR_DELAY_FASTEST);//40ms // 자력
        sm.registerListener(magL, magSensor, SensorManager.SENSOR_DELAY_UI);//40ms // 자력
        //sm.registerListener(gyroL, gyroSensor, SensorManager.SENSOR_DELAY_GAME);//20ms // 회전
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(accL);
        sm.unregisterListener(magL);
        //sm.unregisterListener(gyroL);
    }

    public class LocationView extends View {
        public LocationView(Context context){
            super(context);
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            width = metrics.widthPixels;
            height = metrics.heightPixels;

            wid_dis = (float)(3054.1 / width);
            hei_dis = (float)(3724.5 / height);

            // Set section region coordinates
            /*
            mSectionRegion[0][0][0] = (width / 70) * 20;
            mSectionRegion[0][0][1] = 0;
            mSectionRegion[0][1][0] = (width / 70) * 44;
            mSectionRegion[0][1][1] = (height / 90) * 37;
            mSectionRegion[1][0][0] = (width / 70) * 48;
            mSectionRegion[1][0][1] = 0;
            mSectionRegion[1][1][0] = width;
            mSectionRegion[1][1][1] = (height / 90) * 57;
            Log.i("MAP","Section: "+mSectionRegion[0][0][0]+","+mSectionRegion[0][0][1]);
            */

            dx = dy = 0;
            mCharacterBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_location);
            //mBeaconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.blue_button);
            //mSensorBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.red_button);

            mCRotateBitmap = Bitmap.createScaledBitmap(mCharacterBitmap, 60, 60, false);
            //mBeaconBitmap = Bitmap.createScaledBitmap(mBeaconBitmap, 40, 40, false);
            //mSensorBitmap = Bitmap.createScaledBitmap(mSensorBitmap, 40, 40, false);
            mCW = mCRotateBitmap.getWidth()/2;
            mCH = mCRotateBitmap.getHeight()/2;
            //mBW = mBeaconBitmap.getWidth()/2;
            //mBH = mBeaconBitmap.getHeight()/2;

            mCWBound = width - mCRotateBitmap.getWidth();
            mCHBound = height - mCRotateBitmap.getHeight();
            //mBWBound = width - mBeaconBitmap.getWidth();
            //mBHBound = height - mBeaconBitmap.getHeight();

            mHandler.sendEmptyMessageDelayed(0, 20);
        }

        public void onDraw(Canvas canvas){
            /*
            if( (mSectionRegion[0][0][0] <= mCurrentX && mCurrentX <= mSectionRegion[0][1][0] && mSectionRegion[0][0][1] <= mCurrentY && mCurrentY <= mSectionRegion[0][1][1]) ||
                    (mSectionRegion[1][0][0] <= mCurrentX && mCurrentX <= mSectionRegion[1][1][0] && mSectionRegion[1][0][1] <= mCurrentY && mCurrentY <= mSectionRegion[1][1][1]) )
                mStatusTextView.setText("섹션 영역 안입니다.");
            else
                mStatusTextView.setText("섹션 영역 밖입니다.");
            */

            // Do not move until start
            if(mCurrentState != INITIAL_STATE){
                updatedata();
                mCurrentX = Math.min(Math.max(mCurrentX-dx,0),mCWBound);
                mCurrentY = Math.min(Math.max(mCurrentY-dy,0),mCHBound);
                //mSensorX = Math.min(Math.max(mSensorX-dx,0),mBWBound);
                //mSensorY =  Math.min(Math.max(mSensorY-dy,0),mBHBound);
            }

            //canvas.drawBitmap(mBeaconBitmap, mBeaconX-mBW, mBeaconY-mBH, null);
            //canvas.drawBitmap(mSensorBitmap, mSensorX-mBW, mSensorY-mBH, null);
            canvas.drawBitmap(mCRotateBitmap, mCurrentX-mCW, mCurrentY-mCH, null);

            mRangeView.setX(Math.min(Math.max(mCurrentX, mCW), mCWBound+mCW) - mRangeMargin);
            mRangeView.setY(Math.min(Math.max(mCurrentY, mCH), mCHBound+mCH) - mRangeMargin);
            mMapLayout.updateViewLayout(mRangeView, mLayoutParams);
            Naviflag++;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if(mCurrentState == INITIAL_STATE) {
                mCurrentX = /*mSensorX = mBeaconX =*/ event.getX();
                mCurrentY = /*mSensorY = mBeaconY =*/ event.getY();
                mStatusTextView.setText("출발 지점을 설정했습니다. ("+(int)mCurrentX+","+(int)mCurrentY+")");
            }
            else {
                mStatusTextView.setText("측정 중에는 설정할 수 없습니다.");
            }
            return true;
        }

        Handler mHandler = new Handler() {               // 타이머로 사용할 Handler
            public void handleMessage(Message msg) {
                invalidate();                              // onDraw() 다시 실행
                mHandler.sendEmptyMessageDelayed(0, 20); // 10/1000초마다 실행
            }
        }; // Handler
    }

    public void onButtonClicked(View v){
        if(v.getId() == R.id.button)
        {
            if(mCurrentState == INITIAL_STATE)
            {
                if(mCurrentX == 0)
                {
                    Toast.makeText(this,"시작 위치를 터치하세요.",Toast.LENGTH_SHORT).show();
                    mStatusTextView.setText("시작 위치를 터치하세요.");
                    return;
                }

                mCurrentState = SENSOR_ONLY_STATE;
                ((Button) v).setText("STOP");
                mStatusTextView.setText("측정을 시작합니다.");
                mRecoManager.setRangingListener(this);
                mRecoManager.bind(this);
            }
            else{
                mCurrentState = INITIAL_STATE;
                ((Button) v).setText("START");
                mStatusTextView.setText("측정을 중단했습니다.");
                this.stop(mRegions);
                this.unbind();
            }
        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<RECOBeacon> collection, RECOBeaconRegion recoBeaconRegion) {
        Log.i("BEACON","didRangeBeaconinRegion: "+collection.size());
        mBeaconList = new Vector<>();
        mStrBuff.setLength(0);
        mStrBuff.append("RSSI:\n");
        try {
            for (RECOBeacon beacon : collection) {
                mMajor = beacon.getMajor() - 1;
                mMinor = beacon.getMinor();
                mRSSI = beacon.getRssi();

                /*
                mStrBuff.append("BeaconID: (");
                mStrBuff.append(mMajor + 1);
                mStrBuff.append(",");
                mStrBuff.append(mMinor);
                mStrBuff.append("), RSSI: ");
                mStrBuff.append(mRSSI);
                mStrBuff.append("\n");
                */

                mBeaconList.add(new Beacon(mMajor * 3 + mMinor, mMajor, mMinor, mRSSI));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.i("BEACON","Collection error!");
            return;
        }
        mStrBuff.append("Degree: ");
        mStrBuff.append(mDegree);
        mRSSITextView.setText(mStrBuff.toString());
        mDBManager.setTextView(mStatusTextView);

        // No revise in section
        /*
        if(mCurrentState == IN_SECTION_STATE && mDegree >= 145 && mDegree <= 215)
            return;
        */

        mResult = mLocEst.getLocation(mBeaconList, (int) mDegree);
        if(mResult == null)
            return;

        mResult2 = mTransCoord.getPixelPoint(mResult[0], mResult[1]);
        //mBeaconX = (float)mResult2[0];
        //mBeaconY = (float)mResult2[1];
        mCurrentX = (float)mResult2[0];
        mCurrentY = (float)mResult2[1];

        mRangeSize = Math.min(mResult[2]/80000,500);
        mRangeMargin = mRangeSize/2;
        mLayoutParams.width = mRangeSize;
        mLayoutParams.height = mRangeSize;
        mStatusTextView.setText("MIN: " + mResult[2]);

        //mCurrentX = mBeaconX;
        //mCurrentY = mBeaconY;
        /*
        if(mResult[2] < RSSI_THRESHOLD)
        {
            mCurrentState = BEACON_AND_SENSOR_STATE;
            mCurrentX = mBeaconX;
            mCurrentY = mBeaconY;
            mStatusTextView.setText("비콘으로 보정중입니다.");
        }
        else
        {
            mCurrentState = SENSOR_ONLY_STATE;
            mStatusTextView.setText("센서로만 측정중입니다.");
        }
        */
    }

    private class accListener implements SensorEventListener{
        @Override
        public void onSensorChanged(SensorEvent event) {
            Acc_data = event.values.clone();
            //   gravity_data = event.values.clone();

            Kalacc_data[0] = (float)Kalman_acc[0].update(Acc_data[0]);
            Kalacc_data[1] = (float)Kalman_acc[1].update(Acc_data[1]);
            Kalacc_data[2] = (float)Kalman_acc[2].update(Acc_data[2]);

            if (Kalacc_data != null && Mag_data != null) {
                float[] R = new float[16];
                SensorManager.getRotationMatrix(R, null, Kalacc_data, Kalmag_data);

                SensorManager.getOrientation(R, Ori_data);
                KalOri_data[0] = (float)Kalman_ori[0].update(Ori_data[0]);
                KalOri_data[1] = (float)Kalman_ori[1].update(Ori_data[1]);
                KalOri_data[2] = (float)Kalman_ori[2].update(Ori_data[2]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

    }

    private class magListener implements SensorEventListener{
        @Override
        public void onSensorChanged(SensorEvent event) {
            Mag_data = event.values.clone();

            Kalmag_data[0] = (float)Kalman_mag[0].update(Mag_data[0]);
            Kalmag_data[1] = (float)Kalman_mag[1].update(Mag_data[1]);
            Kalmag_data[2] = (float)Kalman_mag[2].update(Mag_data[2]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

    }

    /*
    private class gyroListener implements SensorEventListener{
        @Override
        public void onSensorChanged(SensorEvent event) {
            Gyro_data = event.values.clone();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
    */

    protected float GetEnergy(float x, float y, float z){
        return (float)Math.sqrt((x*x + y*y + z*z));
    }

    protected float GetHPFdata(float inputdata){
        float Output;

        if(HPF_prev == 10000)
        {
            Output = 0;
            HPF_prev = inputdata;

        }else
        {
            Output = inputdata - HPF_prev;
            HPF_prev = (inputdata * (1 - HPFconst)) + (HPF_prev * HPFconst);
        }

        return Output;
    }

    protected float MovingAverageFilter(float inputdata){

        float Output = 0.0f;

        MAF_Data[MAF_num++] = inputdata;

        if(MAF_num >= windowSize)
            MAF_num = 0;

        if(MAF_count < windowSize)
            MAF_count++;

        for(int i=0; i<windowSize; i++)
        {
            Output += MAF_Data[i];
        }

        Output /= MAF_count;

        return Output;
    }

    protected float Max_Min_check(float inputdata){
        float Output;

        if(MM_input_prev == 1000){
            Output = 0;
            MM_output_value = inputdata;
        }else{
            if(inputdata * MM_input_prev < 0){
                if(Math.abs(MM_output_value) > MM_const){
                    Output = MM_output_value;
                    MM_output_value = 0;
                }else{
                    Output = 0;
                    MM_output_value = 0;
                }
            }else{
                if(Math.abs(MM_output_value) < Math.abs(inputdata)){
                    Output = 0;
                    MM_output_value = inputdata;
                }else{
                    Output = 0;
                }
            }
        }
        MM_input_prev = inputdata;
        return Output;
    }

    protected float Moving_Distance(float inputdata){

        float Output;

        if(Math.abs(inputdata) > 0.02)//0.02
        //if(inputdata != 0)
        {
            if(MD_input_prev == 0){
                MD_input_prev = inputdata;
            }else{
                MD_dis_prev = (float)(MD_dis_prev + MD_CONST_REVISION[mDeviceNum] * Math.sqrt(Math.sqrt((Math.abs(inputdata)+Math.abs(MD_input_prev)))));
                MD_input_prev = 0;
                MD_num++;
            }
        }
        Output = MD_dis_prev;
        return Output;
    }

    protected float GetLPFdata(float inputdata){
        float Output;

        if(LPF_input_prev == 10000) {
            Output = inputdata;
        }
        else {
            if (Math.abs(inputdata - LPF_input_prev) < 250)
                Output = (float)(0.9355*LPF_output_prev + 0.0323*inputdata + 0.0323* LPF_input_prev);
            else
                Output = inputdata;
        }
        LPF_input_prev = inputdata;
        LPF_output_prev = Output;
        return Output;
    }

    protected float[] Cal_Mapworking(float distance, float angle){
        float[] Output = new float[2];

        if(MW_prev_dis == distance){
            Output[0] = 0;
            Output[1] = 0;
        }else{
            if(distance - MW_prev_dis > 0.35) {
                Output[0] = (float) (((distance - MW_prev_dis) * 100) * Math.cos(angle * Math.PI / 180));
                Output[1] = (float) (((distance - MW_prev_dis) * 100) * Math.sin(angle * Math.PI / 180));
            }
            MW_prev_dis = distance;
        }

        MW_prev_x = Output[0];
        MW_prev_y = Output[1];

        return Output;
    }

    @Override
    public void onServiceConnect() {
        Log.i("RECORangingActivity", "onServiceConnect()");
        mRecoManager.setDiscontinuousScan(MainActivity.DISCONTINUOUS_SCAN);
        this.start(mRegions);
    }

    @Override
    public void rangingBeaconsDidFailForRegion(RECOBeaconRegion recoBeaconRegion, RECOErrorCode recoErrorCode) {}

    @Override
    public void onServiceFail(RECOErrorCode recoErrorCode) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.stop(mRegions);
        this.unbind();
    }

    private void unbind() {
        try {
            mRecoManager.unbind();
        } catch (RemoteException e) {
            Log.i("RECORangingActivity", "Remote Exception");
            e.printStackTrace();
        }
    }

    @Override
    protected void start(ArrayList<RECOBeaconRegion> regions) {
        for(RECOBeaconRegion region : regions) {
            try {
                mRecoManager.startRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                Log.i("RECORangingActivity", "Remote Exception");
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.i("RECORangingActivity", "Null Pointer Exception");
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void stop(ArrayList<RECOBeaconRegion> regions) {
        for(RECOBeaconRegion region : regions) {
            try {
                mRecoManager.stopRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                Log.i("RECORangingActivity", "Remote Exception");
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.i("RECORangingActivity", "Null Pointer Exception");
                e.printStackTrace();
            }
        }
    }
}