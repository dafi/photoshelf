package com.ternaryop.photoshelf.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ternaryop.photoshelf.R;

public class DrawerAdapter extends ArrayAdapter<DrawerItem> {
    private static LayoutInflater inflater = null;
    private boolean isSelectionEnabled;

    public DrawerAdapter(Context context) {
        super(context, 0);
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DrawerItem drawerItem = getItem(position);
        ViewHolder holder;
        
        if (convertView == null) {
            if (drawerItem.isHeader()) {
                convertView = inflater.inflate(R.layout.drawer_header_list_item, null);
            } else {
                convertView = inflater.inflate(R.layout.drawer_list_item, null);
            }
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.title.setText(drawerItem.getTitle());
        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        return isSelectionEnabled() && !getItem(position).isHeader();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItem(position).isHeader() ? 0 : 1;
    }

    @Override
    public int getViewTypeCount() {
        // header and normal row
        return 2;
    }
    
    public boolean isSelectionEnabled() {
        return isSelectionEnabled;
    }

    public void setSelectionEnabled(boolean isSelectionEnabled) {
        this.isSelectionEnabled = isSelectionEnabled;
    }

    private class ViewHolder {
        TextView title;
        
        ViewHolder(View view) {
            title = (TextView) view.findViewById(android.R.id.text1);
        }
    }
}
