package edu.cmu.hw7byadav;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.text.Text;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

public class FirstActivity extends AppCompatActivity {
    public static final String TAG = "FirstActivity";
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final int RESULT_LOAD_IMAGE = 2;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA,
    };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    ImageView imgView;
    Bitmap bmp;
    int numberOfFacesDetected = 0;
    int numberOfSmileyFacesDetected = 0;
    private String mCurrentPhotoPath;
    public static String TWITTER_CONSUMER_KEY = "ozX8AZUOf19wEb7JviZeEFHR1";
    public static String TWITTER_CONSUMER_SECRET = "548wmkcRj37L9DzOcmT4TVwFTSWtNIMjn2FeeS5l3u6qpTshmI";
    public static String PREFERENCE_TWITTER_LOGGED_IN="TWITTER_LOGGED_IN";
    public static float PROBABILITY_OF_SMILE = new Float(0.4);
    Dialog auth_dialog;
    WebView web;
    SharedPreferences pref;
    twitter4j.Twitter twitter;
    RequestToken requestToken;
    AccessToken accessToken;
    String oauth_url, oauth_verifier, profile_url;
    private Button btnTweet;
    private Button btnFind;
    private Button btnTakePhoto;
    File f2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        // check for permissions
        verifyStoragePermissions(FirstActivity.this);
        btnFind = (Button) findViewById(R.id.find_photo);
        btnTweet = (Button) findViewById(R.id.tweet);
        btnTakePhoto = (Button) findViewById(R.id.take_photo);
        imgView = (ImageView)findViewById(R.id.imgview);
        btnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImageChooser();
            }
        });

        btnTweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendOutTweet();
            }
        });

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString("CONSUMER_KEY", TWITTER_CONSUMER_KEY);
        edit.putString("CONSUMER_SECRET", TWITTER_CONSUMER_SECRET);
        edit.commit();

        twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(pref.getString("CONSUMER_KEY", ""), pref.getString("CONSUMER_SECRET", ""));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.second_activity){
            Intent intent = new Intent(FirstActivity.this, BarcodeCaptureActivity.class);
            startActivity(intent);
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openImageChooser(){
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data!=null) {

            //set image in view
            setImageInView(data);


            //run vision api
            getSmileCount(data);
        } else if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data!= null){
            Log.v("entered correctly", "onactivityresult-1");
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imgView.setImageBitmap(imageBitmap);
            Log.v("still going", "onactivityresult-2");
            if (mCurrentPhotoPath == null) {
                Log.v("lets add gallery", "mCurrentPhotoPath is null");
                try {
                    f2 = createImageFile();
                    FileOutputStream output = new FileOutputStream(f2);
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                    output.flush();
                    output.close();
                } catch (IOException e) {
                    Log.v("exceptiontakingphoto","exception while creating file f2");
                    e.printStackTrace();
                }
                //mCurrentPhotoPath needs to be set from invoking galleryIntent
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.MediaColumns.DATA, mCurrentPhotoPath);
                getBaseContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);


                Uri contentUri = Uri.fromFile(f2);
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
                Log.v("broadcast", "should be sent");
                mCurrentPhotoPath = null;
            }
        }
    }

    /* Get the real path from the URI */
    public String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (null != cursor && cursor.getCount() > 0 && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(columnIndex);
        }
        cursor.close();
        return res;
    }

    private String getPathFromURILollipop(Uri contentUri){
        String res = null;
        String wholeID = DocumentsContract.getDocumentId(contentUri);
        Log.v(TAG + "wholeID", wholeID.toString());
        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];

        String[] proj = { MediaStore.Images.Media.DATA };

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = getContentResolver().
                query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        proj, sel, new String[]{ id }, null);

        int columnIndex = cursor.getColumnIndex(proj[0]);

        if (cursor.moveToFirst()) {
            res = cursor.getString(columnIndex);
        }
        cursor.close();
        return res;
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    @NonNull
    private String getXiamoiFilePath(Uri uri){
        StringBuilder res = new StringBuilder();
        String [] testArrForTypeCheck = uri.toString().split(":");
        if(testArrForTypeCheck[0].equalsIgnoreCase("file")){
            int i = 0 ;
            for(String c : testArrForTypeCheck[1].split("")){
                i++;
                if(i > 2)
                    res.append(c);
            }
        }
        return res.toString();
    }

    private void setImageInView(Intent data){
        Uri selectedImage = data.getData();
//            Log.v("Intent data", data.toString());
//            Log.v("URI selectedImage", selectedImage.toString());
//            Log.v("selectedImage authority", selectedImage.getAuthority());
//            Log.v("xiaomifilepath", getXiamoiFilePath(selectedImage));
        if (null != selectedImage) {
            ////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////
            ///////////////////////////////////////////////////////////
            //FIX THIS BEFORE SUBMISSION//////////////////////////////
            // Get the path from the Uri
//                String path = getPathFromURI(selectedImage);

            //Xiaomi specific code here
            String path = getXiamoiFilePath(selectedImage);
            Log.v(TAG, "Image Path : " + path);


            // Set the image in ImageView
            // imgView.setImageURI(selectedImage);

            try {
                bmp = getBitmapFromUri(selectedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            imgView.setImageBitmap(bmp);
        }
    }

    private void getSmileCount(Intent data){
        FaceDetector faceDetector = new
                FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                .setClassificationType(1).build();

        if(!faceDetector.isOperational()){
            Log.d("Not operational", "Could not set up the face detector!");
            Log.w(TAG, "Face detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
            return;
        }
        Frame frame = new Frame.Builder().setBitmap(bmp).build();
        SparseArray<Face> faces = faceDetector.detect(frame);
        numberOfFacesDetected = faces.size();

        for(int i=0; i<faces.size(); i++) {
            Face thisFace = faces.valueAt(i);
            float smile = thisFace.getIsSmilingProbability();
            if (smile>FirstActivity.PROBABILITY_OF_SMILE)
                numberOfSmileyFacesDetected++;

//            float x1 = thisFace.getPosition().x;
//            float y1 = thisFace.getPosition().y;
//            float x2 = x1 + thisFace.getWidth();
//            float y2 = y1 + thisFace.getHeight();
        }

        displayDetectionText(numberOfFacesDetected, numberOfSmileyFacesDetected);
    }

    private void displayDetectionText(int numberOfFacesDetected, int numberOfSmileyFacesDetected){
        TextView faceDetectionResult = (TextView) findViewById(R.id.face_detection_result);
        faceDetectionResult.setText(
                getString(R.string.face_detection_result, numberOfFacesDetected, numberOfSmileyFacesDetected));
    }

    private void sendOutTweet(){
        if (!pref.getBoolean(PREFERENCE_TWITTER_LOGGED_IN,false)){
            new TokenGet().execute(); //no Token obtained, first time use
        }else{
            new PostTweet().execute(); //when Tokens are obtained , ready to Post
        }
    }

    private class PostTweet extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(String... args) {

            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(pref.getString("CONSUMER_KEY", ""));
            builder.setOAuthConsumerSecret(pref.getString("CONSUMER_SECRET", ""));

            AccessToken accessToken = new AccessToken(pref.getString("ACCESS_TOKEN", ""), pref.getString("ACCESS_TOKEN_SECRET", ""));
            twitter4j.Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm:aa");
            String currentDateTime = sdf.format(new Date());
//            String currentDateTime = "Jun 23, 2017 01:40";
//            String status = "Check In:" + currentDateTime + " http://www.pvgp.org";
            String status = "@08723Mapp byadav " +
                    numberOfFacesDetected + " face detected / " +
                    numberOfSmileyFacesDetected + " smiley face detected at " + currentDateTime.toString();
            Log.v("tweet text", status);
            twitter4j.Status response = null;
            try {
                response = twitter.updateStatus(status);
            } catch (twitter4j.TwitterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return (response != null) ? response.toString() : "";
        }

        protected void onPostExecute(String res) {
            if (res != null) {
                // progress.dismiss();
                Toast.makeText(getApplicationContext(), "Tweet successfully Posted", Toast.LENGTH_SHORT).show();

            } else {
                //progress.dismiss();
                Toast.makeText(getBaseContext(), "Error while tweeting !", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private class TokenGet extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... args) {
            try {
                requestToken = twitter.getOAuthRequestToken();
                oauth_url = requestToken.getAuthorizationURL();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return oauth_url;
        }

        @Override
        protected void onPostExecute(String oauth_url) {
            if(oauth_url != null){
                auth_dialog = new Dialog(FirstActivity.this);
                auth_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                auth_dialog.setContentView(R.layout.oauth_webview);

                web = (WebView)auth_dialog.findViewById(R.id.webViewOAuth);
                web.getSettings().setJavaScriptEnabled(true);
                web.loadUrl(oauth_url);
                web.setWebViewClient(new WebViewClient() {
                    boolean authComplete = false;

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon){
                        super.onPageStarted(view, url, favicon);                 }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        if (url.contains("oauth_verifier") && authComplete == false){
                            authComplete = true;
                            Uri uri = Uri.parse(url);
                            oauth_verifier = uri.getQueryParameter("oauth_verifier");
                            auth_dialog.dismiss();
                            new AccessTokenGet().execute();
                        }else if(url.contains("denied")){
                            auth_dialog.dismiss();
                            Toast.makeText(getBaseContext(), "Sorry !, Permission Denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                Log.d("Debug", auth_dialog.toString());
                auth_dialog.show();
                auth_dialog.setCancelable(true);
            }else{
                Toast.makeText(getBaseContext(), "Sorry !, Error or Invalid Credentials", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class AccessTokenGet extends AsyncTask<String, String, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... args) {
            try {
                accessToken = twitter.getOAuthAccessToken(requestToken, oauth_verifier);
                SharedPreferences.Editor edit = pref.edit();
                edit.putString("ACCESS_TOKEN", accessToken.getToken());
                edit.putString("ACCESS_TOKEN_SECRET", accessToken.getTokenSecret());
                edit.putBoolean(PREFERENCE_TWITTER_LOGGED_IN, true);

                User user = twitter.showUser(accessToken.getUserId());
                profile_url = user.getOriginalProfileImageURL();
                edit.putString("NAME", user.getName());
                edit.putString("IMAGE_URL", user.getOriginalProfileImageURL());
                edit.commit();
            } catch (TwitterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean response) {
            if(response){
                //progress.hide(); after login, tweet Post right away
                new PostTweet().execute();
            }
        }
    }

    //Checking permission
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        Log.v("EnvironmentDataDir", Environment.getDataDirectory().toString());
        Log.v("EnvironmentRootDir", Environment.getRootDirectory().toString());
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        Log.v("getExStoragePublicDir", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());
        Log.v("getExStorageDir", (Environment.getExternalStorageDirectory()).toString());
        Log.v("storageDir", storageDir.toString());
        //File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),IMAGE_DIRECTORY_NAME);

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);

        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

}
