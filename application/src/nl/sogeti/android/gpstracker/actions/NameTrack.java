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

import java.util.Calendar;

import android.database.Cursor;
import android.widget.*;
import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Empty Activity that pops up the dialog to name the track
 *
 * @version $Id: NameTrack.java 1132 2011-10-09 18:52:59Z rcgroot $
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 */
public class NameTrack extends Activity
{
   private static final int DIALOG_TRACKNAME = 23;

   protected static final String TAG = "OGT.NameTrack";

   private EditText mTrackNameView;
   //F8F BEGIN
   private CheckBox mTrackHelmetView;
   private Spinner mOriginReasonSpinner;
   private Spinner mDestinationReasonSpinner;

   private ArrayAdapter<CharSequence> mReasonAdapter;

    private RatingBar mServiceRatingBar;

   private AutoCompleteTextView mStartStationAutoCompleteTextView;
    private AutoCompleteTextView mEndStationAutoCompleteTextView;
   private ArrayAdapter<String> mStationAdapter;
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

    private void updateTrack()
    {
        String trackName = null;
        trackName = mTrackNameView.getText().toString();
        ContentValues values = new ContentValues();
        values.put( Tracks.NAME, trackName );


        if (mTrackHelmetView.isChecked())
        {
            values.put(Tracks.WITH_HELMET, "1");
        }
        else
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
             mTrackHelmetView = (CheckBox) view.findViewById(R.id.helmetCheck);
             mServiceRatingBar = (RatingBar) view.findViewById(R.id.serviceRatingBar);
             mOriginReasonSpinner = (Spinner) view.findViewById(R.id.originReasonSpinner);
             mDestinationReasonSpinner = (Spinner) view.findViewById(R.id.destinationReasonSpinner);
             mReasonAdapter = ArrayAdapter.createFromResource(this, R.array.Reason_choices, android.R.layout.simple_spinner_item);
             mReasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

             mOriginReasonSpinner.setAdapter(mReasonAdapter);
             mDestinationReasonSpinner.setAdapter(mReasonAdapter);

             mOriginReasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
             {
                 //          public void onItemSelected(AdapterView<?> parent,
//                                     View view, int pos, long id) {
                 public void onItemSelected(AdapterView< ? > arg0, View arg1, int position, long arg3)
                 {
                     //adjustTargetToType(position);
                 }

                 public void onNothingSelected(AdapterView< ? > arg0)
                 { /* NOOP */
                 }
             });
//             mDestinationReasonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
//             {
//                 //          public void onItemSelected(AdapterView<?> parent,
////                                     View view, int pos, long id) {
//                 public void onItemSelected(AdapterView< ? > arg0, View arg1, int position, long arg3)
//                 {
//                     //adjustTargetToType(position);
//                 }
//
//                 public void onNothingSelected(AdapterView< ? > arg0)
//                 { /* NOOP */
//                 }
//             });

             mStartStationAutoCompleteTextView = (AutoCompleteTextView) view.findViewById(R.id.startStationAutocomplete);

             String[] stationsNames = getResources().getStringArray(R.array.Stations_names);
             //mStationAdapter = ArrayAdapter.createFromResource(this, R.array.Stations_names, android.R.layout.simple_list_item_1);
             mStationAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, stationsNames);
             mStartStationAutoCompleteTextView.setAdapter(mStationAdapter);

             mEndStationAutoCompleteTextView = (AutoCompleteTextView) view.findViewById(R.id.endStationAutocomplete);
             mEndStationAutoCompleteTextView.setAdapter(mStationAdapter);

            builder
               .setTitle( R.string.dialog_routename_title )
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
            //String trackName;


             //I need to setup data such as dialog reflects database content
             //I have the track URI so damn fucking fuck I'll get that row
             Cursor trackCursor = null;
             //trackCursor = TrackList.this.getContentResolver().query(mDialogUri, new String[] { Tracks._ID, Tracks.WITH_HELMET, Tracks.START_REASON}, null, null,null);
             trackCursor = getContentResolver().query(mTrackUri, null, null, null,null);

             //This should always contains at most 1 row, given we request for a particular track ID
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

                 int helmetIdx = trackCursor.getColumnIndex(Tracks.WITH_HELMET);
                 boolean isnull = trackCursor.isNull(helmetIdx);

                 int helmetValue = trackCursor.getInt(helmetIdx);

                 //if (trackCursor.getInt(trackCursor.getColumnIndex(Tracks.WITH_HELMET)) == 1)
                 if (helmetValue == 1)
                 {
                     mTrackHelmetView.setChecked(true);
                 }
                 else
                 {
                     mTrackHelmetView.setChecked(false);
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

             //TODO: Retrieve stuff from the database to link the dialog state to data
             //Check hos this is done in the track lst view for example
             trackCursor.close();
            break;
         default:
            super.onPrepareDialog( id, dialog );
            break;
      }
   }   
}
   