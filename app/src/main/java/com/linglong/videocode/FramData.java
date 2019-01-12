package com.linglong.videocode;

public class FramData {

    public long time;

    public byte [] datas;

    public FramData(){

    }

    public FramData(byte [] datas,long time){
        this.datas = datas;
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public byte[] getDatas() {
        return datas;
    }

    public void setDatas(byte[] datas) {
        this.datas = datas;
    }
}
