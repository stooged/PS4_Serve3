package stooged.ps4serve3;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;

public class ListAdapter extends BaseAdapter{

    Context mContext;
    sFile[] sFilesForAdapter;


    public ListAdapter(Context context,  sFile sFiles[])
    {
        mContext = context;
        sFilesForAdapter = sFiles;
    }


    @Override
    public int getCount() {
        return sFilesForAdapter.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


    static class ViewHolder {
        TextView text;
        TextView text2;
        TextView text3;
        ImageView icon;
        File lFile;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null)
        {
            convertView = li.inflate(R.layout.list_layout, null);
            viewHolder = new ViewHolder();
            viewHolder.text = (TextView) convertView.findViewById(R.id.label2);
            viewHolder.text2 =(TextView) convertView.findViewById(R.id.label);
            viewHolder.text3 =(TextView) convertView.findViewById(R.id.label3);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.logo);
            viewHolder.lFile = null;
            convertView.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder)convertView.getTag();
        }
        viewHolder.text.setText(sFilesForAdapter[position].label);
        viewHolder.text2.setText(sFilesForAdapter[position].name);
        viewHolder.text3.setText(sFilesForAdapter[position].sdir);
        viewHolder.icon.setImageDrawable(sFilesForAdapter[position].icon);
        viewHolder.lFile = (sFilesForAdapter[position].lFile);
        return convertView;
    }
}
