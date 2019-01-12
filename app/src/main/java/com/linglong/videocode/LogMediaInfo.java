package com.linglong.videocode;

import android.media.MediaFormat;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

public class LogMediaInfo {

    public static final String TAG = "LogMediaInfo";

    public static void LogMediaFormat(MediaFormat format){
        if (format == null){
            return;
        }
        Log.e(TAG,"------------------------------------------------:");
        Class<?> formatClass = format.getClass();
        try {
            Field field = formatClass.getDeclaredField("mMap");
            field.setAccessible(true);
            Map<String,Object> map = (Map<String, Object>) field.get(format);
            if (map!=null){
                Iterator entries = map.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry entry = (Map.Entry) entries.next();
                    String key = (String)entry.getKey();
                    Object value = entry.getValue();
                    Log.i(TAG,"key:"+key+"--value:"+value.toString());
                }
            }
        } catch (Exception e) {//18612816987
            e.printStackTrace();
        }
    }


}
