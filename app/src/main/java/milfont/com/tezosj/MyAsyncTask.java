package milfont.com.tezosj;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;

@SuppressLint("NewApi")
public abstract class MyAsyncTask<T, V, Q> extends AsyncTask<T, V, Q>
{

    @SafeVarargs
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public final void executeContent(T... content)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, content);
        }
        else
        {
            this.execute(content);
        }
    }
}