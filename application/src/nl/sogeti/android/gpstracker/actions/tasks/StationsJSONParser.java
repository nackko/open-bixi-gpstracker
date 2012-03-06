package nl.sogeti.android.gpstracker.actions.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.db.DatabaseHelper;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.util.ByteProgressAdmin;
import nl.sogeti.android.gpstracker.util.ProgressFilterInputStream;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.client.methods.HttpGet;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CancellationException;

/**
 * Created by IntelliJ IDEA.
 * User: F8Full
 * Date: 12-02-29
 * Time: 15:56
 * This parser is used to import Velov stations into the database
 */
public class StationsJSONParser extends AsyncTask<String, Void, Uri>
        //Parameters are input/output types, GpxParser takes an URI for the file reference,
        //whereas I'll have a collection of URLS
{
    private static final String URL = "http://www.velov.grandlyon.com/velovmap/zhp/inc/";
    private static final String PREFIX_QUARTIER = "StationsParArrondissement.php?arrondissement=";
    private static final String [] QUARTIERS = {"69381", "69382", "69383", "69384", "69385", "69386", "69387", "69388", "69389", "69266", "69034", "69256"};

    private static final String TAG = "F8F.StationsJSONParser";
    protected String mErrorDialogMessage;
    protected Exception mErrorDialogException;
    protected Context mContext;
    private ContentResolver mContentResolver;
    private ProgressListener mProgressListener;
    protected StationsJSONParserProgressAdmin mStationsJSONParserProgressAdmin;

    //TODO: manage database through the conventional way with versioning and stuff
    private DatabaseHelper mDbHelper;

    public StationsJSONParser(Context context, ProgressListener progressListener)
    {
        mContext = context;
        mProgressListener = progressListener;
        mDbHelper = new DatabaseHelper(mContext);

        mContentResolver = mContext.getContentResolver();
        mStationsJSONParserProgressAdmin = new StationsJSONParserProgressAdmin();
    }

    protected ArrayList<InputStream> getAllJSONStreams() throws Exception
            //InputStream will hopefully contains as many members as there are boroughs, though for
            //now I'll let silent fail go on (having only partial data). I will fail if now stream could be reached
            //TODO: Add warning in case of incomplete data availability
    {
        //Vector JSONPerBorough = new Vector();
        //SequenceInputStream toReturn = null;

        ArrayList<InputStream> toReturn = new ArrayList<InputStream>();

        long totalLength = 0;

        for (int i=0; i<QUARTIERS.length; ++i)
        {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(URL + PREFIX_QUARTIER + QUARTIERS[i]);

            HttpResponse response = client.execute(request);

            toReturn.add(response.getEntity().getContent());

            totalLength += response.getEntity().getContentLength();
        }
        
        if(toReturn.size() <= 0)
        {
            throw new Exception();
        }

        mStationsJSONParserProgressAdmin.setContentLength(totalLength);
        
        return toReturn;
    }
    
    /*public Uri importStationsFromJSONURLs(String[] importJSONURLs)
    {

    } */
    //  This is way too specific
    //TODO : have this in some generic way if the compatibility list is to expand (which I highly doubt)
    public Uri importStationsFromVelovJSONURLs()
    {
        Uri result = null;

        ArrayList<InputStream> JSONInputStreamsList= null;

        try
        {
            JSONInputStreamsList = getAllJSONStreams();
        }
        catch (Exception e) {
            handleError(e, "JSON opening exception");
        }

        if(JSONInputStreamsList.size() > 0)
        {
            mDbHelper.dropStationsTable();
            mDbHelper.createStationsTable();
        }

        Iterator<InputStream> iterator = JSONInputStreamsList.iterator();

        //THIS WILL LOOP THROUGH ALL INPUSTREAM RETURNED FROM THE OTHER FUNCTION,
        while(iterator.hasNext())
        {
            ImportStations(iterator.next());
        }

        return result;

    }
    
    public Uri ImportStations(InputStream JSONInputStream)
    {
        //{"markers":[{"numStation":"1001","nomStation":"1001 - Terreaux \/ Terme","x":"45.76765100000000","y":"4.832158000000000",
        // "infoStation":"Angle rue d'Alg\u00e9rie"},{"numStation":"1002","nomStation":"1002 - Op\u00e9ra","x":"45.76751200000000",
        // "y":"4.836279000000000","infoStation":"Angle rue Serlin - Angle place de la com\u00e9die"},...]}
        
        Uri result = null;

        try
        {
            ProgressFilterInputStream pfis = new ProgressFilterInputStream(JSONInputStream, mStationsJSONParserProgressAdmin);

            BufferedInputStream bis = new BufferedInputStream(pfis);
            InputStreamReader ISReader= new InputStreamReader(bis);
            BufferedReader reader = new BufferedReader(ISReader);

            String json = reader.readLine();
            JSONObject markersObject = new JSONObject(json);

            JSONArray stations = markersObject.getJSONArray("markers");

            ContentValues stationContent = new ContentValues();

            for(int i=0; i<stations.length(); ++i)
            {
                JSONObject cur = stations.getJSONObject(i);

                stationContent.put(GPStracking.Stations._ID, cur.getInt("numStation"));
                stationContent.put(GPStracking.Stations.NAME, cur.getString("nomStation"));
                stationContent.put(GPStracking.Stations.LATITUDE, cur.getDouble("x"));
                stationContent.put(GPStracking.Stations.LONGITUDE, cur.getDouble("y"));

                mContentResolver.insert(GPStracking.Stations.CONTENT_URI, stationContent);
                stationContent.clear();
            }
        }
        catch (IOException e)
        {
            handleError(e, "error while building JSONArray");
        }
        catch (JSONException e)
        {
            handleError(e, "error while building JSONArray");
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
        //String url = params[0];

        return importStationsFromVelovJSONURLs();
    }

    @Override
    protected  void onPreExecute()
    {
        mProgressListener.started();
    }

    @Override
    protected void onProgressUpdate(Void... values)
    {
        mProgressListener.setProgress(mStationsJSONParserProgressAdmin.getProgress());
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

    public class StationsJSONParserProgressAdmin extends ByteProgressAdmin
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
