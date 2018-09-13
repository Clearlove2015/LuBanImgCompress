package com.odbpo.fenggou.lubanimgcompress;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.odbpo.fenggou.lubanimgcompress.util.FileSizeUtil;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.functions.Action1;
import top.zibin.luban.CompressionPredicate;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.btn_selector_img)
    Button btnSelectorImg;
    @Bind(R.id.tv_img_path)
    TextView tvImgPath;
    @Bind(R.id.iv_img)
    ImageView ivImg;
    @Bind(R.id.iv_img_old)
    ImageView ivImgOld;
    @Bind(R.id.tv_img_size_old)
    TextView tvImgSizeOld;
    @Bind(R.id.tv_img_size)
    TextView tvImgSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        requestPermission();
    }

    /**
     * RxPermission版本使用0.9.1正常运行，如果使用0.7.0会出现异常（android.os.FileUriExposedException: file:///storage/emulated/0/）
     */
    public void requestPermission() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) {
                            //当所有权限都允许之后，返回true
                            Log.i("permissions", "btn_more_sametime：" + aBoolean);
                        } else {
                            //只要有一个权限禁止，返回false，
                            //下一次申请只申请没通过申请的权限
                            Log.i("permissions", "btn_more_sametime：" + aBoolean);
                        }
                    }
                });
    }

    @OnClick(R.id.btn_selector_img)
    public void onViewClicked() {
        //调用相册
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");//图片类型
        //intent.setType("*/*");//任意类型
        startActivityForResult(intent, 1004);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1004) {
            if (resultCode == RESULT_OK) {
                //获取图片路径
                Uri selectedImage = data.getData();
                String[] filePathColumns = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePathColumns[0]);
                String imagePath = c.getString(columnIndex);
                tvImgPath.setText("文件路径：" + imagePath);
                System.out.println("path:" + imagePath);
                System.out.println("压缩前图片大小：" + FileSizeUtil.getFileOrFilesSize(imagePath, FileSizeUtil.SIZETYPE_KB) + "KB");
                tvImgSizeOld.setText("压缩前图片大小：" + FileSizeUtil.getFileOrFilesSize(imagePath, FileSizeUtil.SIZETYPE_KB) + "KB");
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                ivImgOld.setImageBitmap(bitmap);

                //压缩图片
                compressImg(imagePath);
                c.close();
            }
        }
    }

    public void compressImg(String imagePath) {
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        File TARGET_DIR = new File(root + File.separator + "LuBan_cache");
        if (!TARGET_DIR.exists()) {
            TARGET_DIR.mkdirs();
        }

        Luban.with(this)
                .load(imagePath)
                .ignoreBy(100)//不压缩的阈值，单位为K
                .setTargetDir(TARGET_DIR.getPath())
                .filter(new CompressionPredicate() {
                    @Override
                    public boolean apply(String path) {
                        return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
                    }
                })
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                        // TODO 压缩开始前调用，可以在方法内启动 loading UI
                        Toast.makeText(MainActivity.this, "开始压缩", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(File file) {
                        // TODO 压缩成功后调用，返回压缩后的图片文件
                        double fileSize = FileSizeUtil.getFileOrFilesSize(file.getAbsolutePath(), FileSizeUtil.SIZETYPE_KB);
                        System.out.println("压缩后图片大小：" + fileSize + "KB");
                        tvImgSize.setText("压缩后图片大小：" + fileSize + "KB");
                        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                        ivImg.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(Throwable e) {
                        // TODO 当压缩过程出现问题时调用
                        System.out.println("图片压缩出错：" + e.getMessage());
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).launch();
    }

}
