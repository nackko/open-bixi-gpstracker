package nl.sogeti.android.gpstracker.actions.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.db.DatabaseHelper;
import nl.sogeti.android.gpstracker.db.GPStracking.Stations;
import nl.sogeti.android.gpstracker.util.ByteProgressAdmin;
import nl.sogeti.android.gpstracker.util.ProgressFilterInputStream;
import nl.sogeti.android.gpstracker.util.UnicodeReader;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.client.methods.HttpGet;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.concurrent.CancellationException;


/**
 * Created by IntelliJ IDEA.
 * User: F8Full
 * Date: 12-02-16
 * Time: 11:58
 * This is the parser class for an XML following Bixi format -- see later comment
 * it reads and create stations in the data provider
 */
public class StationsXMLParser extends AsyncTask<String, Void, Uri>
    //Parameters are input/output types, GpxParser takes an URI for the file reference,
    //whereas I'll have a URL
{
    private static final String TAG = "F8F.StationsXMLParser";
    protected String mErrorDialogMessage;
    protected Exception mErrorDialogException;
    protected Context mContext;
    private ContentResolver mContentResolver;
    private ProgressListener mProgressListener;
    protected StationsXMLParserProgressAdmin mStationsXMLParserProgressAdmin;

    //TODO: manage database through the conventional way with versioning and stuff
    private DatabaseHelper mDbHelper;
    
    public StationsXMLParser(Context context, ProgressListener progressListener)
    {
        mContext = context;
        mProgressListener = progressListener;
        mDbHelper = new DatabaseHelper(mContext);

        mContentResolver = mContext.getContentResolver();
        mStationsXMLParserProgressAdmin = new StationsXMLParserProgressAdmin();
    }

    protected InputStream getXMLInputStreamFromURL(String url) throws Exception{
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);

        HttpResponse response = client.execute(request);
        mStationsXMLParserProgressAdmin.setContentLength(response.getEntity().getContentLength());

        return response.getEntity().getContent();
    }

    public Uri importStationsFromXMLURL(String importXMLURL)
    {
        //This is more appropriate in the case of a tracks because there is a Uri associated with each one
        //I return the uri of the whole stations table
        Uri result = null;
        
        InputStream XmlInputStream = null;

        try
        {
            XmlInputStream = getXMLInputStreamFromURL(importXMLURL);
        }
        //TODO: Retrieve those string from resources
        catch (UnsupportedEncodingException e) {
            handleError(e, "XML opening exception");
        } catch (MalformedURLException e) {
            handleError(e, "XML opening exception");
        } catch (IOException e) {
            handleError(e, "XML opening exception");
        }
        catch (Exception e) {
            handleError(e, "XML opening exception");
        }

        result = importStations(XmlInputStream);
        
        return result;
    }

    //This is a one block for all stations right now, I'll probably cut it later
    //to extract the import of only a single station (which will have an associated uri)
    //It returns the stations uri if data have been read from input
    public Uri importStations(InputStream xmlInputStream)
    {
        int eventType;
        Uri result = null;
//     <stations>
//        <station>
//          <id>1</id>
//          <name>Notre Dame / Place Jacques Cartier</name>
//          <terminalName>6001</terminalName>
//          <lat>45.508183</lat>
//          <long>-73.554094</long>
//          <installed>true</installed>
//          <locked>false</locked>
//          <installDate>1276012920000</installDate>
//          <removalDate />
//          <temporary>false</temporary>
//          <nbBikes>14</nbBikes>
//          <nbEmptyDocks>17</nbEmptyDocks>
//        </station>
//        <station>
//          ...
//        </station>
//     </stations>

        try
        {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            //factory.setNamespaceAware(true);
            XmlPullParser xmlParser = factory.newPullParser();

            ProgressFilterInputStream pfis = new ProgressFilterInputStream(xmlInputStream, mStationsXMLParserProgressAdmin);
            BufferedInputStream bis = new BufferedInputStream(pfis);

            UnicodeReader ur = new UnicodeReader(bis, "UTF-8");

            xmlParser.setInput(ur);

            eventType = xmlParser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT)
            {
                xmlParser.next();
                eventType = xmlParser.getEventType();


                if(eventType == XmlPullParser.START_TAG && xmlParser.getName().equals("stations"))
                {
                    mDbHelper.dropStationsTable();
                    mDbHelper.createStationsTable();
                }

                if(xmlParser.getName().equals("station"))
                {
                    ContentValues stationContent = new ContentValues();

                    xmlParser.next();   //will now be positioned on station ID
                    eventType = xmlParser.getEventType();

                    while ( eventType != XmlPullParser.END_DOCUMENT && !xmlParser.getName().equals("station") )
                    {
                        if(eventType == XmlPullParser.START_TAG && xmlParser.getName().equals("id"))
                        {
                            xmlParser.next();   //to get to TEXT
                            String stationID = xmlParser.getText();
                            xmlParser.next();                   //END TAG
                            xmlParser.next(); //START TAG
                            eventType = xmlParser.getEventType();

                            stationContent.put(Stations._ID, stationID);
                        }
                        else if(eventType == XmlPullParser.START_TAG && xmlParser.getName().equals("name"))
                        {
                            xmlParser.next();   //to get to TEXT
                            String stationName = xmlParser.getText();
                            xmlParser.next(); //END TAG
                            xmlParser.next(); //START TAG
                            eventType = xmlParser.getEventType();

                            stationContent.put(Stations.NAME, stationName);
                        }
                        else if(eventType == XmlPullParser.START_TAG && xmlParser.getName().equals("lat"))
                        {
                            xmlParser.next();   //to get to TEXT
                            String stationLat = xmlParser.getText();
                            xmlParser.next(); //END TAG
                            xmlParser.next(); //START TAG
                            eventType = xmlParser.getEventType();

                            stationContent.put(Stations.LATITUDE, stationLat);
                        }
                        else if(eventType == XmlPullParser.START_TAG && xmlParser.getName().equals("long"))
                        {
                            xmlParser.next();   //to get to TEXT
                            String stationLong = xmlParser.getText();
                            xmlParser.next(); //END TAG
                            xmlParser.next(); //START TAG
                            eventType = xmlParser.getEventType();

                            stationContent.put(Stations.LONGITUDE, stationLong);
                        }
                        else
                        {
                            xmlParser.next();
                            eventType = xmlParser.getEventType();

                            while(eventType != XmlPullParser.START_TAG && !(eventType == XmlPullParser.END_TAG && xmlParser.getName().equals("station")))
                            {
                                xmlParser.next();
                                eventType = xmlParser.getEventType();
                            }

                        }

                        if(stationContent.size() == 4)
                        //I should have a complete station now
                        {
                            //This returns the URI
                            mContentResolver.insert(Stations.CONTENT_URI, stationContent);
                            stationContent.clear();
                        }
                    }
                }
                else if(eventType == XmlPullParser.END_TAG)   //we were on the stations end, we need to do a next to reach the end of the document
                {
                    xmlParser.next();
                    eventType = xmlParser.getEventType();
                }

            }
            result = Stations.CONTENT_URI;
        }
        catch (XmlPullParserException e)
        {
            handleError(e, "error while creating XML parser");
        }
        catch (IOException e)
        {
            handleError(e, "error while creating XML parser");
        }

     return result;
    }

    /**
     *
     * @param dialogException
     * @param dialogErrorMessage
     */
    protected void handleError(Exception dialogException, String dialogErrorMessage)
    {
        Log.e(TAG, "Unable to save ", dialogException);
        mErrorDialogException = dialogException;
        mErrorDialogMessage = dialogErrorMessage;
        cancel(false);
        throw new CancellationException(dialogErrorMessage);
    }

    @Override
    protected Uri doInBackground(String... params) {
        String url = params[0];

        return importStationsFromXMLURL(url);
    }

    @Override
    protected  void onPreExecute()
    {
        mProgressListener.started();
    }
    
    @Override
    protected void onProgressUpdate(Void... values)
    {
        mProgressListener.setProgress(mStationsXMLParserProgressAdmin.getProgress());
    }
    
    @Override
    protected void onPostExecute(Uri result)
    {
        mProgressListener.finished(result);
    }

    @Override
    protected void onCancelled()
    {
        //mProgressListener.showError(mContext.getString(R.string.taskerror_gpx_import), mErrorDialogMessage, mErrorDialogException);
        mProgressListener.showError("oups", mErrorDialogMessage, mErrorDialogException);
    }

    public class StationsXMLParserProgressAdmin extends ByteProgressAdmin
    {
        public void addBytesProgress(int addedBytes)
        {
            super.addBytesProgress(addedBytes);

            considerPublishProgress();
        }

        @Override
        public void considerPublishProgress()
        {
            if(mustPublishProgress())
            {
                publishProgress();
            }
        }
     }
}
