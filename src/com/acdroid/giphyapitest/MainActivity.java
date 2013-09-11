
package com.acdroid.giphyapitest;

import com.acdroid.giphyapi.Giphy;
import com.acdroid.giphyapi.Giphy.GiphyCallback;
import com.acdroid.giphyapi.data.GiphyInfo.GifInfo;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private TextView mResultText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 
        
        mResultText = (TextView)findViewById(R.id.result);
        mResultText.setMovementMethod(LinkMovementMethod.getInstance());
        
        
        Giphy.initBeta();
        //Giphy.getRecentGifs(null, 1, callback);
        Giphy.translateWordToGif("facepalm", callback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    CustomGiphyCallback callback = new CustomGiphyCallback();
    public class CustomGiphyCallback implements GiphyCallback{

        @Override
        public void onResult(ArrayList<GifInfo> result) {
            mResultText.setText(result.toString());            
        }
        
    }

}
