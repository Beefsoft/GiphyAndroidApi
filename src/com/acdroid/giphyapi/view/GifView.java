
package com.acdroid.giphyapi.view;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Android view that can show a Gif.
 * 
 * @author Marcos Trujillo
 */
public class GifView extends WebView {
    private static final String GIF_HTML_START = "<html><head><meta name=\"viewport\" " +
            "content=\" width=device-width, initial-scale=1\"/>" +
            "<style>body{margin: 0px; padding: 0px;}a {display:block;} " +
            "a img {margin:0;width:100%;} </style></head><body><img src=\"";
    private static final String GIF_HTML_END = "\" border=\"0\"/></body></html>";

    private String mPath = "";

    public GifView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public GifView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    public GifView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }

    public void setGifUrl(String url) {
        if (url != null)
            mPath = url;
    }

    /**
     * Returns the HTML that load the current Gif
     * 
     * @param path
     * @return
     */
    private String getGifHtml(String path) {
        return GIF_HTML_START + mPath + GIF_HTML_END;
    }
}
