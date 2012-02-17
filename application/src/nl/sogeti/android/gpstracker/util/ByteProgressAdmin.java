package nl.sogeti.android.gpstracker.util;

import android.view.Window;

/**
 * Created by IntelliJ IDEA.
 * User: F8Full
 * Date: 12-02-17
 * Time: 10:22
 * Refactoring parsors so that they can be used with ProgressFilterInputStream
 */
abstract public class ByteProgressAdmin extends ProgressAdmin
{
    private long mProgressedBytes;
    private int mProgress;
    private long mContentLength;

    public void setContentLength(long contentLength)
    {
        mContentLength = contentLength;
    }
    
    @Override
    public int getProgress()
    {
        return mProgress;
    }

    protected void addBytesProgress(int addedBytes)
    {
        mProgressedBytes += addedBytes;
        mProgress = (int) (Window.PROGRESS_END * mProgressedBytes / mContentLength);
    }
}
