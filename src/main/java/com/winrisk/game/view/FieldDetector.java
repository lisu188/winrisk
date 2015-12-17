package com.winrisk.game.view;

import com.winrisk.game.data.MotionEvent;
import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;

class FieldDetector {
    private static final int distMin = 25;
    private final FieldListener fieldListener;
    private Field New;
    private Field Old;
    private PointF oldPoint;
    private boolean set;

    public FieldDetector(FieldListener fl) {
        fieldListener = fl;
        Old = null;
        New = null;
        set = false;
    }

    private float calcDist(PointF from, PointF to) {
        float dist = 0;
        dist += Math.pow(to.x - from.x, 2);
        dist += Math.pow(to.y - from.y, 2);
        dist = (float) Math.sqrt(dist);
        return dist;
    }

    public void feed(MotionEvent next) {
        if (fieldListener == null) {
            return;
        }
        switch (next.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!set) {
                    onDown(next);
                    set = true;
                }
                return;
            case MotionEvent.ACTION_UP:
                if (set) {
                    onUp(next);
                    set = false;
                }
        }
    }

    private float[] genDistTab(PointF p) {
        Field tmp;
        float dist[] = new float[fieldListener.getFieldList().size()];
        for (int i = 0; i < fieldListener.getFieldList().size(); i++) {
            tmp = fieldListener.getFieldList().get(i);
            dist[i] = PointF.dist(p, tmp.getPoint());
        }
        return dist;
    }

    private PointF getPoint(MotionEvent e) {
        return new PointF(e.getX(), e.getY());
    }

    private boolean next(PointF from, PointF to) {
        float dist = calcDist(from, to);
        return dist < distMin;
    }

    private void onDown(MotionEvent next) {
        Old = search(next);
        oldPoint = getPoint(next);

    }

    private boolean onUp(MotionEvent next) {
        boolean flag;
        New = search(next);
        PointF newPoint = getPoint(next);

        if ((Old == null) && (New == null)) {
            if (next(oldPoint, newPoint)) {
                flag = fieldListener.onVoidClick(getPoint(next));
            } else {
                flag = fieldListener.onVoidDrag(oldPoint, newPoint);
            }
        } else if (Old == New) {
            if ((next.getEventTime() - next.getDownTime()) > 500) {
                flag = fieldListener.onLongClick(Old);
            } else {
                flag = fieldListener.onShortClick(Old);
            }
        } else if (New == null) {
            if (!(flag = fieldListener.onFromField(Old))) {
                flag = fieldListener.onVoidDrag(oldPoint, newPoint);
            }
        } else if (Old == null) {
            if (!(flag = fieldListener.onToField(New))) {
                flag = fieldListener.onVoidDrag(oldPoint, newPoint);
            }
        } else if (!(flag = fieldListener.onFieldDrag(Old, New))) {
            flag = fieldListener.onVoidDrag(oldPoint, newPoint);
        }

        return flag;
    }

    private Field search(MotionEvent e) {
        PointF p = getPoint(e);
        float[] dist = genDistTab(p);
        int ind = searchNearest(dist);
        if (ind == -1) {
            return null;
        }
        return fieldListener.getFieldList().get(ind);
    }

    private int searchNearest(float[] dist) {
        int ind = -1;
        float min = distMin;
        for (int i = 0; i < fieldListener.getFieldList().size(); i++) {
            if (dist[i] < min) {
                ind = i;
                min = dist[i];
            }
        }
        return ind;
    }
}
