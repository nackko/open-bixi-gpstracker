package nl.sogeti.android.gpstracker.util;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: F8Full
 * Date: 12-02-17
 * Time: 09:35
 * Parsers refactor, this provide base elements for a ProgressAdmin object
 */
abstract public class ProgressAdmin {

    private long lastUpdate;


    abstract public int getProgress();
    abstract public void considerPublishProgress();

    protected boolean mustPublishProgress()
    {
        boolean toReturn = false;

        long now = new Date().getTime();
        if( now - lastUpdate > 1000 )
        {
            lastUpdate = now;
            toReturn = true;
        }

        return toReturn;
    }
}
