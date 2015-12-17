package com.winrisk.game.view;

import com.winrisk.game.cluster.FieldList;
import com.winrisk.game.object.Field;
import com.winrisk.game.util.PointF;

interface FieldListener {
    FieldList getFieldList();

    boolean onFieldDrag(Field from, Field to);

    boolean onFromField(Field field);

    boolean onLongClick(Field field);

    boolean onShortClick(Field field);

    boolean onToField(Field field);

    boolean onVoidClick(PointF point);

    boolean onVoidDrag(PointF from, PointF to);
}