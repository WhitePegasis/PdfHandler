package com.example.pdfhandler;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.example.pdfhandler.databinding.ActivityImageToPdfConverterBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class ImageToPdfConverter extends AppCompatActivity {

    ActivityImageToPdfConverterBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    private ArrayList<Uri> imageUris;
    int position=0;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityImageToPdfConverterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        imageUris=new ArrayList<>();

        //creating a scroll view for images selected
        binding.imageview.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView imageView=new ImageView(getApplicationContext());
                return imageView;
            }
        });

        activityResultLauncher= registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode()== RESULT_OK && result.getData()!=null){
                            if(result.getData().getClipData()!=null){
                                binding.textview.setText("Preview");
                                //picked multiple images
                                int count=result.getData().getClipData().getItemCount();
                                for(int i=0;i<count;i++)
                                {
                                    Uri uri=result.getData().getClipData().getItemAt(i).getUri();
                                    imageUris.add(uri);

                                }
                                binding.imageview.setImageURI(imageUris.get(0));
                                position=0;

                            }
                            else{
                                binding.textview.setText("Preview");
                                //picked single images
                                Uri uri=result.getData().getData();
                                imageUris.add(uri);
                                binding.imageview.setImageURI(imageUris.get(0));
                                position=0;
                            }

                        }
                    }
                });
        binding.selectImage.setOnClickListener(view -> {
            /*Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);*/
            /*Intent intent=new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);*/ // for capturing image through camera
            Intent intent=new Intent();
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            if(intent.resolveActivity(getPackageManager())!=null){
                imageUris.clear();
                activityResultLauncher.launch(Intent.createChooser(intent,"Select Image(s)"));
            }
            else{
                Toast.makeText(this, "No app that supports this action!!!", Toast.LENGTH_SHORT).show();
            }
        });

        ////scroll imageview left
        binding.leftArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(position>0){
                    position--;
                    binding.imageview.setImageURI(imageUris.get(position));
                }
                else
                    Toast.makeText(ImageToPdfConverter.this, "No previous image", Toast.LENGTH_SHORT).show();
            }
        });

        //scroll imageview right
        binding.rightArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(position<imageUris.size()-1){
                    position++;
                    binding.imageview.setImageURI(imageUris.get(position));
                }
                else
                    Toast.makeText(ImageToPdfConverter.this, "No more images", Toast.LENGTH_SHORT).show();
            }
        });

        ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);


        progressDialog=new ProgressDialog(ImageToPdfConverter.this);
        progressDialog.setTitle("Converting to Pdf");
        progressDialog.setMessage("Your file is being converted, please wait for few seconds!");

        //on ConvertToPdf button clicked call method createPDFWithMultipleImage()
        binding.ConvertToPdf.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if(imageUris.size()!=0)
                {
                    try {

                        createPDFWithMultipleImage();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    Toast.makeText(ImageToPdfConverter.this, "No image selected! Please select an image first.", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    /*public void createPdf(){
            Bitmap bm=BitmapFactory.decodeResource(getResources(),R.drawable.left_arrow);
        //String filePath = directory+"ss.jpg";
            //Bitmap bm=BitmapFactory.decodeFile(filePath);
            PdfDocument myPdfDocument=new PdfDocument();
            Paint myPaint=new Paint();
            PdfDocument.PageInfo myPageInfo1=new PdfDocument.PageInfo.Builder(1200,800,1).create();
            PdfDocument.Page myPage1=myPdfDocument.startPage(myPageInfo1);
            Canvas canvas=myPage1.getCanvas();
            canvas.drawPaint(myPaint);
            canvas.drawBitmap(bm, 0f, 0f, myPaint);
            myPdfDocument.finishPage(myPage1);
            File file=new File(Environment.getExternalStorageDirectory(),"/con2Pdf.pdf");
            try {
                myPdfDocument.writeTo(new FileOutputStream(file));
                Toast.makeText(this, "Pdf Created in: "+Environment.getExternalStorageDirectory(), Toast.LENGTH_SHORT).show();
            } catch(IOException e){
                e.printStackTrace();
            }
            myPdfDocument.close();
        }
    }*/


    //create pdf with selected images
    private void createPDFWithMultipleImage() throws IOException {
        progressDialog.show();
        PdfDocument pdfDocument = new PdfDocument();

        for (int i = 0; i < imageUris.size(); i++){

            //converting image uri to bitmap
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUris.get(i));

            //creating a pdf document page
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), (i + 1)).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            //creating canvas for above page for editing the page
            Canvas canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);
            Paint paint = new Paint();
            canvas.drawPaint(paint);

            //canvas.drawBitmap(bitmapBg, 0f, 0f, paint);
            canvas.drawBitmap(bitmap, 0f, 0f, paint); //position of image
            pdfDocument.finishPage(page);
            bitmap.recycle();
        }

        try {
            //saving pdf in storage with unique filename
            Date date = new Date();
            File file=new File(Environment.getExternalStorageDirectory(),"/MyPDF"+date.getTime()+".pdf");
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "Pdf Created in: "+Environment.getExternalStorageDirectory(), Toast.LENGTH_SHORT).show();
            pdfDocument.close();
        } catch(IOException e){
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            Log.d("Expp","inside exception");
        }
        progressDialog.dismiss();
        pdfDocument.close();
    }
    }
