package tools.soaring.igcuploader;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.Request;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

class LeoCredentials
{
    String user;
    String pass;
    String url;
}

class FlightParameters
{
    String glider;
    int gliderBrand;
    int gliderClass;
    int gliderCert;
    int startType;
    String comments;
}

public class IgcUploadActivity extends AppCompatActivity {
    static final String TAG = "IGC_UPLOADER";

    RequestQueue mQueue;
    LeoCredentials mLeoCred;
    WebView mResponseView;
    String mIgcFilename;
    String mIgcFileData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_igc_upload);
        mResponseView = (WebView) findViewById(R.id.info_uploadresponse);

        // Grab the IGC file
        Intent intent = getIntent();
        File igcFile = (File)intent.getExtras().get(MainActivity.EXTRA_IGCFILE);

        // Set the title to the file name
        mIgcFilename = igcFile.getName();
        getSupportActionBar().setTitle( mIgcFilename );

        // Read the whole IGC file in
        mIgcFileData = readFile( igcFile );

        // Initialize network support
        mQueue = Volley.newRequestQueue(this);

        // @HACK: Settings
        mLeoCred = new LeoCredentials();
        mLeoCred.user = "igcuploadertest";
        mLeoCred.pass = "";
        mLeoCred.url = "http://www.paraglidingforum.com/leonardo/flight_submit.php";

    }

    public String readFile(File file)
    {
        String result;
        long length = file.length();
        if (length < 1 || length > Integer.MAX_VALUE) {
            result = "";
            Log.w(TAG, "File is empty or huge: " + file);
        } else {
            try (FileReader in = new FileReader(file)) {
                char[] content = new char[(int)length];

                int numRead = in.read(content);
                if (numRead != length) {
                    Log.e(TAG, "Incomplete read of " + file + ". Read chars " + numRead + " of " + length);
                }
                result = new String(content, 0, numRead);
            }
            catch (Exception ex) {
                Log.e(TAG, "Failure reading " + file, ex);
                result = "";
            }
        }
        return result;
    }

    public void onUploadButtonPressed(View view)
    {
        getSupportActionBar().setTitle( "Uploaded" );

        FlightParameters params = new FlightParameters();
        params.glider = "Chili 4";
        params.gliderBrand = 6;
        params.gliderCert = 64;
        params.gliderClass = 4;
        params.startType = 1;

        uploadLeonardo( mLeoCred, params );
    }

    public void uploadLeonardo( final LeoCredentials cred, final FlightParameters params )
    {
        final String USER_PARAM = "user";
        final String PASS_PARAM = "pass";
        final String IGCNAME_PARAM = "igcfn";
        final String IGC_PARAM = "IGCigcIGC";
        final String GLIDERCLASS_PARAM = "klasse";
        final String GLIDERCAT_PARAM = "gliderCat";
        final String GLIDERSUBCAT_PARAM = "Category";
        final String GLIDERCERT_PARAM = "gliderCertCategory";
        final String GLIDER_PARAM = "glider";
        final String GLIDERBRAND_PARAM = "gliderBrandID";

        StringRequest request = new StringRequest(Request.Method.POST, cred.url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response)
                    {
                        mResponseView.loadData(response, "text/html; charset=utf-8", "UTF-8");
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mResponseView.loadData(error.toString(), "text/html; charset=utf-8", "UTF-8");
                    }
        }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                Uri query = Uri.EMPTY.buildUpon()
                        .appendQueryParameter(USER_PARAM, cred.user)
                        .appendQueryParameter(PASS_PARAM, cred.pass)
                        .appendQueryParameter(IGCNAME_PARAM, mIgcFilename)
                        .appendQueryParameter(GLIDER_PARAM, params.glider)
                        .appendQueryParameter(GLIDERBRAND_PARAM, Integer.toString(params.gliderBrand))
                        .appendQueryParameter(GLIDERCERT_PARAM, Integer.toString(params.gliderCert))
                        .appendQueryParameter(GLIDERCLASS_PARAM, Integer.toString(params.gliderClass))
                        .appendQueryParameter(IGC_PARAM, mIgcFileData)
                        .build();

                String requestBody = query.toString().substring(1); // Remove the leading '?'

                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }
        };

        // Without disabling retires, Volley seems to send the request twice
        request.setRetryPolicy(new DefaultRetryPolicy(
            0,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        mQueue.add(request);
    }
}
