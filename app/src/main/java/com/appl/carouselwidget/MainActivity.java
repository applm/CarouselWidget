package com.appl.carouselwidget;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.appl.library.Carousel;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Carousel carousel = (Carousel)findViewById(R.id.carousel);
        Adapter adapter = new MyAdapter();
        carousel.setAdapter(adapter);
        carousel.setSelection(adapter.getCount()-1); //adapter.getCount()-1
    }


    private class MyAdapter extends BaseAdapter {
        private int[] mResourceIds = {R.drawable.poster1, R.drawable.poster2, R.drawable.poster3, R.drawable.poster4,
            R.drawable.poster5};

        @Override
        public int getCount() {
            return mResourceIds.length * 5;
        }

        @Override
        public Object getItem(int position) {
            return mResourceIds[position % mResourceIds.length];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView v;
            if (convertView == null) {
                v = new ImageView(MainActivity.this);
            } else {
                v = (ImageView)convertView;
            }

            v.setImageResource(mResourceIds[position % mResourceIds.length]);
            v.setScaleType(ImageView.ScaleType.FIT_XY);

            return v;
        }
    }
}
