package com.example.occ_opencv;

import android.util.Size;

import java.util.Comparator;

class CompareSizesByArea implements Comparator<Size> {
    @Override
    public int compare(Size lhs, Size rhs) {
        // Ordenar en orden descendente según el área
        return Long.signum((long) rhs.getWidth() * rhs.getHeight() - (long) lhs.getWidth() * lhs.getHeight());
    }
}
