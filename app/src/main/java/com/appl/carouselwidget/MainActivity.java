package com.appl.carouselwidget;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
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
        carousel.setSlowDownCoefficient(2);
        carousel.setSpacing(0.2f);
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
            MyFrame v;
            if (convertView == null) {
                v = new MyFrame(MainActivity.this);
            } else {
                v = (MyFrame)convertView;
            }

            v.setImageResource(mResourceIds[position % mResourceIds.length]);


            return v;
        }
    }

    public static class MyFrame extends FrameLayout{
        private ImageView mImageView;

        public void setImageResource(int resId){
            mImageView.setImageResource(resId);
        }

        public MyFrame(Context context) {
            super(context);

            mImageView = new ImageView(context);
            mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            addView(mImageView);

            setBackgroundColor(Color.WHITE);
            setSelected(false);
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);

            if(selected) {
                mImageView.setAlpha(1.0f);
            } else {
                mImageView.setAlpha(0.5f);
            }
        }
    }
}
