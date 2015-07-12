package com.px.charge.dao;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by pangpengde on 15/7/12.
 */
public class Charge implements Serializable {
    private long mId;
    private Date mPaidDate;
    private Date mCreateDate;
    private String mTitle;
    private float mNumber;
    private String mDescription;

    public Charge(String mTitle, float mNumber, String mDescription) {
        this.mTitle = mTitle;
        this.mNumber = mNumber;
        this.mDescription = mDescription;
    }
}
