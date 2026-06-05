package com.herohan.uvcapp;

class ImageRawData {
    private byte[] mData;
    private int mWidth;
    private int mHeight;

    public ImageRawData(byte[] data, int width, int height) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive, got: " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive, got: " + height);
        }
        this.mData = data;
        this.mWidth = width;
        this.mHeight = height;
    }

    public byte[] getData() {
        return mData;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }
}
