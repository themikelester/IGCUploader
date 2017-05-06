package tools.soaring.igcuploader;

import android.Manifest;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_IGCFILE = "tools.soaring.igcuploader.IGCFILE";
    public static final int LOADER_IGC = 1;
    private static final String TAG = "IgcUploader";
    private static final String ACTION_USB_PERMISSION = "com.github.mjdev.libaums.USB_PERMISSION";
//    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context = this;

        // Make sure we have SD card permissions
        //@TODO: Request permission and explain if denied. https://developer.android.com/training/permissions/requesting.html
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if( permissionCheck != PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }

        // Find all IGC files on the device
        ArrayList<File> igcFiles = new ArrayList<>();
        findIgcFiles(Environment.getExternalStorageDirectory(), igcFiles);

        // Sort the files by date
        Collections.sort(igcFiles, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Long.valueOf(o2.lastModified()).compareTo(o1.lastModified());
            }
        });

        // Set up an ArrayAdapter
        IgcFileAdapter adapter = new IgcFileAdapter(this, igcFiles );

        // Register the adapter to our ListView
        ListView listView = (ListView) findViewById(R.id.igcListView);
        listView.setAdapter(adapter);

        // Handle ListView item clicks
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(context, IgcUploadActivity.class);
                File igcFile = (File)parent.getItemAtPosition(position);
                intent.putExtra(EXTRA_IGCFILE, igcFile);
                startActivity(intent);
            }
        });
    }

    public void findIgcFiles(File dir, ArrayList<File> files)
    {
        final String IGC_EXT = ".igc";
        File[] dirFiles = dir.listFiles();

        if(dirFiles != null)
        {
            for( int i = 0; i < dirFiles.length; i++ )
            {
                File file = dirFiles[i];
                if(file.isDirectory()) { findIgcFiles(file, files); }
                else { if(file.getName().endsWith(IGC_EXT)) {
                        files.add(file);
                    }
                }
            }
        }
    }
}

class IgcFileAdapter extends ArrayAdapter<File> {
    private static class ViewHolder {
        TextView name;
        TextView date;
    }

    public IgcFileAdapter(Context context, ArrayList<File> files) {
        super(context, 0, files);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        File file = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_igc, parent, false);
            viewHolder.name = (TextView) convertView.findViewById(R.id.igcName);
            viewHolder.date = (TextView) convertView.findViewById(R.id.igcDate);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.name.setText( file.getName() );
        viewHolder.date.setText( (new Date(file.lastModified())).toString() );

        return convertView;
    }
}
