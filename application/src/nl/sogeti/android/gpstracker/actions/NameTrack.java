/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) Apr 24, 2011 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 *
 *   This file is part of OpenGPSTracker.
 *
 *   OpenGPSTracker is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OpenGPSTracker is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OpenGPSTracker.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package nl.sogeti.android.gpstracker.actions;

import android.app.*;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.Stations;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;

import java.util.Calendar;

//To use LoaderManager and cursorLoader classes on pre honeycomb

/**
 * Empty Activity that pops up the dialog to name the track
 *
 * @version $Id: NameTrack.java 1132 2011-10-09 18:52:59Z rcgroot $
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 */
public class NameTrack extends FragmentActivity //Compatibility requirement : instead of Activity
    implements LoaderManager.LoaderCallbacks<Cursor>, TextWatcher   //Required for LoaderManager, Used to watch user text
                                                                         //input and restart cursorLoaders accordingly
{
   private static final int DIALOG_TRACKNAME = 23;

   protected static final String TAG = "OGT.NameTrack";

   private EditText mTrackNameView;
   //F8F BEGIN
    private RadioGroup mHelmetRadioGroup;
   private Spinner mOriginReasonSpinner;
   private Spinner mDestinationReasonSpinner;

   private ArrayAdapter<CharSequence> mReasonAdapter;

    private RatingBar mServiceRatingBar;

   private AutoCompleteTextView mStartStationAutoCompleteTextView;
    private AutoCompleteTextView mEndStationAutoCompleteTextView;

    //To deprecate : this is used in the textbook version of autocompletetextview
    //(filled from a resource file)
   private ArrayAdapter<String> mStationAdapter;
    ///////////////////////////////////////////////////////////////////
    private SimpleCursorAdapter mStartStationSimpleCursorAdapter;
    private SimpleCursorAdapter mEndStationSimpleCursorAdapter;
    //Used to pass around current user input in suggestion boxes
    String mCurStartStationNameFilter;
    String mCurEndStationNameFilter;

    ///////////////////////////////////////////////////////////////////
    
    private static final int STARTSTATIONCURSOR_LOADER = 0;
    private static final int ENDSTATIONCURSOR_LOADER = 1;

    //private CursorAdapter
    //F8F END

   private boolean paused;
   Uri mTrackUri;

   private final DialogInterface.OnClickListener mTrackNameDialogListener = new DialogInterface.OnClickListener()
   {
      public void onClick( DialogInterface dialog, int which )
      {
         switch( which )
         {
            case DialogInterface.BUTTON_POSITIVE:
                updateTrack();
               clearNotification();
               break;
            case DialogInterface.BUTTON_NEUTRAL:
               startDelayNotification();
               break;
            case DialogInterface.BUTTON_NEGATIVE:
               clearNotification();
               break;
            default:
               Log.e( TAG, "Unknown option ending dialog:"+which );
               break;
         }
         finish();
      }


   };

    //Some hack to prevent autocomplete tips being displayed again after item selection
    private final TextWatcher mStartStationTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
            if (count > 1)
            {
                mCurStartStationNameFilter = charSequence.toString();
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            //NOTE : the activity is also listening because cursorloader restart function needs to
            //know where to find the callbacks, hence being passed the activity itself
        }
    };

    private final TextWatcher mEndStationTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
            if (count > 1 )
            {
                mCurEndStationNameFilter = charSequence.toString();
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            //NOTE : the activity is also listening because cursorloader restart function needs to
            //know where to find the callbacks, hence being passed the activity itself
        }
    };
    //End of hack



    private void updateTrack()
    {
        String trackName = null;
        trackName = mTrackNameView.getText().toString();
        ContentValues values = new ContentValues();
        values.put( Tracks.NAME, trackName );

        if (mHelmetRadioGroup.getCheckedRadioButtonId() == -1)
        {
            values.put(Tracks.WITH_HELMET, "-1");
        }
        else if (mHelmetRadioGroup.getCheckedRadioButtonId() == R.id.helmetRadio_yes)
        {
            values.put(Tracks.WITH_HELMET, "1");
        }
        else    //mHelmetRadioGroup.getCheckedRadioButtonId() == R.id.helmetRadio_no
        {
            values.put(Tracks.WITH_HELMET, "0");
        }

        String startReason = mOriginReasonSpinner.getSelectedItem().toString();

        values.put(Tracks.START_REASON, startReason);

        String endReason = mDestinationReasonSpinner.getSelectedItem().toString();

        values.put(Tracks.END_REASON, endReason);

        if (mServiceRatingBar.getRating() > 0)
            // 0 = unrated track, minimum valid rating being 1
        {
            values.put(Tracks.SERVICE_RATING, (int)mServiceRatingBar.getRating());
        }
        
        values.put(Tracks.START_STATION_NAME, mStartStationAutoCompleteTextView.getText().toString());
        values.put(Tracks.END_STATION_NAME, mEndStationAutoCompleteTextView.getText().toString());
        




        getContentResolver().update( mTrackUri, values, null, null );

    }
   
   
   private void clearNotification()
   {

      NotificationManager noticationManager = (NotificationManager) this.getSystemService( Context.NOTIFICATION_SERVICE );;
      noticationManager.cancel( R.layout.namedialog );
   }
   
   private void startDelayNotification()
   {
      int resId = R.string.dialog_routename_title;
      int icon = R.drawable.ic_maps_indicator_current_position;
      CharSequence tickerText = getResources().getString( resId );
      long when = System.currentTimeMillis();
      
      Notification nameNotification = new Notification( icon, tickerText, when );
      nameNotification.flags |= Notification.FLAG_AUTO_CANCEL;
      
      CharSequence contentTitle = getResources().getString( R.string.app_name );
      CharSequence contentText = getResources().getString( resId );
      
      Intent notificationIntent = new Intent( this, NameTrack.class );
      notificationIntent.setData( mTrackUri );
      
      PendingIntent contentIntent = PendingIntent.getActivity( this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK );
      nameNotification.setLatestEventInfo( this, contentTitle, contentText, contentIntent );
      
      NotificationManager noticationManager = (NotificationManager) this.getSystemService( Context.NOTIFICATION_SERVICE );
      noticationManager.notify( R.layout.namedialog, nameNotification );
   }
   
   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
       //This activity hides itself, the TrackList activity switches it's visible state
      this.setVisible( false );
      paused = false;
      mTrackUri = this.getIntent().getData();
       mCurStartStationNameFilter = "";
       mCurEndStationNameFilter = "";

       getSupportLoaderManager().initLoader(STARTSTATIONCURSOR_LOADER, null, this);
       getSupportLoaderManager().initLoader(ENDSTATIONCURSOR_LOADER, null, this);

       String[] uiBindFrom = { Tracks.NAME };
       int[] uiBindTo = { android.R.id.text1 };
       
       mStartStationSimpleCursorAdapter = new SimpleCursorAdapter(
               this, android.R.layout.simple_dropdown_item_1line,
               null, uiBindFrom,
               uiBindTo,
               0);

       mEndStationSimpleCursorAdapter = new SimpleCursorAdapter(
               this, android.R.layout.simple_dropdown_item_1line,
               null, uiBindFrom,
               uiBindTo,
               0);

       // Set the CursorToStringConverter, to provide the labels for the
       // choices to be displayed in the AutoCompleteTextView.
       mStartStationSimpleCursorAdapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
           public String convertToString(android.database.Cursor cursor) {
               // Get the label for this row out of the "state" column
               final int columnIndex = cursor.getColumnIndexOrThrow(Tracks.NAME);
               final String str = cursor.getString(columnIndex);
               return str;
           }
       });

       //TODO : Maybe I should have this in an external object I'd reuse instead of two anonymous objects ?
       mEndStationSimpleCursorAdapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
           public String convertToString(android.database.Cursor cursor) {
               // Get the label for this row out of the "state" column
               final int columnIndex = cursor.getColumnIndexOrThrow(Tracks.NAME);
               final String str = cursor.getString(columnIndex);
               return str;
           }
       });



       //DO NOT BE TEMPTED, THE FOLLOWING CODE MAKE THE QUERY FROM THE UI THREAD I THINK, AND THIS IS BAD, VERY BAD !
       // Set the FilterQueryProvider, to run queries for choices
       // that match the specified input.
       /*mStartStationSimpleCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
           public Cursor runQuery(CharSequence constraint) {
               // Search for states whose names begin with the specified letters.
               Cursor cursor = mDbHelper.getMatchingStates(
                       (constraint != null ? constraint.toString() : null));
               return cursor;
           }
       });*/
   }
   
   @Override
   protected void onPause()
   {
      super.onPause();
      paused = true;
   }
   
   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onPause()
    */
   @Override
   protected void onResume()
   {
      super.onResume();
      if(  mTrackUri != null )
      {
         showDialog( DIALOG_TRACKNAME );
      }
      else
      {
         Log.e(TAG, "Naming track without a track URI supplied." );
         finish();
      }
   }
   
   @Override
   protected Dialog onCreateDialog( int id )
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_TRACKNAME:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.namedialog, null );
            mTrackNameView = (EditText) view.findViewById( R.id.nameField );
             mHelmetRadioGroup = (RadioGroup) view.findViewById(R.id.helmetRadioGroup);
             mServiceRatingBar = (RatingBar) view.findViewById(R.id.serviceRatingBar);
             mOriginReasonSpinner = (Spinner) view.findViewById(R.id.originReasonSpinner);
             mDestinationReasonSpinner = (Spinner) view.findViewById(R.id.destinationReasonSpinner);
             mReasonAdapter = ArrayAdapter.createFromResource(this, R.array.Reason_choices, android.R.layout.simple_spinner_item);
             mReasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

             mOriginReasonSpinner.setAdapter(mReasonAdapter);
             mDestinationReasonSpinner.setAdapter(mReasonAdapter);

             mStartStationAutoCompleteTextView = (AutoCompleteTextView) view.findViewById(R.id.startStationAutocomplete);

             mStartStationAutoCompleteTextView.setAdapter(mStartStationSimpleCursorAdapter);
             mStartStationAutoCompleteTextView.addTextChangedListener(this);
             mStartStationAutoCompleteTextView.addTextChangedListener(mStartStationTextWatcher);


             mEndStationAutoCompleteTextView = (AutoCompleteTextView) view.findViewById(R.id.endStationAutocomplete);

             mEndStationAutoCompleteTextView.setAdapter(mEndStationSimpleCursorAdapter);
             mEndStationAutoCompleteTextView.addTextChangedListener(this);
             mEndStationAutoCompleteTextView.addTextChangedListener(mEndStationTextWatcher);

            builder
               .setTitle(R.string.dialog_routename_title)
               //.setMessage( R.string.dialog_routename_message )
               .setIcon( android.R.drawable.ic_dialog_alert )
               .setPositiveButton( R.string.btn_okay, mTrackNameDialogListener )
               .setNeutralButton( R.string.btn_skip, mTrackNameDialogListener )
               .setNegativeButton( R.string.btn_cancel, mTrackNameDialogListener )
               .setView( view );
            dialog = builder.create();
            dialog.setOnDismissListener( new OnDismissListener()
            {
               public void onDismiss( DialogInterface dialog )
               {
                  if( !paused )
                  {
                     finish();
                  }
               }
            });
            return dialog;
         default:
            return super.onCreateDialog( id );
      }
   }
   
   @Override
   protected void onPrepareDialog( int id, Dialog dialog )
   {
      switch (id)
      {
         case DIALOG_TRACKNAME:

             Cursor trackCursor = null;
             //TODO: I feel kinda bad now I'm using CursorLoader for start/end station, though here I always request 1 ROW AT MOST (AND HOPEFULLY)
             //trackCursor = TrackList.this.getContentResolver().query(mDialogUri, new String[] { Tracks._ID, Tracks.WITH_HELMET, Tracks.START_REASON}, null, null,null);
             trackCursor = getContentResolver().query(mTrackUri, null, null, null,null);

             //This should always contains at most 1 row, given we request for a particular track ID, contained in the URI
             if (trackCursor.moveToFirst())
             {
                 String trackName = trackCursor.getString(trackCursor.getColumnIndex(Tracks.NAME));
                 if (trackName == null || trackName.isEmpty())
                 {
                     Calendar c = Calendar.getInstance();
                     trackName = String.format( getString( R.string.dialog_routename_default ), c, c, c, c, c );
                 }

                 mTrackNameView.setText( trackName );
                 mTrackNameView.setSelection( 0, trackName.length() );

                 //boolean nullHelmet = trackCursor.isNull(trackCursor.getColumnIndex(Tracks.WITH_HELMET));

                 int helmetValue = trackCursor.getInt(trackCursor.getColumnIndex(Tracks.WITH_HELMET));

                 if (/*nullHelmet ||*/ helmetValue == -1)
                 {
                     mHelmetRadioGroup.clearCheck();
                 }
                 else if (helmetValue == 1)
                 {
                     mHelmetRadioGroup.check(R.id.helmetRadio_yes);
                 }
                 else if (helmetValue == 0)
                 {
                     mHelmetRadioGroup.check(R.id.helmetRadio_no);
                 }

                 int startReasonIdx = trackCursor.getColumnIndex(Tracks.START_REASON);
                 String startReason = trackCursor.getString(startReasonIdx);

                 //beurk !
                 mOriginReasonSpinner.setSelection(mReasonAdapter.getPosition(startReason));

                 int endReasonIdx = trackCursor.getColumnIndex(Tracks.END_REASON);
                 String endReason = trackCursor.getString(endReasonIdx);

                 //beurk !
                 mDestinationReasonSpinner.setSelection(mReasonAdapter.getPosition(endReason));
                 
                 mServiceRatingBar.setRating(trackCursor.getInt(trackCursor.getColumnIndex(Tracks.SERVICE_RATING)));
                 
                 String startStationName = trackCursor.getString(trackCursor.getColumnIndex(Tracks.START_STATION_NAME));
                 if (startStationName == null)
                 {
                     startStationName = "";
                 }
                 mStartStationAutoCompleteTextView.setText(startStationName);

                 String endStationName = trackCursor.getString(trackCursor.getColumnIndex(Tracks.END_STATION_NAME));
                 if (endStationName == null)
                 {
                     endStationName = "";
                 }
                 mEndStationAutoCompleteTextView.setText(endStationName);
             }

             trackCursor.close();
             //Yeah that's where it is suspect, I should now use Loader to avoid managing cursor myself
            break;
         default:
            super.onPrepareDialog( id, dialog );
            break;
      }
   }

    //////////////////////////////////////////////////////////////////////////
    //CursorLoader interface
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle)
    {
        //??
        // NOTE:
        // If wildcards are to be used in a rawQuery, they must appear
        // in the query parameters, and not in the query string proper.
        // See http://code.google.com/p/android/issues/detail?id=3153
        //constraint = constraint.trim() + "%";
        //--> Ok got it, though I wonder what dictates where I put the '%' character
        //: in the select clause or concatenated at the end of the corresponding selectArgs[] item
        //So here goes, I put the % in selectArgs, though I don't feel like I use a 'rawQuery'

        String[] stationProjection = {Stations._ID, Stations.NAME};
        String stationSelect = Stations.NAME + " LIKE ?";
        //Assuming mCurStartStationNameFilter/mCurEndStationNameFilter has been trimmed
        String[] stationSelectArgs = null;
        if (loaderID == STARTSTATIONCURSOR_LOADER)
        {
            stationSelectArgs = new String[]{"%" + mCurStartStationNameFilter + "%"};
        }
        else
        {
            stationSelectArgs = new String[]{"%" + mCurEndStationNameFilter + "%"};
        }

        //String[] selectArgs = {"sim%"};
        String stationSortOrder = Stations.NAME + " COLLATE LOCALIZED ASC";

        CursorLoader cursorLoader = null;

        if (loaderID == STARTSTATIONCURSOR_LOADER && mCurStartStationNameFilter.isEmpty() ||
                loaderID == ENDSTATIONCURSOR_LOADER && mCurEndStationNameFilter.isEmpty())
        {
            //I don't feel good requesting every rows here as the precise point of using an autocompletetextview
            //is to avoid it. Though it seems the GUI element can't function with an empty Loader created with the empty constructor
            //even if suggestion are supposed to appear after a few characters are in. I could format a request such as getting an
            //empty cursor ?

            cursorLoader = new CursorLoader(this,
                    //        Tracks.CONTENT_URI, projection, select, selectArgs, sortOrder);
                    Stations.CONTENT_URI, null, null, null, null);
        }
        else
        {
            cursorLoader = new CursorLoader(this,
                    Stations.CONTENT_URI, stationProjection, stationSelect, stationSelectArgs, stationSortOrder);
            //Tracks.CONTENT_URI, null, null, null, null);
        }

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor)
    {
        if (cursorLoader.getId() == STARTSTATIONCURSOR_LOADER)
        {
            mStartStationSimpleCursorAdapter.swapCursor(cursor);
        }
        else
        {
            mEndStationSimpleCursorAdapter.swapCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader)
    {
        if (cursorLoader.getId() == STARTSTATIONCURSOR_LOADER)
        {
            mStartStationSimpleCursorAdapter.swapCursor(null);
        }
        else
        {
            mEndStationSimpleCursorAdapter.swapCursor(null);
        }
    }
    //////////////////////////////////////////////////////////////////////////
    //TextWatcher interface for activity
    @Override
    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable editable) {

        if(mStartStationAutoCompleteTextView.enoughToFilter())
        {
            String newFilter = editable.toString();

            if (!newFilter.equalsIgnoreCase(mCurStartStationNameFilter))
            {
                mCurStartStationNameFilter = newFilter;
                getSupportLoaderManager().restartLoader(STARTSTATIONCURSOR_LOADER, null, this);
            }
        }
        
        if (mEndStationAutoCompleteTextView.enoughToFilter())
        {
            String newFilter = editable.toString();
            if (!newFilter.equalsIgnoreCase(mCurEndStationNameFilter))
            {
                mCurEndStationNameFilter = newFilter;
                getSupportLoaderManager().restartLoader(ENDSTATIONCURSOR_LOADER, null, this);
            }
        }
    }
}
