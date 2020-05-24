package com.joyusing.finger;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.bean.fingerBean;
import com.jy.finger.Common.Finger;
import com.jy.finger.Common.FpCommon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;


public class MainActivity extends Activity {
    private RadioGroup mRgColorGroup;
    private TextView mTvStatus, mTvFeatureByte, mTvFeature64, mTvFeature0, mTvFeature1, mTvFeature2, mTvCompared;
    private EditText mEtMatch;
    private ImageView mIvFinger;
    private Context mContext;
    private byte[] mFeatureByteByte = null;
    private byte[] mFeatureByteBase64 = null;
    private byte[] mFeatureByte0 = null;
    private byte[] mFeatureByte1 = null;
    private byte[] mFeatureByte2 = null;
    private fingerBean mFingerInfo;
    private Bitmap fpBitmap;

    private static final int PHOTO_IMAGE_FEATURE = 1;
    private static final int PHOTO_IMAGE_TO_BASE64 = 2;
    private static final int PHOTO_IMAGE_TO_BYTES = 3;

    private static final int TASK_GET_PICTURE = 1;
    private static final int TASK_GET_EIGENVALUES1 = 2;
    private static final int TASK_GET_EIGENVALUES2 = 3;
    private static final int TASK_GET_QUALITY = 4;
    private static final int TASK_GET_OBJ = 5;
    private static final int TASK_GET_OBJ1 = 6;

    private boolean inOperation = false;

    private boolean isOpen;

    private String mBase64;
    private byte[] mBytes;
    private boolean isOpenFinger =false;
    private Handler fingerHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    Bitmap bitmap = (Bitmap) msg.obj;
                    mIvFinger.setImageBitmap(bitmap);
//                    getFingerImage();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();
            }
            //申请权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2222);

        } else {
            Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show();
            Log.e("111", "checkPermission: 已经授权！");
        }
        //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();
            }
            //申请权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2222);

        } else {
            Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show();
            Log.e("111", "checkPermission: 已经授权！");
        }


        initView();
        FpCommon.init(mContext);
    }

    private void initView() {
        mRgColorGroup = findViewById(R.id.rg_color);

        mTvStatus = findViewById(R.id.tv_status);
        mEtMatch = findViewById(R.id.et_match_value);
        mTvFeatureByte = findViewById(R.id.tv_feature_byte);
        mTvFeature64 = findViewById(R.id.tv_feature64);
        mTvFeature0 = findViewById(R.id.tv_feature0);
        mTvFeature1 = findViewById(R.id.tv_feature1);
        mTvFeature2 = findViewById(R.id.tv_feature2);
        mTvCompared = findViewById(R.id.tv_compared);

        mIvFinger = findViewById(R.id.iv_result);

        mRgColorGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_black:
                        FpCommon.setFingerColor(Finger.BLACK);
                        break;
                    case R.id.rb_red:
                        FpCommon.setFingerColor(Finger.RED);
                        break;
                }
            }
        });
    }

    private int getMatchValue() {
        String value = mEtMatch.getText().toString().trim();

        return TextUtils.isEmpty(value) ? 0 : Integer.valueOf(value);
    }

    /* 打开指纹设备 */
    public void openFinger(View view) {
        isOpen = FpCommon.openFpModule() == 0;
        mTvStatus.setText(isOpen ? "设备运行中" : "设备启动失败");
        if (isOpen)
            mEtMatch.setText(String.valueOf(FpCommon.getFingerMatchValue()));
    }

    /* 关闭指纹设备 */
    public void closeFinger(View view) {
        mIvFinger.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(mIvFinger.getDrawingCache());
        mIvFinger.setDrawingCacheEnabled(false);
        if (bitmap!=null){
            ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG,100,byteArrayOutputStream);
            File file=new File(Environment.getExternalStorageDirectory()+"/Finger.png");
            try {
                FileOutputStream fos=new FileOutputStream(file);
                fos.write(byteArrayOutputStream.toByteArray());
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        if (isOpen) {
            FpCommon.closeFpModule();
        }
        mTvStatus.setText("设备已关闭");
        isOpen = false;

        mTvCompared.setText("");
        FpCommon.stopGetImge();
        fpBitmap = null;
        mIvFinger.setImageBitmap(null);
        isOpenFinger =false;

    }

    /* 获取图片特征值 */
    public void getFeatureByImage(View view) {
        if (!canOperation()) {
            return;
        }
        goPhotoAlbum(PHOTO_IMAGE_FEATURE);
    }

    /* 转Base64 */
    public void toBase64(View view) {
        goPhotoAlbum(PHOTO_IMAGE_TO_BASE64);
    }

    /* 获取Base64特征值 */
    public void getFeatureByBase64(View view) {
        if (TextUtils.isEmpty(mBase64)) {
            Toast.makeText(this, "无需要解析的Base64值", Toast.LENGTH_LONG).show();
            return;
        }
        if (!canOperation()) {
            return;
        }
        inOperation = true;
        mFeatureByteBase64 = null;
        mFeatureByteBase64 = FpCommon.getFeatureByBase64(mBase64);
        Bitmap bitmap = null;
        if (mFeatureByteBase64 != null) {
            bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_success);
            UIThread(R.id.tv_feature64, "获取成功");
        } else {
            Toast.makeText(this, "请检查Base64是否正确", Toast.LENGTH_SHORT).show();
            bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_failure);
            UIThread(R.id.tv_feature64, "获取失败");
        }

        inOperation = false;
        if (bitmap != null) {
            mIvFinger.setImageBitmap(bitmap);
        } else {
            showToast("没有获取到特征值");
        }
        mTvCompared.setText("");
        bitmap = null;
    }

    /* 转Bytes */
    public void toBytes(View view) {
        goPhotoAlbum(PHOTO_IMAGE_TO_BYTES);
    }

    /* 获取Bytes特征值 */
    public void getFeatureByBytes(View view) {
        if (mBytes == null) {
            Toast.makeText(this, "无需要解析的Byte[]值", Toast.LENGTH_LONG).show();
            return;
        }
        if (!canOperation()) {
            return;
        }
        inOperation = true;
        mFeatureByteByte = null;
        mFeatureByteByte = FpCommon.getFeatureByBytes(mBytes);
        Bitmap bitmap = null;
        if (mFeatureByteByte != null) {
            bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_success);
            UIThread(R.id.tv_feature_byte, "获取成功");
        } else {
            Toast.makeText(this, "请检查Base64是否正确", Toast.LENGTH_SHORT).show();
            bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_failure);
            UIThread(R.id.tv_feature_byte, "获取失败");
        }

        inOperation = false;
        if (bitmap != null) {
            mIvFinger.setImageBitmap(bitmap);
        } else {
            showToast("没有获取到特征值");
        }
        mTvCompared.setText("");
        bitmap = null;
    }

    /* 获取特征值1 */
    public void getFeature1(View view) {
        if (!canOperation()) {
            return;
        }
        new MyAsyncTask().execute(TASK_GET_EIGENVALUES1);
        mIvFinger.setImageBitmap(null);
        mTvFeature1.setText("获取指纹中...");
        mTvCompared.setText("");
    }

    /* 获取特征值2 */
    public void getFeature2(View view) {
        if (!canOperation()) {
            return;
        }
        new MyAsyncTask().execute(TASK_GET_EIGENVALUES2);
        mIvFinger.setImageBitmap(null);
        mTvFeature2.setText("获取指纹中...");
        mTvCompared.setText("");
    }

    /* 指纹比对 */
    public void comparedFinger(View view) {
        if (!canOperation()) {
            return;
        }
        FpCommon.setFingerMatchValue(getMatchValue());
        mEtMatch.setText(String.valueOf(FpCommon.getFingerMatchValue()));
        inOperation = true;
        mIvFinger.setImageBitmap(null);
        boolean score = false;
        if (mFeatureByte1 != null && mFeatureByte1.length != 0 && mFeatureByte2 != null && mFeatureByte2.length != 0) {
            score = FpCommon.compareFpFeature(mFeatureByte1, mFeatureByte2);
        }
        inOperation = false;
        if (score) {
            mTvCompared.setTextColor(Color.GREEN);
            mTvCompared.setText("指纹比对成功");
            mIvFinger.setImageResource(R.mipmap.ic_success);
        } else {
            mTvCompared.setTextColor(Color.RED);
            mTvCompared.setText("指纹比对失败");
            mIvFinger.setImageResource(R.mipmap.ic_failure);
        }
    }

    /* 获取指纹比对值 */
    public void getCompareValue(View view) {
        if (!canOperation()) {
            return;
        }

        inOperation = true;
        mIvFinger.setImageBitmap(null);
        int score = -1;
        if (mFeatureByte1 != null && mFeatureByte1.length != 0 && mFeatureByte2 != null && mFeatureByte2.length != 0) {
            score = FpCommon.getCompareValue(mFeatureByte1, mFeatureByte2);
        }
        if (score != -1) {
            mTvCompared.setTextColor(Color.GREEN);
            mTvCompared.setText("指纹比对匹配值 = " + score);
            mIvFinger.setImageResource(R.mipmap.ic_success);
        } else {
            mTvCompared.setTextColor(Color.RED);
            mTvCompared.setText("指纹比对异常");
            mIvFinger.setImageResource(R.mipmap.ic_failure);
        }
        inOperation = false;
    }

    /* 图片比对 */
    public void comparedFingerByImage(View view) {
        if (!canOperation()) {
            return;
        }
        FpCommon.setFingerMatchValue(getMatchValue());
        mEtMatch.setText(String.valueOf(FpCommon.getFingerMatchValue()));
        inOperation = true;
        mIvFinger.setImageBitmap(null);
        boolean score = false;
        if (mFeatureByte0 != null && mFeatureByte0.length != 0 && ((mFeatureByte1 != null && mFeatureByte1.length != 0) || (mFeatureByte2 != null && mFeatureByte2.length != 0))) {
            if (mFeatureByte1 != null) {
                score = FpCommon.compareFpFeature(mFeatureByte0, mFeatureByte1);
            } else {
                score = FpCommon.compareFpFeature(mFeatureByte0, mFeatureByte2);
            }
        } else {
            showToast("图片特征值不能为空，且至少有一个指纹特征值");
        }
        inOperation = false;
        if (score) {
            mTvCompared.setTextColor(Color.GREEN);
            mTvCompared.setText("指纹比对成功");
            mIvFinger.setImageResource(R.mipmap.ic_success);
        } else {
            mTvCompared.setTextColor(Color.RED);
            mTvCompared.setText("指纹比对失败");
            mIvFinger.setImageResource(R.mipmap.ic_failure);
        }
    }

    /* Base64比对 */
    public void comparedFingerAndBase64(View view) {
        if (!canOperation()) {
            return;
        }
        FpCommon.setFingerMatchValue(getMatchValue());
        mEtMatch.setText(String.valueOf(FpCommon.getFingerMatchValue()));
        inOperation = true;
        mIvFinger.setImageBitmap(null);
        boolean score = false;
        if (mFeatureByteBase64 != null && mFeatureByteBase64.length != 0 && ((mFeatureByte1 != null && mFeatureByte1.length != 0) || (mFeatureByte2 != null && mFeatureByte2.length != 0))) {
            if (mFeatureByte1 != null) {
                score = FpCommon.compareFpFeature(mFeatureByteBase64, mFeatureByte1);
            } else {
                score = FpCommon.compareFpFeature(mFeatureByteBase64, mFeatureByte2);
            }
        } else {
            showToast("Base64特征值不能为空，且至少有一个指纹特征值");
        }
        inOperation = false;
        if (score) {
            mTvCompared.setTextColor(Color.GREEN);
            mTvCompared.setText("指纹比对成功");
            mIvFinger.setImageResource(R.mipmap.ic_success);
        } else {
            mTvCompared.setTextColor(Color.RED);
            mTvCompared.setText("指纹比对失败");
            mIvFinger.setImageResource(R.mipmap.ic_failure);
        }
    }

    /* Bytes比对 */
    public void comparedFingerWithBytes(View view) {
        if (!canOperation()) {
            return;
        }
        FpCommon.setFingerMatchValue(getMatchValue());
        mEtMatch.setText(String.valueOf(FpCommon.getFingerMatchValue()));
        inOperation = true;
        mIvFinger.setImageBitmap(null);
        boolean score = false;
        if (mFeatureByteByte != null && mFeatureByteByte.length != 0 && ((mFeatureByte1 != null && mFeatureByte1.length != 0) || (mFeatureByte2 != null && mFeatureByte2.length != 0))) {
            if (mFeatureByte1 != null) {
                score = FpCommon.compareFpFeature(mFeatureByteByte, mFeatureByte1);
            } else {
                score = FpCommon.compareFpFeature(mFeatureByteByte, mFeatureByte2);
            }
        } else {
            showToast("Bytes特征值不能为空，且至少有一个指纹特征值");
        }
        inOperation = false;
        if (score) {
            mTvCompared.setTextColor(Color.GREEN);
            mTvCompared.setText("指纹比对成功");
            mIvFinger.setImageResource(R.mipmap.ic_success);
        } else {
            mTvCompared.setTextColor(Color.RED);
            mTvCompared.setText("指纹比对失败");
            mIvFinger.setImageResource(R.mipmap.ic_failure);
        }
    }

    /* 获取指纹质量 */
    public void getFingerQuality(View view) {
        if (!canOperation()) {
            return;
        }
        mIvFinger.setImageBitmap(null);
        new MyAsyncTask().execute(TASK_GET_QUALITY);
        mTvCompared.setTextColor(Color.RED);
        mTvCompared.setText("请按压指纹...");
    }

    /* 指纹图片 */
    public void getFingerImage(View view) {
        if (!canOperation()) {
            return;
        }
        mIvFinger.setImageBitmap(null);
//        new MyAsyncTask().execute(TASK_GET_PICTURE);
        mTvCompared.setTextColor(Color.RED);
        mTvCompared.setText("请按压指纹...");
        isOpenFinger = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isOpenFinger) {
                    fpBitmap = FpCommon.getFpImage();
                    if (fpBitmap != null) {
                        Message message = Message.obtain();
                        message.what = 1;
                        message.obj = fpBitmap;
                        fingerHandler.sendMessage(message);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();


//        FpCommon.stopGetImge();
//        if (fpBitmap == null) {
//            fpBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_failure);
//
//        } else {
//            mIvFinger.setImageBitmap(fpBitmap);
//        }
//        mTvCompared.setText("");
//        fpBitmap = null;
    }

    /* 指纹对象 */
    public void getFingerObj0(View view) {
        if (!canOperation()) {
            return;
        }
        mIvFinger.setImageBitmap(null);
        new MyAsyncTask().execute(TASK_GET_OBJ);
        mTvCompared.setTextColor(Color.RED);
        mTvCompared.setText("请按压指纹...");
    }

    /* 指纹对象 */
    public void getFingerObj1(View view) {
        if (!canOperation()) {
            return;
        }
        mIvFinger.setImageBitmap(null);
        new MyAsyncTask().execute(TASK_GET_OBJ1);
        mTvCompared.setTextColor(Color.RED);
        mTvCompared.setText("请按压指纹...");
    }

    private boolean canOperation() {
        if (!isOpen) {
            mTvStatus.setText("设备未启动");
            showToast("设备未启动");
            return false;
        }
        if (inOperation) {
            Log.e("Error", "inOperation...");
            showToast("其他操作尚未结束");
            return false;
        }
        return true;
    }

    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void UIThread(final int viewID, final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (viewID) {
                    case R.id.tv_feature_byte:
                        mTvFeatureByte.setText(msg);
                        break;
                    case R.id.tv_feature64:
                        mTvFeature64.setText(msg);
                        break;
                    case R.id.tv_feature0:
                        mTvFeature0.setText(msg);
                        break;
                    case R.id.tv_feature1:
                        mTvFeature1.setText(msg);
                        break;

                    case R.id.tv_feature2:
                        mTvFeature2.setText(msg);
                        break;
                }
            }
        });
    }

    private void goPhotoAlbum(int flag) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, flag);
    }

    class MyAsyncTask extends AsyncTask<Integer, Void, Object> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //TODO 此处可以添加 识别中的提示
            inOperation = true;
        }

        @Override
        protected Object doInBackground(Integer... integers) {

            switch (integers[0]) {
                case TASK_GET_PICTURE:
                    fpBitmap = FpCommon.getFpImage();
                    FpCommon.stopGetImge();
                    if (fpBitmap == null) {
                        fpBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_failure);
                        return fpBitmap;
                    }
                    return fpBitmap;
                case TASK_GET_EIGENVALUES1:
                    mFeatureByte1 = null;
                    mFeatureByte1 = FpCommon.getFpFeature();
                    if (mFeatureByte1 != null) {
                        fpBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_success);
                        UIThread(R.id.tv_feature1, "获取成功");
                    } else {
                        fpBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_failure);
                        UIThread(R.id.tv_feature1, "获取失败");
                    }
                    return fpBitmap;
                case TASK_GET_EIGENVALUES2:
                    mFeatureByte2 = null;
                    mFeatureByte2 = FpCommon.getFpFeature();
                    if (mFeatureByte2 != null) {
                        fpBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_success);
                        UIThread(R.id.tv_feature2, "获取成功");
                    } else {
                        fpBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_failure);
                        UIThread(R.id.tv_feature2, "获取失败");
                    }
                    return fpBitmap;
                case TASK_GET_QUALITY:
                    mFingerInfo = null;
                    mFingerInfo = FpCommon.getFingerInfo();
                    if (mFingerInfo != null) {
                        fpBitmap = mFingerInfo.getFpBitmap();
                        byte[] fpFeature = mFingerInfo.getFpFeature();
                        if (fpBitmap == null)
                            fpBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_success);
                        UIThread(R.id.tv_feature2, "指纹质量获取成功： " + mFingerInfo.getQuality());
                    } else {
                        fpBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_failure);
                        UIThread(R.id.tv_feature2, "获取失败");
                    }
                    return fpBitmap;
                case TASK_GET_OBJ:
                    order(0);
                    return fpBitmap;
                case TASK_GET_OBJ1:
                    order(1);
                    return fpBitmap;
                default:
                    return null;
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            fpBitmap = (Bitmap) o;
            inOperation = false;
            if (fpBitmap != null) {
                mIvFinger.setImageBitmap(fpBitmap);
            } else {
                showToast("没有获取到指纹");
            }
            mTvCompared.setText("");
            fpBitmap = null;
        }

        private void order(int order) {
            mFingerInfo = null;
            mFingerInfo = FpCommon.getFingerObj(order);
            if (mFingerInfo != null) {
                fpBitmap = mFingerInfo.getFpBitmap();
                if (fpBitmap == null)
                    fpBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_success);
                UIThread(R.id.tv_feature2, "指纹对象获取成功： [质量值：" + mFingerInfo.getQuality() + ", 特征数值大小：" + mFingerInfo.getFpFeature().length + "]");
            } else {
                fpBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_failure);
                UIThread(R.id.tv_feature2, "获取失败");
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.e("Result", "Result = " + resultCode);
        Log.e("Result", "Result = " + requestCode);
        if (resultCode == RESULT_OK) {
            String photoPath = AlbumTools.getRealPathFromUri(this, data.getData());
            switch (requestCode) {
                case PHOTO_IMAGE_FEATURE:
                    inOperation = true;
                    mFeatureByte0 = null;
                    mFeatureByte0 = FpCommon.getFeatureByImage(photoPath);
                    Bitmap bitmap = null;
                    if (mFeatureByte0 != null) {
                        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_success);
                        UIThread(R.id.tv_feature0, "获取成功");
                    } else {
                        Toast.makeText(this, "请检查图片是否存在", Toast.LENGTH_SHORT).show();
                        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_failure);
                        UIThread(R.id.tv_feature0, "获取失败");
                    }

                    inOperation = false;
                    if (bitmap != null) {
                        mIvFinger.setImageBitmap(bitmap);
                    } else {
                        showToast("没有获取到特征值");
                    }
                    mTvCompared.setText("");
                    bitmap.recycle();
                    bitmap = null;
                    break;
                case PHOTO_IMAGE_TO_BASE64:
                    Bitmap base64Bitmap = BitmapFactory.decodeFile(photoPath);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    base64Bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    byte[] bytes1 = baos.toByteArray();

                    //将byte[]转为base64
                    mBase64 = Base64.encodeToString(bytes1, Base64.DEFAULT);
                    Toast.makeText(this, "转换成功", Toast.LENGTH_SHORT).show();
                    base64Bitmap.recycle();
                    base64Bitmap = null;
                    break;
                case PHOTO_IMAGE_TO_BYTES:
                    Bitmap bytesBitmap = BitmapFactory.decodeFile(photoPath);
                    ByteArrayOutputStream bytesBaos = new ByteArrayOutputStream();
                    bytesBitmap.compress(Bitmap.CompressFormat.PNG, 100, bytesBaos);
                    mBytes = bytesBaos.toByteArray();
                    Toast.makeText(this, "转换成功", Toast.LENGTH_SHORT).show();
                    bytesBitmap.recycle();
                    bytesBitmap = null;
                    break;
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        if (isOpen) {
            FpCommon.closeFpModule();
        }
        isOpen = false;
        super.onDestroy();
    }
}
