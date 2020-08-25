package org.apromore.plugin.portal.loganimation.frame;

public class AnimationSetting {
    private int fps = 24; //frames per second
    private int frameGap = 41; //milliseconds
    private long startTimestamp;
    private int chunkSize;
    
    public int getFPS() {
        return this.fps;
    }
    
    public void setFPS(int fps) {
        this.fps = fps;
    }
    
    public int getFrameGap() {
        return this.frameGap;
    }
    
    public void setFrameGap(int frameGap) {
        this.frameGap = frameGap;
    }
    
    public long getStartTimestamp() {
        return this.startTimestamp;
    }
    
    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }
    
    public int getChunkSize() {
        return this.chunkSize;
    }
    
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
}
