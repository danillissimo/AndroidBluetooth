package com.bt.device.message.incoming;

import com.bt.BTInputStream;

import java.io.IOException;
import java.text.ParseException;

import androidx.annotation.NonNull;

public abstract class Appendix {

    //Методы, поощряющие предварительнео чтение - зло

//    public abstract void parse(@NonNull Parser p) throws ParseException;

//    public abstract void parse(@NonNull String data) throws ParseException;

    public abstract void parse(@NonNull BTInputStream input)throws StreamParseException, IOException;
}
