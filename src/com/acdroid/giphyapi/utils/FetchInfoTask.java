package com.acdroid.giphyapi.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Build;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * FetchInfoTask is a AsyncTask wrapper to fetch information using http. Override the setResult to get the
 * result of the task. Override the setError to get an advise when an error occur.<br>
 * <p>
 * If the fetched information is a Json and you want to receive the json parsed extends this class with the
 * Result class assigned (Should be different from String)<br>
 * <b>To parse the JSON uses the library Jackson, you should add this library to your project</b>
 * <p>
 * Usage:
 * <ul>
 * <li>Extends this class.
 * <li>Override the {@link #setResult(Object) and {@link #onError(int, String)} methods
 * <li>Execute it with:
 * <em>new CustomClass&lt;ResultType&gt;(url).enableJsonParsing(result.class).executeFetch();</em>
 * </ul>
 * <p>
 * If the fetched information is other than Json, like XML, you can set an interface to parse the result and
 * receive the information parsed on the onResult method. <br>
 * <p>
 * Usage:
 * <ul>
 * <li>Extends this class.
 * <li>Override the {@link #setResult(Object) and {@link #onError(int, String)} methods
 * <li>Execute it with:
 * <em>new CustomClass&lt;ResultType&gt;(url).enableCustonParsing(FetchInfoParser).executeFetch();</em>
 * </ul>
 * <p>
 * <p>
 * Example class
 * 
 * <pre class="prettyprint">
 * private class CustomFetchInfo extends FetchInfoTask&lt;SomethingClass&gt; {
 *     protected void setResult(SomethingClass stuff) {
 *         mStuff = stuff;
 *     }
 * 
 *     protected void setError(int type, String message) {
 *         // Important Stuff
 *     }
 * }
 * </pre>
 * 
 * @author Marcos Trujillo Seoane
 * @param <Result> Result object type
 */
public abstract class FetchInfoTask<Result> extends AsyncTask<String, Void, Result> {
    protected static final int HTTP_CONNECTION_TIMEOUT = 10000;
    protected static final int HTTP_READ_TIMEOUT = 8000;
    protected static final int HTTP_SOCKET_TIMEOUT = 8000;
    protected static final int BUFFER_SIZE = 2048;

    protected static final int TYPE_DEFAULT = 2;
    protected static final int TYPE_HTTP_GET = 0;
    protected static final int TYPE_HTTP_POST = 1;
    protected static final int TYPE_HTTP_URLCONNECTION_GET = 2;

    protected static final int ERROR_NULL_URL = 0;
    protected static final int ERROR_JSON_PARSE = 1;
    protected static final int ERROR_JSON_MAPPING = 2;
    protected static final int ERROR_IO_EXCEPTION = 505;

    private Class<?> mJsonObjectClass = null; // .class to parse the result with Jackson
    private FetchInfoParser<Result> mParser = null;
    private static ObjectMapper mMapper;
    private int mType = TYPE_DEFAULT;
    private String mUrl;
    private int mTrafficStatsTag = 0xAAAA;

    public FetchInfoTask() {
        mType = TYPE_DEFAULT;
    }

    /**
     * Default FetchInfoTask builder The HTTP connection will be a <em>HTTP GET</em>
     * 
     * @param url EndPoint where is the information
     */
    public FetchInfoTask(String url) {
        if (url == null || "".equals(url)) {
            onError(ERROR_NULL_URL, "URL is empty");
            mUrl = null;
            return;
        }

        mUrl = url;
        mType = TYPE_DEFAULT;
    }

    /**
     * FetchInfoTask builder
     * 
     * @param url EndPoint where is the information
     * @param type {@link #TYPE_HTTP_GET} or {@link #TYPE_HTTP_POST}
     */
    public FetchInfoTask(String url, int type) {
        if (url == null || "".equals(url)) {
            onError(ERROR_NULL_URL, "URL is empty");
            mUrl = null;
            return;
        }

        mUrl = url;
        mType = type;
    }

    /**
     * Sets the url to fetch
     * 
     * @param url
     */
    protected void setUrl(String url) {
        mUrl = url;
    }

    /**
     * Sets the type of connection to use
     * 
     * @param type
     */
    protected void setType(int type) {
        mType = type;
    }

    /**
     * For DDMS Connection stats purposes. Sets the tag. The tags normally are like 0xF00D
     * 
     * @param tag
     */
    protected void setTrafficStatsTag(int tag) {
        mTrafficStatsTag = tag;
    }

    /**
     * Execute the Task. This method controls the Thread Pool to maximize the
     */
    @SuppressLint("NewApi")
    public void executeFetch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            execute(mUrl);
        else
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mUrl);
    }

    /**
     * Enables the json parsing of the response with the Jackson library
     * 
     * @param jsonObjectClass .Class of the result object
     */
    protected FetchInfoTask<Result> enableJsonParsing(Class<?> jsonObjectClass) {
        mJsonObjectClass = jsonObjectClass;
        return this;
    }

    /**
     * Enables the json parsing of the response with the Jackson library
     * 
     * @param jsonObjectClass .Class of the result object
     */
    protected FetchInfoTask<Result> enableCustomParsing(FetchInfoParser<Result> parser) {
        mParser = parser;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Result doInBackground(String... url) {
        if (url == null || url.length == 0 || "".equals(url[0])) {
            onError(ERROR_NULL_URL, "URL is empty");
            return null;
        }

        try {
            Object infoFetched;
            switch (mType) {

                case TYPE_HTTP_POST:
                    throw new UnsupportedOperationException("HTTP_POST unsupported");
                case TYPE_HTTP_URLCONNECTION_GET:
                    infoFetched = httpUrlConnectionGet(url[0], mResponseHandler);
                    break;
                case TYPE_HTTP_GET:
                default:
                    infoFetched = httpGetPetition(url[0], mResponseHandler);
                    break;
            }

            if (infoFetched instanceof FetchInfoTask.FetchInfoError) {
                onError(((FetchInfoError) infoFetched).mValue, ((FetchInfoError) infoFetched).mMessage);
                return null;
            }

            // When no need to parse the info fetched, return the String
            if (mJsonObjectClass != null) {
                // Other cases, parse the info with Jackson
                if (mMapper == null) mMapper = new ObjectMapper();
                Result r = (Result) mMapper.readValue((String) infoFetched, mJsonObjectClass);
                return r;
            }

            if (mParser != null) { return mParser.onParse((String) infoFetched); }

            return (Result) infoFetched;

        } catch (ClientProtocolException e) {
            onError(ERROR_IO_EXCEPTION, "Client protocol Exception");
        } catch (JsonParseException e) {
            onError(ERROR_JSON_PARSE, "JsonParseException " + e.getMessage());
            e.printStackTrace();
        } catch (JsonMappingException e) {
            onError(ERROR_JSON_MAPPING, "JsonMappingException, some of the values is not well mapped");
            e.printStackTrace();
        } catch (IOException e) {
            onError(ERROR_IO_EXCEPTION, "IOException: " + e.getMessage());
        } catch (Exception e) {
            onError(ERROR_IO_EXCEPTION, "Exception: " + e.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(Result result) {
        if (result == null) { return; }

        onResult(result);
    }

    /**
     * Override this method to get and manage the result of fetching the info. The specified parameter is the
     * parameter passed to {@link FetchInfoTask} when instantiate it. If the type of FethcInfoTask is If the
     * FetchInfoTask type is {@link #TYPE_FETCH_JSON} the result will be a Result parsed.
     * 
     * @param params The parameters of the result task
     * @see #setResult(Object)
     * @see #onError(int, String)
     */
    protected abstract void onResult(Result result);

    protected abstract void onError(int type, String message);

    /**
     * Response Handler that manages the result of the connection execution Returns the fetch info or result
     */
    private final ResponseHandler<Object> mResponseHandler = new ResponseHandler<Object>() {

        @Override
        public Object handleResponse(HttpResponse response) {
            int status = response.getStatusLine().getStatusCode();
            if (status > 200)
                return new FetchInfoError(status, response.getStatusLine().getReasonPhrase());

            BufferedInputStream content = null;
            try {
                content = (BufferedInputStream) response.getEntity().getContent();
                return readStream(content);
            } catch (IOException e) {
                return new FetchInfoError(ERROR_IO_EXCEPTION, "IOException: " + e.getMessage());
            }
        }
    };

    /**
     * Reads a InputStream. This method close the inputStream
     * 
     * @param in
     * @return
     * @throws IOException
     */
    private Object readStream(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(BUFFER_SIZE);
        byte[] buffer = new byte[BUFFER_SIZE];
        int lenght;
        while ((lenght = in.read(buffer)) > 0) {
            bout.write(buffer, 0, lenght);
        }
        in.close();
        return bout.toString();
    }

    /**
     * Make a Synchronous Http Get petition using the HttpUrlConnection library.
     * 
     * @param endPoint Url to make the get petition
     * @param responseHandler
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected Object httpUrlConnectionGet(String endPoint, ResponseHandler<?> responseHandler)
            throws IOException, ClientProtocolException, MalformedURLException {

        URL url = new URL(endPoint);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
        urlConnection.setConnectTimeout(HTTP_CONNECTION_TIMEOUT);
        urlConnection.setReadTimeout(HTTP_READ_TIMEOUT);
        urlConnection.setUseCaches(false);
        urlConnection.setInstanceFollowRedirects(true);

        // Tag the connection for debug purposes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            TrafficStats.setThreadStatsTag(mTrafficStatsTag);
        }
        try {
            BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());

            int status = urlConnection.getResponseCode();
            if (status / 100 != 2)
                return new FetchInfoError(status, urlConnection.getResponseMessage());

            return readStream(in);
        } catch (IOException e) {
            return new FetchInfoError(ERROR_IO_EXCEPTION, "IOException: " + e.getMessage());
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                TrafficStats.clearThreadStatsTag();
            }
            urlConnection.disconnect();
        }
    }

    /**
     * Make a Synchronous Http Get petition to the url and usePARSE the {@link ResponseHandler}
     * 
     * @param endPoint Url to make the get petition
     * @param responseHandler
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected Object httpGetPetition(String endPoint, ResponseHandler<?> responseHandler)
            throws IOException, ClientProtocolException {
        // HttpClient
        // final DefaultHttpClient httpClient = HttpClientFactory.getThreadSafeClient();
        final DefaultHttpClient httpClient = new DefaultHttpClient();

        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, HTTP_CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, HTTP_SOCKET_TIMEOUT);
        HttpProtocolParams.setUseExpectContinue(params, true);

        // HttpGet
        HttpGet httpGet = new HttpGet(endPoint);

        // Execute the request
        Object result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            TrafficStats.setThreadStatsTag(mTrafficStatsTag);
            try {
                result = httpClient.execute(httpGet, responseHandler);
            } finally {
                TrafficStats.clearThreadStatsTag();
            }
        }
        else {
            result = httpClient.execute(httpGet, responseHandler);
        }

        // close the connection
        httpClient.getConnectionManager().shutdown();

        return result;
    }

    /**
     * Bean of an error
     * 
     * @author Marcos Trujillo Seoane
     */
    protected class FetchInfoError {
        private final int mValue;
        private final String mMessage;

        protected FetchInfoError(int value, String message) {
            mValue = value;
            mMessage = message;
        }
    }

    protected interface FetchInfoParser<Result> {
        Result onParse(String info);
    }

    /**
     * A nested class to get an singleton/instance of DefaultHTTPClient. Is ThreadSafe Tested and fail!! Need
     * more test
     * 
     * @author Marcos Trujillo
     */
    private static class HttpClientFactory {

        private static DefaultHttpClient mHttpClient;

        public synchronized static DefaultHttpClient getThreadSafeClient() {

            if (mHttpClient != null)
                return mHttpClient;

            mHttpClient = new DefaultHttpClient();

            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, HTTP_CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, HTTP_SOCKET_TIMEOUT);
            HttpProtocolParams.setUseExpectContinue(params, true);

            ClientConnectionManager mgr = mHttpClient.getConnectionManager();
            mHttpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, mgr.getSchemeRegistry()),
                    params);

            return mHttpClient;
        }
    }

}